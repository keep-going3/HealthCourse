import asyncio
from fastapi import FastAPI, HTTPException, Header
from fastapi.responses import StreamingResponse
from app.config import settings
from app.models.schemas import ChatRequest, ParseWorkoutRequest, ParseWorkoutResponse
from app.services.rag_service import RagService
from app.agent.graph import process_chat, parse_workout
from app.utils.sse import event_stream
from app.utils.logger import logger

settings.validate()

app = FastAPI(title="Fitness Agent", version="2.0.0")
rag = RagService.get_instance()


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "knowledge_base": "ready",
        "documents_count": rag.collection.count(),
        "model": settings.llm_model,
    }


@app.post("/agent/chat/stream")
async def chat_stream(req: ChatRequest):
    context = req.context or {}

    async def generate():
        async for chunk in process_chat(req.message, context):
            yield chunk

    return StreamingResponse(
        event_stream(generate()),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        }
    )


@app.post("/agent/parse-workout")
async def parse_workout_endpoint(req: ParseWorkoutRequest):
    try:
        result = await parse_workout(req.text)
        return ParseWorkoutResponse(data=result)
    except Exception as e:
        logger.error(f"parse-workout error: {e}")
        raise HTTPException(status_code=500, detail="解析失败")


@app.post("/kb/reload")
async def reload_knowledge_base(x_admin_token: str = Header(None)):
    if x_admin_token != settings.kb_reload_token:
        raise HTTPException(status_code=403, detail="无权限")
    try:
        count = rag.reload(settings.chroma_path + "_new")
        return {"code": 200, "message": "重载成功", "documents_loaded": count}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
