# 健身训练记录 App — 开发路线图

## 总体策略

三个子项目并行开发，按 **后端 → 前端** 依赖顺序分 6 个阶段交付。每个阶段产出可验证的增量，不跨阶段留尾巴。

```
Phase 0: 项目脚手架
     │
Phase 1: 基础设施 ────── 用户认证（前后端贯通）
     │
Phase 2: 训练核心 ────── 主流程跑通（增/删/查/改 + 日历）
     │
Phase 3: 身体数据 ────── 体重 + 身体信息
     │
Phase 4: AI Agent ────── 流式对话 + 自然语言解析
     │
Phase 5: 收尾打磨 ────── 反馈、错误态、加载态、边界情况
```

---

## Phase 0 — 项目脚手架搭建（1-2 天）

**目标**：三个项目可启动，目录结构就位，依赖安装完成。

| 子项目 | 具体任务 | 产出 |
|--------|---------|------|
| **Backend** | IDEA 初始化 Spring Boot 3 项目；配置 `application.yml`（MySQL、Redis、JWT、WebClient lay 连接池）；引入 MyBatis-Plus 代码生成器；整合 Resilience4j | 项目可编译启动 |
| **Frontend** | `npm create vite` 创建 Vue 3 项目；安装 Vant 4、Pinia、Vue Router、Axios；配置 Vite 代理到 `localhost:8080` | `npm run dev` 可访问 |
| **Agent** | 创建 FastAPI 项目结构；安装 LangChain、LangGraph、ChromaDB 依赖；创建 `.env` 配置模板 | `uvicorn app.main:app` 启动成功 |

**前端代理配置备忘**：
```javascript
// vite.config.js
export default defineConfig({
  server: { proxy: { '/api': { target: 'http://localhost:8080' } } }
});
```

---

## Phase 1 — 基础设施 + 用户认证（3-4 天）

**目标**：用户可注册、登录、鉴权全链路跑通。

### 1.1 数据库初始化
- 编写 `schema.sql`：6 张表（user、workout_session、workout_exercise、exercise_library、weight_record、feedback）
- 编写 `data.sql`：45 条系统动作库预设数据
- 添加 Flyway 或直接执行 SQL 管理迁移

### 1.2 Backend — 通用层
- `common/Result.java` — 统一响应体（code、message、data、timestamp）
- `common/GlobalExceptionHandler.java` — `@RestControllerAdvice`，按错误码返回
- `security/` — JWT 生成/校验过滤器、Token 黑名单校验、`@RequestAttribute("userId")` 解析

### 1.3 Backend — 用户模块
| 接口 | 要点 |
|------|------|
| POST /user/register | 参数校验（用户名 4-20 位，密码 6-20 位），BCrypt 加密，返回 token |
| POST /user/login | 校验密码，生成 JWT（含 jti、userId、过期时间），存入 Redis 黑名单预留 |
| POST /user/refresh-token | 旧 token 在黑名单中则拒绝；生成新 token，加入黑名单使旧 token 失效 |
| PUT /user/password | 传入旧密码校验 → BCrypt 新密码 → 旧 token 入黑名单 |
| POST /user/logout | 当前 token 入黑名单 |

### 1.4 Frontend — 登录注册
- `request.js` — 带防重入标记的 Axios 拦截器（使用独立的 axios 实例发刷新请求避免循环依赖）
- `router/index.js` — 路由守卫，`/login`、`/register` 公开，其余需认证
- `userStore.js` — 管理 token + setToken/clearToken
- `Login.vue` — 登录表单 + 加载态 + 错误提示
- `Register.vue` — 注册表单

### 1.5 验收标准
- 注册新用户 → 自动登录 → 收到 token
- 关闭页面重新打开 → token 从 localStorage 恢复 → 自动登录
- 手动修改 token 为无效值 → 请求返回 401 → 跳登录页
- 登录状态下退出 → token 入黑名单 → 原 token 请求被拒

---

## Phase 2 — 训练核心模块（5-7 天）

**目标**：完整的训练记录增删改查 + 日历视图。

### 2.1 Backend — 动作库

| 接口 | 要点 |
|------|------|
| GET /exercise/library | 系统动作 + 用户自定义合并返回 |
| GET /exercise/library/group | 按 target_muscle 分组，Redis 缓存（1h 过期），更新时删缓存 |
| POST /exercise/custom | user_id = 当前用户 |
| PUT /exercise/custom/{id} | 校验 user_id = 当前用户 |
| DELETE /exercise/custom/{id} | 只删自定义，系统动作不可删 |

### 2.2 Backend — 训练记录

| 接口 | 要点 |
|------|------|
| POST /workout/save | 校验当天无记录 → 事务内插入 session + 批量插入 exercise；写入 Redis `workout:today:{userId}:{date}` |
| PUT /workout/update | 当天已有 → 事务内删除原 exercise + 重新插入；覆盖 Redis 标记 |
| GET /workout/detail?date= | 联表查 session + exercises |
| GET /workout/calendar?year=&month= | 批量查当月 session → 批量查关联 exercise → 内存分组组装 |
| DELETE /workout/{sessionId} | 校验 userId + 级删 exercise |

### 2.3 Frontend — 训练记录页
- `WorkoutRecord.vue` — 选择日期、选动作（从动作库）、填组数/重量/次数/RPE、评分、保存
- **保存冲突处理**：收到 409 → 弹确认框 → 确认后调用覆盖接口
- `WorkoutHistory.vue` — 月日历组件（Vant Calendar 改），点击日期显示详情

### 2.4 验收标准
- 添加训练 → 看到"保存成功"
- 同天再添加 → 弹"是否覆盖"确认框 → 覆盖成功
- 日历页面 → 当月有训练的日期有标记 → 点击查看详情
- 删除训练 → 记录消失

---

## Phase 3 — 身体数据模块（2 天）

**目标**：身体信息管理、体重记录与趋势。

### 3.1 Backend
| 接口 | 要点 |
|------|------|
| GET /body/info | 查 user 表 height/targetWeight/goal，Redis 缓存 |
| PUT /body/info | 更新 user 表、删缓存 |
| POST /body/weight | 当天已有则覆盖（用唯一约束 ON DUPLICATE） |
| GET /body/weight/list?months= | 按日期升序查 → 计算 change（当前 - 前一天，第一天 change=null）→ 倒序返回 |
| PUT /body/weight/{recordId} | 校验 userId + recordId |
| DELETE /body/weight/{recordId} | 同上 |

### 3.2 Frontend
- `BodyData.vue` — 展示身体信息（身高/目标/当前体重），编辑弹窗；体重录入；体重趋势折线图（用 Vant 或轻量 chart 库）
- `Profile.vue` — 修改密码、展示个人信息

### 3.3 验收标准
- 录入体重 → 列表出现新记录，change 值正确
- 修改体重 → 更新成功
- 删除体重 → 记录消失
- 修改身体目标 → 下次打开持久化

---

## Phase 4 — AI Agent 集成（5-6 天）

**目标**：AI 流式对话 + 自然语言解析训练。

### 4.1 Agent — 基础服务
| 任务 | 要点 |
|------|------|
| `config.py` | 从环境变量读取配置，启动校验 ALIYUN_API_KEY、知识库目录 |
| `models/schemas.py` | ChatRequest（userId+message+context）、ParseWorkoutRequest、SSE event 模型 |
| 健康检查 GET /health | 返回 agent 状态 + ChromaDB 状态 + 模型名 |
| `utils/sse.py` | SSE 事件格式化工具（start/chunk/ping/end/error），ping 每 15s |
| `utils/logger.py` | 结构化日志配置 |

### 4.2 Agent — 知识库
| 任务 | 要点 |
|------|------|
| 准备 `data/literature/` 中的健身知识文档（.md 或 .txt） | 如训练原则、营养基础、动作要领等 |
| `scripts/build_kb.py` | 读取文献目录 → 分段 → 生成 embedding → 存入 ChromaDB |
| `services/rag_service.py` | 查询 ChromaDB（默认每请求 5 条，过滤 <0.7），热更新接口 POST /kb/reload |
| 启动自动校验 ChromaDB 目录存在，否则报错并提示执行 build_kb | |

### 4.3 Agent — LLM 集成
| 任务 | 要点 |
|------|------|
| `agent/prompts.py` | system prompt（健身教练人设）、意图分类 prompt、训练解析 prompt |
| `agent/tools.py` | RAG 检索工具 |
| `agent/graph.py` | LangGraph 流程：意图分类 →（record→parse-workout / analyze→RAG+LLM）→ 格式化输出 |
| LLM 调用 | qwen-max 通过阿里云百炼 SDK / OpenAI 兼容接口，超时 25s，临时错误重试 1 次 |
| 上下文截断 | 若 recentWorkouts 超过 20 个动作则截断并在回复中说明 |
| SSE 流式输出 | 客户端断连通过 asyncio.Task 取消机制停止 LLM 调用 |

### 4.4 Backend — Agent 网关
- `agent/` 包：WebClient 转发到 `http://localhost:8000` + Resilience4j 熔断配置
- **SSE 转发**：WebClient 以 `ExchangeFilterFunction` 读取 Agent SSE 流，逐事件转发给前端
- **降级处理**：熔断时根据用户提问关键词返回对应的降级提示文本

### 4.5 Frontend — AI 聊天页
- `AIAssistant.vue` — 输入框 + 消息列表 + 流式渲染（带闪烁光标）
- 发送请求带最近训练上下文（Java 后端在转发前从 DB 拼装 context）
- 接收 SSE 事件：start → chunks → end/error
- 连接断开自动重试

### 4.6 Frontend — 自然语言解析入口
- 在 WorkoutRecord 页面增加"语音/文字输入"入口
- 调用 `parse-workout` 接口 → 回填表单

### 4.7 验收标准
- 向 AI 提问训练建议 → 正常 SSE 流式输出含专业内容
- 输入"帮我分析最近胸肌训练" → AI 读取上下文回复分析
- 停止 Agent 服务 → AI 回复降级提示
- 输入"今天深蹲 80kg 4x8" → 解析并回填到训练表单
- 知识库热更新后不重启服务即可生效
- 体重数据超过 7 天未更新 → AI 回复中提醒

---

## Phase 5 — 收尾打磨（2-3 天）

**目标**：补全剩余功能 + 异常态覆盖 + 体验优化。

### 5.1 反馈模块
- Backend: `POST /feedback`
- Frontend: Profile 页增加"提交反馈"入口

### 5.2 全局异常态覆盖
- **网络异常**：axios 拦截器统一提示
- **空状态**：训练日历无记录、无体重数据时显示空状态占位
- **加载态**：所有列表页首屏加载展示骨架屏（Vant Skeleton）
- **错误重试**：AI 页面 SSE 断连自动重连（最多 3 次）

### 5.3 部署配置
- Nginx SPA fallback 配置（解决 createWebHistory 刷新 404）
- 后端 `application-prod.yml` 配置
- Agent uvicorn 生产启动参数

### 5.4 验收标准
- 提交反馈 → 成功后端收到
- 离线时使用 → 友好提示不白屏
- 空数据 → 有占位引导
- 部署后刷新任意前端路由 → 不 404

---

## 依赖关系总图

```
Phase 0 (脚手架)
   │
   ├──→ Phase 1 (认证) ───→ Phase 2 (训练) ───→ Phase 5 (收尾)
   │                            │
   │                            └──→ Phase 3 (身体数据) ──→ Phase 5
   │                                                    ↑
   └──→ Phase 4 (Agent) ────────────────────────────────┘
                    ↑
              Phase 0 Agent 脚手架
```

- Phase 1、2、3 可串行推进（同一团队做后端→前端）
- Phase 4 Agent 可独立于 Phase 1-3 开发，仅在后端网关集成时需要协调
- Phase 5 收尾贯穿所有模块

---

## 工时估算

| 阶段 | 后端 | 前端 | Agent | 合计 |
|------|------|------|-------|------|
| Phase 0 | 0.5d | 0.5d | 0.5d | **1.5d** |
| Phase 1 | 2d | 1.5d | — | **3.5d** |
| Phase 2 | 3d | 3d | — | **6d** |
| Phase 3 | 1d | 1d | — | **2d** |
| Phase 4 | 1d | 1.5d | 3d | **5.5d** |
| Phase 5 | 0.5d | 1.5d | 0.5d | **2.5d** |
| **总计** | **8d** | **9d** | **4d** | **~21个工作日** |

> 估算是单人全栈推进的粗略时间，实际受并行度、联调复杂度影响。
