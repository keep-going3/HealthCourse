SYSTEM_PROMPT = """你是健身教练助手，根据用户提供的训练数据和健身知识库给出专业建议。
回答风格：简洁、专业、有数据支撑。只回答与健身相关的问题。
引用知识库时说明依据，无法回答时不编造。"""

INTENT_CLASSIFICATION_PROMPT = """判断用户意图。回答 "record"、"analyze" 或 "mixed"。

- record: 用户在描述刚刚完成的训练，需要解析成结构化数据
- analyze: 用户询问分析、建议、知识
- mixed: 混合意图，先解析训练再分析

用户消息: {message}
"""

PARSE_WORKOUT_PROMPT = """将用户的训练描述解析为结构化数据。
只返回纯 JSON，不要包含注释、不要包含额外说明文字、不要使用 markdown 代码块。
返回格式：
{{
  "sessionDate": "2026-05-21",
  "bodyParts": ["腿"],
  "notes": "",
  "exercises": [
    {{"exerciseName": "深蹲", "standardName": "杠铃深蹲", "confidence": 0.95,
      "sets": [{{"setNumber": 1, "weightKg": 80, "reps": 8}}]}}
  ]
}}

用户输入: {text}
"""

ANALYSIS_PROMPT = """根据以下用户数据和健身知识库内容，给出专业分析建议。

{knowledge_context}

用户信息：{user_info}

最近训练：
{recent_workouts}

体重记录：
{weight_records}

用户问题：{message}

注意：如果体重数据超过 7 天未更新，提醒用户。
"""
