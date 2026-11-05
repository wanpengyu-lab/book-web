# boook-web · 图书管理系统

Spring Boot 4（Java 17）+ MyBatis + MySQL 后端，Vue 3 + TypeScript + Vite 前端（`book-web-ui/`）。
功能：馆藏检索（服务端搜索/分类/分页）· 借阅闭环（并发安全）· 预约与站内通知 · 看板 · ISBN 建档代理 · DeepSeek 馆藏助手 · PWA。

按 `docs/PRD.md` 分 11 个小版本（v0.1–v0.10 + v1.0）交付完成，API 契约以 `docs/api-contract.md` 为准。

## 一、环境变量总表

| 变量 | 用途 | 默认值 | 必填 |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | 数据库连接串 | 无（本地默认可写 `jdbc:mysql://localhost:3306/jdbc_demo?characterEncoding=UTF-8`） | 是 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | 无 | 是 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码（仓库内零明文，只走环境变量） | 无 | 是 |
| `JWT_SECRET` | JWT 签名密钥（≥32 字节，不足时启动快速失败） | 无 | 是 |
| `ADMIN_INIT_PASSWORD` | admin 初始密码：库中无 admin 时创建；无 admin 且未配置 → **启动失败**（禁止默认密码兜底） | 空 | 条件必填 |
| `BORROW_DAYS` | 借期天数（应还日期 = 借出 + N 天） | `30` | 否 |
| `ISBN_PROXY_TIMEOUT` | ISBN 外部书源超时（毫秒） | `5000` | 否 |
| `DEEPSEEK_API_KEY` | AI 馆藏助手（DeepSeek 开放平台，OpenAI 兼容协议）；未配置时接口返回明确提示、不 500 | 空 | 用 AI 则必填 |
| `DEEPSEEK_MODEL` | 覆盖默认模型 | `deepseek-chat` | 否 |
| `VITE_USE_MOCK` | 前端数据源开关（构建时读取）：`true` 走 `src/api/mock/` 手写 mock（离线开发用，层永久保留）；**生产构建必须 `false`**（走真实 `/api`） | dev `.env` 为 `true`；`.env.production` 为 `false` | 构建时 |

## 二、本地开发启动

### 1. 数据库

本机 MySQL（localhost:3306）创建库 `jdbc_demo`，并按版本顺序执行一次建表脚本：

```bash
mysql -uroot -p < init.sql            # 建 book 表 + 5 条示例数据
mysql -uroot -p jdbc_demo < docs/sql/V0_2__book_auto_increment.sql
mysql -uroot -p jdbc_demo < docs/sql/V0_3__app_user.sql
mysql -uroot -p jdbc_demo < docs/sql/V0_4__borrow_record.sql
mysql -uroot -p jdbc_demo < docs/sql/V0_5__book_fields.sql
mysql -uroot -p jdbc_demo < docs/sql/V0_6__reservation_notification.sql
```

### 2. 后端（端口 8080）

```bash
# Windows PowerShell 示例：环境变量从安全渠道注入，禁止写入任何文件
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/jdbc_demo?characterEncoding=UTF-8"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "<你的数据库密码>"
$env:JWT_SECRET = "<至少32字节的随机串>"
$env:ADMIN_INIT_PASSWORD = "<库中无 admin 时的初始密码>"   # 条件必填，见上表
./mvnw spring-boot:run
```

启动后验证：`curl http://localhost:8080/api/stats` 返回 `{"code":0,...}`。

### 3. 前端（端口 5173）

```bash
cd book-web-ui
npm install
npm run dev      # .env 默认 VITE_USE_MOCK=true：mock 数据离线开发
```

- 需连真实后端联调：临时把 `.env` 的 `VITE_USE_MOCK` 改为 `false`（vite 已预配 `/api` 代理 → `http://localhost:8080`），**不要提交该改动**。
- mock 层说明见 `book-web-ui/README.md`。

## 三、生产部署（compose 一键）

```bash
# 1. 前端生产构建（读 .env.production，VITE_USE_MOCK=false），产物替换后端 static/
cd book-web-ui && npm run build && cd ..
rm -rf src/main/resources/static/*
cp -r book-web-ui/dist/. src/main/resources/static/

# 2. compose 构建并启动（fastapi-net 网络与 mysql 容器准备见 部署指南.md）
#    全部凭据经环境变量显式传入：MYSQL_ROOT_PASSWORD、JWT_SECRET、ADMIN_INIT_PASSWORD、DEEPSEEK_API_KEY
docker compose up -d --build

# 3. 健康检查（约 30s 内变 healthy）
docker inspect --format '{{.State.Health.Status}}' boook-web   # 期望 healthy
curl http://localhost:8080/api/stats
```

SPA 已配 fallback：`/books/1`、`/records` 等深链刷新直达；非 `/api` 未匹配 GET 回 `index.html`。

## 四、目录与文档

| 路径 | 说明 |
|---|---|
| `src/main/java/org/example/bookweb/` | 后端（controller/service/mapper/dto/interceptor/config） |
| `book-web-ui/` | 前端 SPA（Vue 3 + Vite + Tailwind v4 + Pinia） |
| `docs/PRD.md` | 产品需求文档（11 版交付与锁定表） |
| `docs/api-contract.md` | 全量 API 契约（已冻结） |
| `docs/sql/` | v0.2–v0.6 增量 DDL 记录 |
| `部署指南.md` | compose 全流程部署手册 |
| `init.sql` | 初始建库脚本 |

## 五、测试

```bash
./mvnw test          # 后端 54 个测试（含并发借阅硬门槛 ConcurrentBorrowTest）
cd book-web-ui && npm run build   # 前端 TS strict 构建零报错
```

接口自测样本见 `src/test.http`。
