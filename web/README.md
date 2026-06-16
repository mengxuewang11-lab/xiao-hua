# 小花无限小说工坊 Web

用户 Web 网站，面向创作者使用。当前已完成 Next.js 初始骨架、年轻化 ChatGPT 风格工作台首页、侧边栏折叠/展开、多主题与自定义主题色、抽象动态背景、真实聊天接口接入、API 路径约定和本地启动验证。

## 技术栈

```text
Next.js App Router
React
TypeScript
Tailwind CSS
pnpm
```

## 本地启动

```powershell
cd D:\xiaohua\novel-master-three\web
pnpm install
pnpm dev
```

访问：

```text
http://localhost:3000
```

## 后端代理

Web 端真实访问路径统一使用：

```text
/api/{控制器}/{接口名}
```

本地 Next.js 会把 `/api/*` 代理到后端：

```text
http://localhost:8080/api/*
```

配置文件：

```text
.env.example
next.config.ts
src/lib/api.ts
```

示例：

```text
页面访问：/api/test/database
代理到：  http://localhost:8080/api/test/database
```

当前工作台接口：

```text
GET  /api/chat/models
GET  /api/chat/preferences
PUT  /api/chat/preferences
GET  /api/chat/conversations
GET  /api/chat/conversations/{conversationId}
POST /api/chat/messages
```

## 可用命令

```powershell
pnpm lint
pnpm typecheck
pnpm build
```

## 最近验证

2026-06-16 已验证：

- `pnpm install` 成功。
- `pnpm lint` 通过。
- `pnpm typecheck` 通过。
- `pnpm build` 通过。
- `http://127.0.0.1:3000` 返回 200。
- 首页为小说工坊工作台 UI，包含可折叠左侧栏、聊天记录主视图、固定底部输入框、默认粉色主题、多主题切换、自定义主题色、模型下拉和抽象动态光效。
- 主题、自定义色、侧边栏折叠状态、默认模型、会话和消息都从后端读取并保存。
- 没有聊天记录时才展示 32px 空白态主标题。
- `http://127.0.0.1:3000/api/test/database` 可通过 rewrite 访问后端。
- 临时生产服务 `http://127.0.0.1:3001` 已通过 rewrite 访问 `http://127.0.0.1:18080/api/chat/preferences`。

验证完成后已关闭本次测试启动的 Web 进程。
