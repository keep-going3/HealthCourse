# 智能健身训练记录平台

全栈智能健身训练记录平台，支持训练记录管理、身体数据追踪、AI 健身助手，采用前后端分离 + AI Agent 架构。

## 项目架构

```
HealthCourse/
├── fitness-app/        # 移动端 Web 应用（Vue 3）
├── fitness-server/     # 后端 API 服务（Spring Boot）
└── fitness-agent/      # AI Agent 服务（FastAPI + LLM）
```

## 技术栈

### 前端
- **Vue 3** (Composition API) + **Vite 5** — 构建 & 开发
- **Vant 4** — 移动端 UI 组件库
- **Pinia** — 状态管理
- **Vue Router 4** — 前端路由
- **Axios** — HTTP 封装（Token 自动刷新、401 拦截）

### 后端
- **Spring Boot 3.2.5** + **Java 17**
- **Spring Security** + **JWT** (jjwt 0.12.5) — 认证授权
- **MyBatis-Plus 3.5.7** — ORM
- **MySQL 8.0** — 关系数据库
- **Redis** — 缓存 & Token 黑名单
- **Spring WebFlux** — SSE 流式代理
- **Resilience4j** — 熔断降级
- **Hutool** — Java 工具库

### AI Agent
- **FastAPI** — Python 异步框架
- **阿里云通义千问 (qwen-max)** — LLM
- **LangChain / LangGraph** — Agent 流程编排
- **ChromaDB** — 向量数据库（RAG 知识库）
- **SSE** — 流式对话输出

### 部署
- **Nginx** — 反向代理 & SPA 静态资源
- **Docker** — 容器化

## 核心功能

| 模块 | 功能 |
|------|------|
| 用户系统 | 注册 / 登录 / JWT 鉴权 / Token 刷新 / 黑名单登出 |
| 训练管理 | 动作库（系统+自定义）、训练记录增删改查、月日历、覆盖当天记录 |
| 身体数据 | 身高体重目标管理、体重记录、变化趋势计算 |
| AI 助手 | RAG 知识库问答、SSE 流式对话、自然语言训练解析、熔断降级 |
| 反馈 | 用户反馈提交 |

## 快速启动

### 后端

```bash
cd fitness-server
# 配置 application.yml 中的 MySQL 和 Redis 连接
mvn spring-boot:run
```

### 前端

```bash
cd fitness-app
npm install
npm run dev
```

### AI Agent

```bash
cd fitness-agent
pip install -r requirements.txt
cp .env.example .env    # 填入 ALIYUN_API_KEY
python scripts/build_kb.py
uvicorn app.main:app --port 8000
```

## 接口概览

| 模块 | 接口数 | 说明 |
|------|--------|------|
| 用户 | 5 | 注册、登录、刷新 Token、改密、登出 |
| 训练 | 10 | 动作库 CRUD、训练记录 CRUD、月日历 |
| 身体数据 | 6 | 身体信息、体重记录 CRUD |
| AI | 2 | SSE 流式对话、自然语言解析 |
| 反馈 | 1 | 提交反馈 |
