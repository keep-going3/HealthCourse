# Python Agent 开发文档 v2.0

## 一、基础信息

| 项目 | 说明 |
|------|------|
| 框架 | FastAPI + LangChain + LangGraph |
| 模型 | qwen-max（阿里云百炼） |
| API地址 | https://dashscope.aliyuncs.com/compatible-mode/v1 |
| 向量库 | ChromaDB（本地） |
| 端口 | 8000 |

## 二、核心约定

- Agent **不连数据库、不存状态**，所有数据由 Java 请求体传入
- 专业知识仅来自本地 ChromaDB，无网络搜索
- 训练记录保存**不走 Agent**，Java 直连 DB
- Agent **不做限流/熔断**，由 Java 网关统一处理

## 三、接口定义

### 3.1 流式对话

```
POST /agent/chat/stream
Response: text/event-stream (SSE)
```

**请求体：**

```json
{
  "userId": 1,
  "message": "帮我分析最近胸肌训练",
  "context": {
    "userInfo": {"goal": "gain", "height": 175, "targetWeight": 78, "latestWeight": 72.5},
    "recentWorkouts": [...],
    "weightRecords": [...],
    "chatHistory": [...]
  }
}
```

**SSE事件：**

| type | 说明 |
|------|------|
| start | 开始，含 messageId |
| chunk | 文本片段 |
| ping | 心跳，每15秒 |
| end | 结束，可能含 truncated:true |
| error | 错误 |

### 3.2 自然语言解析

```
POST /agent/parse-workout
```

**请求体：**

```json
{
  "userId": 1,
  "text": "今天做了深蹲4组80kg每组8次"
}
```

**响应（Agent 原生返回，无需 Java 二次包装）：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionDate": "2026-05-21",
    "bodyParts": ["腿"],
    "notes": "",
    "exercises": [
      {
        "exerciseName": "深蹲",
        "standardName": "杠铃深蹲",
        "confidence": 0.95,
        "sets": [{"setNumber": 1, "weightKg": 80, "reps": 8}]
      }
    ]
  }
}
```

### 3.3 健康检查

```
GET /health
响应：{"status": "ok", "knowledge_base": "ready", "model": "qwen-max"}
```

### 3.4 知识库热更新

```
POST /kb/reload
Header: X-Admin-Token: {token}
成功：{"code": 200, "message": "重载成功", "documents_loaded": 42}
```

## 四、内部处理流程（文字描述）

1. **意图分类**：用 LLM 判断是 `record`（记录训练）还是 `analyze`（分析咨询）。混合意图时先 record 后 analyze。
2. **上下文截断**：最多取最近 20 个动作，按日期倒序，超出丢弃并注明已截断。
3. **RAG 检索**：检索 5 条，过滤相似度低于 0.7 的文档。无结果时注明未找到权威资料。
4. **SSE 流式**：每 15 秒发心跳，客户端断连时取消 LLM 任务，LLM 超时 25 秒发 error。
5. **重试策略**：LLM 临时性错误重试 1 次，等待 1 秒。超时不重试。
6. **热更新**：新目录加载后原子切换引用，不中断服务。

## 五、环境变量

| 变量 | 默认值 | 必填 |
|------|--------|------|
| ALIYUN_API_KEY | 无 | ✅ |
| LLM_MODEL | qwen-max | |
| LLM_BASE_URL | https://dashscope.aliyuncs.com/compatible-mode/v1 | |
| LLM_TIMEOUT | 25 | |
| CHROMA_PATH | ./chroma_db | |
| RAG_SCORE_THRESHOLD | 0.7 | |
| MAX_ACTIONS_COUNT | 20 | |
| KB_RELOAD_TOKEN | 无 | |

**启动校验：** 缺 `ALIYUN_API_KEY` 报错；缺知识库目录报错并提示执行 build_kb.py。

## 六、项目结构

```
fitness-agent/
├── app/
│   ├── main.py
│   ├── config.py
│   ├── agent/（graph.py, tools.py, prompts.py）
│   ├── services/（rag_service.py）
│   ├── models/（schemas.py）
│   └── utils/（truncate.py, sse.py, logger.py）
├── data/literature/
├── chroma_db/
├── scripts/build_kb.py
├── tests/
├── requirements.txt
└── .env.example
```

## 七、启动命令

```bash
pip install -r requirements.txt
cp .env.example .env   # 填入 ALIYUN_API_KEY
python scripts/build_kb.py
uvicorn app.main:app --port 8000
```

## 八、验收要点

| 场景 | 预期 |
|------|------|
| 无知识库启动 | 报错退出 |
| 健康检查 | 返回状态 |
| 分析请求 | SSE流式+心跳 |
| LLM超时 | 发error事件 |
| 热更新 | 服务不中断 |
| 动作解析 | 返回confidence |
| 体重过时 | 回答中提醒 |
