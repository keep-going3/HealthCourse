# 健身训练记录 App — Java 后端开发手册 v2.0（简化版）

## 一、基础信息

| 项目 | 说明 |
|------|------|
| 框架 | Spring Boot 3 + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 认证 | Spring Security + JWT |
| 熔断器 | Resilience4j |
| Base URL | http://localhost:8080/api |
| Token 有效期 | 7 天 |

## 二、统一规范

### 2.1 返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1703001234567
}
```

### 2.2 错误码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 不存在 |
| 409 | 数据冲突 |
| 429 | 服务繁忙 |
| 500 | 服务器错误 |
| 503 | 依赖不可用 |

### 2.3 认证机制

- 请求头：`Authorization: Bearer <token>`
- Controller 通过 `@RequestAttribute("userId")` 获取当前用户
- 无状态，禁用 Session，关闭 CSRF

### 2.4 Token 黑名单

| 操作 | 行为 |
|------|------|
| 退出登录 | Token 加入黑名单 |
| 修改密码 | 旧 Token 加入黑名单 |

### 2.5 异常处理

- 全局 `@RestControllerAdvice` 统一处理
- Controller 不写 try-catch
- Agent 失败走降级，不抛异常

## 三、数据库设计（核心表）

### 3.1 用户表 (user)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | BCrypt 编码 |
| height_cm | DECIMAL(5,1) | | 身高(cm) |
| target_weight_kg | DECIMAL(5,1) | | 目标体重(kg) |
| goal | VARCHAR(20) | | gain/cut/maintain |
| refresh_token | VARCHAR(255) | | 预留刷新令牌 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 3.2 训练记录主表 (workout_session)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 会话ID |
| user_id | BIGINT | FK, NOT NULL | 用户ID |
| session_date | DATE | NOT NULL | 训练日期 |
| body_parts | JSON | | JSON 数组，如 ["chest","shoulder"] |
| total_sets | INT | NOT NULL | 后端自动计算，前端不传 |
| feel_rating | TINYINT | 1-5 | 训练评分 |
| notes | VARCHAR(500) | | 备注 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

索引：`(user_id, session_date)` 联合唯一

### 3.3 动作明细表 (workout_exercise)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| session_id | BIGINT | FK, NOT NULL | 关联 workout_session |
| exercise_name | VARCHAR(100) | NOT NULL | 动作名称 |
| set_number | TINYINT | NOT NULL | 第几组 |
| weight_kg | DECIMAL(5,1) | | 重量(kg) |
| reps | TINYINT | | 次数 |
| rpe | DECIMAL(2,1) | 1.0-10.0 | RPE 值 |
| created_at | DATETIME | NOT NULL | |

### 3.4 动作库表 (exercise_library)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK | NULL=系统内置，非NULL=用户自定义 |
| name | VARCHAR(100) | NOT NULL | 动作名称 |
| target_muscle | VARCHAR(20) | NOT NULL | chest/back/shoulder/arms/legs/other |
| equipment | VARCHAR(20) | NOT NULL | barbell/dumbbell/machine/cable/bodyweight |
| type | VARCHAR(10) | NOT NULL | compound/isolation |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

规则：系统动作只读，用户可增删改自己的动作。

### 3.5 体重记录表 (weight_record)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK, NOT NULL | |
| weight_kg | DECIMAL(5,1) | NOT NULL | 体重(kg) |
| record_date | DATE | NOT NULL | 记录日期 |
| created_at | DATETIME | NOT NULL | |

唯一约束：`(user_id, record_date)` 同一用户同一天只能有一条记录。

### 3.6 反馈表 (feedback)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK, NOT NULL | |
| content | TEXT | NOT NULL | 反馈内容 |
| created_at | DATETIME | NOT NULL | |

## 四、Redis 缓存设计

| Key | 类型 | 过期时间 | 说明 |
|-----|------|----------|------|
| user:body:{userId} | Hash | 1 小时 | 身体信息缓存 |
| exercise:library | String | 1 小时 | 动作库缓存 |
| chat:history:{userId} | List | 30 分钟 | 对话历史，最多 30 条 |
| workout:today:{userId}:{date} | String | 当天 23:59:59 | 今日已练标记 |
| token:blacklist:{tokenId} | String | Token 剩余时间 | 黑名单 |

## 五、接口清单

### 用户模块（5 个）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /user/register | 注册 |
| POST | /user/login | 登录 |
| POST | /user/refresh-token | 刷新 Token（无业务有效期校验） |
| PUT | /user/password | 修改密码 |
| POST | /user/logout | 退出 |

### 训练模块（10 个）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /exercise/library | 获取动作库 |
| GET | /exercise/library/group | 按肌群分组 |
| POST | /exercise/custom | 新增自定义动作 |
| PUT | /exercise/custom/{id} | 修改自定义动作 |
| DELETE | /exercise/custom/{id} | 删除自定义动作 |
| POST | /workout/save | 保存训练记录（当天已有时返回 409） |
| PUT | /workout/update | 覆盖当天训练记录 |
| GET | /workout/detail | 获取某天详情 |
| GET | /workout/calendar | 获取月日历 |
| DELETE | /workout/{sessionId} | 删除记录 |

### 身体数据模块（6 个）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /body/info | 获取身体信息 |
| PUT | /body/info | 更新身体信息 |
| POST | /body/weight | 记录体重 |
| GET | /body/weight/list | 体重列表 |
| PUT | /body/weight/{recordId} | 修改体重记录 |
| DELETE | /body/weight/{recordId} | 删除体重记录 |

### AI 助手模块（2 个）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /agent/chat/stream | SSE 流式对话 |
| POST | /agent/parse-workout | 自然语言解析 |

### 反馈模块（1 个）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /feedback | 提交反馈 |

## 六、核心逻辑说明

### 6.1 保存训练记录

1. 校验当天是否已有记录 → 有则返回 409
2. 后端计算 `total_sets` = 所有动作的组数总和（如 3 个动作各 4 组 → 12）
3. `body_parts` 存为 MySQL JSON 列
4. 事务内：插入 session + 批量插入 exercise
5. 写入 Redis 今日标记

### 6.2 获取月训练日历

一次性查出该月所有 session，再批量查出所有 exercise，内存中按 sessionId 分组组装，避免 N+1 查询。

### 6.3 体重变化计算

按日期升序查询，计算 `当前体重 - 前一天体重`，返回时倒序排列（最新在前），第一天 change 为 null。

### 6.4 Agent 网关（SSE 代理 + 熔断降级）

Java 后端通过 **WebClient**（非 RestTemplate）将 `/api/agent/chat/stream` 请求转发到 Python Agent，保证 SSE 流式传输不阻塞线程。

- 配置：连续失败 3 次 → 熔断 60 秒
- 超时：连接 5 秒，读取 25 秒
- 降级回复根据关键词判断：
  - 含"分析/统计/数据" → 引导去数据页面
  - 含"推荐/计划/建议" → 引导参考动作库
  - 其他 → 通用维护提示

### 6.5 动作库权限

- 系统动作（user_id = NULL）：只读
- 用户自定义动作（user_id = 当前用户）：可增删改

### 6.6 性能优化

- 月日历：批量查询 + 内存分组
- 保存记录：批量插入 exercise
- 身体信息：Redis 缓存，更新时删除
- 动作库：Redis 缓存

## 七、配置要点

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fitness_db
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: your-secret-key
  expiration: 604800000  # 7天

agent:
  base-url: http://localhost:8000
  connect-timeout: 5000
  read-timeout: 25000
```

## 八、项目结构（简化）

```
fitness-server/
├── common/          # 通用类（Result、异常处理）
├── config/          # 配置类（Security、Redis、WebClient）
├── security/        # JWT 过滤器、工具类
├── module/
│   ├── user/        # 用户模块
│   ├── workout/     # 训练模块
│   ├── body/        # 身体数据模块
│   └── feedback/    # 反馈模块
├── agent/           # Agent 网关（熔断降级）
└── resources/
    ├── application.yml
    └── db/          # schema.sql + data.sql
```

## 九、依赖清单（核心）

- Spring Boot Starter WebFlux（SSE 代理转发，替代 RestTemplate）
- Spring Boot Starter Security
- Spring Boot Starter Data Redis
- Spring Boot Starter Cache
- Spring Boot Starter Validation
- Resilience4j（熔断器）
- MyBatis-Plus
- MySQL Connector
- JJWT
- Hutool
- Lombok

## 十、预设数据说明

启动时自动初始化 45 条系统动作，覆盖五大肌群（胸、背、肩、手臂、腿），每个动作包含：名称、目标肌群、器材类型、动作类型（复合/孤立）。

前端展示时自行将英文肌群映射为中文。

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | - | 初始版本 |
| v2.0 | 2026-05-21 | 优化 JSON 存储、缓存、熔断、批量查询；新增自定义动作 |
