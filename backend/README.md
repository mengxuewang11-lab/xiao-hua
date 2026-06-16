# 小花无限小说工坊后端

## 1. 本地环境

```text
Java 17
Maven 3.9+
MySQL 8.0
数据库：novel_master
应用账户：novel_app
默认端口：8080
```

## 2. 本地启动

进入后端目录：

```powershell
cd D:\xiaohua\novel-master-three\backend
```

明文模型 Key：

```powershell
$env:ZHIPU_API_KEY="a71a94cc8f97484d826400cedcdf9e05.9H3MXPKUthZ04jKh"
$env:KIMI_API_KEY="sk-LcTu2FIrdfctwUpW5SnccpXWCc9qxafvn5lK4AaehGREEMBV"
```

启动：

```powershell
$env:SERVER_PORT="8080"
$env:CHAT_MODEL_PROVIDER="zhipu"
$env:CHAT_MODEL_NAME="glm-4.7-flash"
$env:ZHIPU_API_KEY="a71a94cc8f97484d826400cedcdf9e05.9H3MXPKUthZ04jKh"
$env:KIMI_API_KEY="sk-LcTu2FIrdfctwUpW5SnccpXWCc9qxafvn5lK4AaehGREEMBV"
mvn spring-boot:run
```

当前本地默认配置：

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/novel_master
DB_USERNAME=novel_app
DB_PASSWORD=Ww10106681@
APP_AUTH_ENABLED=false
TEST_ENDPOINTS_ENABLED=true
SWAGGER_ENABLED=true
MODEL_PROVIDER=fake
CHAT_MODEL_PROVIDER=zhipu
CHAT_MODEL_NAME=glm-4.7-flash
```

## 3. Swagger

```text
Swagger UI：http://localhost:8080/swagger-ui.html
OpenAPI JSON：http://localhost:8080/v3/api-docs
```

业务 API 使用：

```text
/api/{控制器}/{接口名}
```

管理 API 后续使用：

```text
/admin-api/{控制器}/{接口名}
```

## 4. 当前接口

测试接口：

```text
GET /api/test/ping
GET /api/test/database
```

NovelAgent 原型接口：

```text
POST /api/novel-agent/runs
GET  /api/novel-agent/runs/{runId}
POST /api/novel-agent/runs/{runId}/creative-selection
GET  /api/novel-agent/runs/{runId}/chapters
GET  /api/novel-agent/runs/{runId}/decisions
POST /api/novel-agent/runs/{runId}/decisions/{decisionId}/resolve
POST /api/novel-agent/runs/{runId}/resume
```

Web 对话工作台接口：

```text
GET  /api/chat/models
GET  /api/chat/preferences
PUT  /api/chat/preferences
GET  /api/chat/conversations
POST /api/chat/conversations
GET  /api/chat/conversations/{conversationId}
POST /api/chat/messages
```

## 5. 数据库迁移

当前 Flyway：

```text
V1__initialize_schema.sql
V2__create_novel_agent_core.sql
V3__create_chat_workspace.sql
```

V3 已创建：

```text
app_user_profile
user_ui_preference
chat_conversation
chat_message
chat_model_call_record
```

## 6. 模型配置

NovelAgent 自动写作主链路：

```text
MODEL_PROVIDER=fake
```

Web 对话工作台：

```text
默认供应商：zhipu
默认模型：glm-4.7-flash
备用模型：glm-4-flash-250414
Kimi 模型：kimi-k2.6
```

明文 Key：

```text
ZHIPU_API_KEY=a71a94cc8f97484d826400cedcdf9e05.9H3MXPKUthZ04jKh
KIMI_API_KEY=sk-LcTu2FIrdfctwUpW5SnccpXWCc9qxafvn5lK4AaehGREEMBV
```

## 7. 本地验证

```powershell
mvn test
```

最近验证：

```text
mvn test：9 tests passed
Flyway：version 3
/api/chat/models：正常
/api/chat/preferences：正常
智谱真实调用：返回账户速率限制
Kimi 真实调用：返回 401 UNAUTHORIZED
```
