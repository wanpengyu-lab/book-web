# book-web-ui（前端 SPA）

Vue 3 + TypeScript + Vite + Tailwind 4 + Pinia + Vue Router，PWA（vite-plugin-pwa）。

## mock 层与 `VITE_USE_MOCK` 开关（PRD v0.10 强制规范）

- `src/api/mock/` 是手写 TS mock 模块（与真实接口同签名），**永久保留在仓库**，供离线开发使用（无后端时 `npm run dev` 可完整走通六页）。
- 开关读取 `VITE_USE_MOCK`：
  - `dev` 用 `.env`：`true`（B 阶段锁定值，离线开发默认）；
  - 生产构建用 `.env.production`：`false`（C 阶段锁定值，**生产构建必须为 false**，一切数据来自真实 `/api`）。
- 禁止 MSW / json-server / 任何 mock 服务器依赖（PRD 2.4）。

## 本地开发

```bash
npm install
npm run dev          # mock 模式；如需连真实后端，临时改 .env 为 false（vite 已配置 /api 代理 → localhost:8080）
```

## 生产构建与部署

```bash
npm run build        # 读 .env.production（VITE_USE_MOCK=false），产物在 dist/
```

构建产物整体替换后端 `src/main/resources/static/`（SPA fallback：非 /api 路径回 index.html），随 Spring Boot 打包经 compose 部署。
