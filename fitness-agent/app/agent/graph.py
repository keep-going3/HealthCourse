import datetime
import json
import re
from typing import AsyncGenerator, Literal

from langchain_openai import ChatOpenAI
from app.config import settings
from app.agent.prompts import (
    SYSTEM_PROMPT,
    INTENT_CLASSIFICATION_PROMPT,
    PARSE_WORKOUT_PROMPT,
    ANALYSIS_PROMPT,
)
from app.services.rag_service import RagService
from app.utils.truncate import truncate_context
from app.utils.sse import make_message
from app.utils.logger import logger

_intent_map = {"record": "record", "analyze": "analyze", "mixed": "mixed"}


def _build_llm():
    return ChatOpenAI(
        model=settings.llm_model,
        api_key=settings.aliyun_api_key,
        base_url=settings.llm_base_url,
        timeout=settings.llm_timeout,
        temperature=0.3,
        max_tokens=1024,
    )


async def classify_intent(message: str) -> Literal["record", "analyze", "mixed"]:
    llm = _build_llm()
    prompt = INTENT_CLASSIFICATION_PROMPT.format(message=message)
    resp = await llm.ainvoke(prompt)
    raw = resp.content.strip().lower()
    return _intent_map.get(raw, "analyze")


async def parse_workout(text: str) -> dict:
    llm = _build_llm()
    prompt = PARSE_WORKOUT_PROMPT.format(text=text)
    resp = await llm.ainvoke(prompt)
    content = resp.content.strip()

    # Remove markdown code block markers
    if content.startswith("```"):
        end_idx = content.find('\n')
        if end_idx != -1:
            content = content[end_idx:].strip()
    if content.endswith("```"):
        content = content[:-3].strip()

    # Extract the first JSON object from the response
    start = content.find('{')
    end = content.rfind('}')
    if start != -1 and end != -1 and end > start:
        content = content[start:end + 1]

    # Strip JSON comments (// ...) that LLMs sometimes insert
    content = re.sub(r',?\s*//.*$', '', content, flags=re.MULTILINE)
    content = content.strip()

    return json.loads(content)


async def analyze_chat(message: str, context: dict = None) -> AsyncGenerator[str, None]:
    rag = RagService.get_instance()
    context = truncate_context(context or {}, settings.max_actions_count)

    # RAG 检索
    knowledge_docs = rag.search(message)
    knowledge_context = "未找到相关专业知识" if not knowledge_docs else "\n\n".join(
        [f"[{d['source']} (score: {d['score']:.2f})]\n{d['content']}" for d in knowledge_docs]
    )

    user_info = json.dumps(context.get("userInfo", {}), ensure_ascii=False, default=str)
    recent_workouts = json.dumps(context.get("recentWorkouts", []), ensure_ascii=False, default=str)
    weight_records = json.dumps(context.get("weightRecords", []), ensure_ascii=False, default=str)

    prompt = ANALYSIS_PROMPT.format(
        knowledge_context=knowledge_context,
        user_info=user_info,
        recent_workouts=recent_workouts,
        weight_records=weight_records,
        message=message,
    )

    llm = _build_llm()
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": prompt},
    ]

    try:
        async for chunk in llm.astream(messages):
            if chunk.content:
                yield chunk.content
    except Exception as e:
        logger.error(f"LLM stream error: {e}")
        raise


async def process_chat(message: str, context: dict = None) -> AsyncGenerator[str, None]:
    """主流程：意图分类 → record/analyze"""
    intent = await classify_intent(message)

    if intent in ("record", "mixed"):
        try:
            parsed = await parse_workout(message)

            # Flatten nested exercises -> sets for auto-save
            exercises_flat = []
            for ex in parsed.get("exercises", []):
                name = ex.get("standardName") or ex.get("exerciseName", "")
                for s in ex.get("sets", []):
                    exercises_flat.append({
                        "exerciseName": name,
                        "setNumber": s["setNumber"],
                        "weightKg": s.get("weightKg"),
                        "reps": s.get("reps"),
                        "rpe": s.get("rpe"),
                    })

            # Emit save_workout event so the backend proxies the DB save
            if exercises_flat:
                session_date = parsed.get("sessionDate", "") or datetime.date.today().isoformat()
                workout_data = {
                    "sessionDate": session_date,
                    "bodyParts": parsed.get("bodyParts", []),
                    "notes": parsed.get("notes", ""),
                    "feelRating": None,
                    "exercises": exercises_flat,
                }
                yield f"__SSE_RAW__{make_message('save_workout', workoutData=workout_data)}"

            yield f"已识别训练：\n"
            for ex in parsed.get("exercises", []):
                name = ex.get("standardName") or ex.get("exerciseName", "")
                confidence = ex.get("confidence", 0)
                sets_info = "; ".join([
                    f"第{s['setNumber']}组 {s.get('weightKg', '?')}kg × {s.get('reps', '?')}次"
                    for s in ex.get("sets", [])
                ])
                yield f"- {name} (置信度: {confidence:.0%})\n  {sets_info}\n"
            yield "\n以上解析可导入训练记录。\n"
        except Exception as e:
            logger.error(f"parse error: {e}")
            yield "训练解析失败，请手动录入。\n"

    if intent in ("analyze", "mixed"):
        async for chunk in analyze_chat(message, context):
            yield chunk
