# boook-web API 契约清单（v0.7 · 契约冻结版）

> 生成依据：以代码为准（PRD v0.7 强制规范：禁止手写漂移）。示例响应均来自本机实测（2026-11-06）。
> 本文件为阶段 B（前端 mock）的唯一接口依据。**v0.7 验收通过即契约冻结（PRD 2.4）**；
> 冻结后前端发现契约问题 → 停止上报 6.2，不得擅自改后端。

## 0. 通用约定

### 0.1 统一响应结构

所有接口一律返回 `Result<T>`，字段顺序固定：

```json
{ "code": 0, "message": "ok", "data": { } }
```

### 0.2 错误码（PRD 3.1）

| code | HTTP | 含义 |
|---|---|---|
| 0 | 200 | 成功（含外部数据源未命中/未配置的 200 + data:null + message 约定） |
| 40001 | 400 | 参数校验失败（含请求方法不支持、路径参数类型错误） |
| 40101 | 401 | 未登录 / token 无效或过期 |
| 40301 | 403 | 无权限（需要 admin、查他人记录） |
| 40401 | 404 | 资源不存在 |
| 40901 | 409 | 业务冲突（书刚被借走、用户名已存在、重复预约） |
| 50001 | 500 | 服务器错误（只回概要，不回堆栈） |

### 0.3 鉴权

- Header：`Authorization: Bearer <token>`；token 由 `POST /api/auth/login` 签发，HS256，24h。
- **开放接口**（无需 token）：`GET /api/stats`、`GET /api/books`（列表）、`GET /api/books/{数字id}`（详情）、`/api/auth/**`。
- **登录接口**：其余 `/api/**` 一律需要登录，包括 `GET /api/books/{id}/related`、`GET /api/records`、`GET /api/notifications`、`GET /api/dashboard`、`POST /api/assistant/chat`、借/还/预约。
- **admin 接口**：`POST/PUT/DELETE /api/books/**`、`GET /api/isbn/{isbn}`。

### 0.4 外部数据源约定（v0.7 拍板）

- `GET /api/isbn/{isbn}` 未命中 / 外部失败超时：**HTTP 200 + code 0 + data:null + 明确 message**，不抛 5xx。
- `POST /api/assistant/chat` 未配置 `GLM_API_KEY`：**HTTP 200 + code 0 + data:null + message「AI 服务未配置」**。

---

## 1. 认证

### POST /api/auth/register — 注册（开放）

请求：`{"username": "string", "password": "string"}`
响应：`{"code":0,"message":"ok","data":true}`；用户名重复 → 409 + 40901。

### POST /api/auth/login — 登录（开放）

请求：`{"username": "string", "password": "string"}`
响应：

```json
{"code":0,"message":"ok","data":{"token":"eyJ...","user":{"id":7,"username":"v06bob","role":"reader"}}}
```

---

## 2. 图书

### GET /api/books — 列表（开放）

参数：`keyword=`（title/author LIKE 两端 %）、`category=`（精确匹配，v0.5）、`page=`（默认 1）、`size=`（默认 5，上限 100）。keyword 与 category 可组合；服务端完成搜索与分页。

```json
{"code":0,"message":"ok","data":{"total":13,"page":1,"size":5,"list":[{"id":16,"title":"三体","author":"刘慈欣","borrowed":false,"category":"科幻","publisher":"重庆出版社","isbn":"9787536692930","coverUrl":"https://example.com/covers/santi2.jpg","description":"更新后的简介。","totalCopies":5}]}}
```

### GET /api/books/{id} — 聚合详情（开放，v0.5）

一次请求取齐：全量字段 + 最近 5 条借阅记录（新→旧）+ 共借推荐 id 列表（v0.6 起真实，无共借退化为同分类最近入库）。

```json
{"code":0,"message":"ok","data":{"id":3,"title":"百年孤独","author":"加西亚·马尔克斯","borrowed":true,"category":"未分类","publisher":null,"isbn":null,"coverUrl":null,"description":null,"totalCopies":1,"recentRecords":[{"id":23,"bookId":3,"userId":5,"borrowedAt":"2026-11-06T05:02:57","dueAt":"2026-12-06T05:02:58","returnedAt":null,"status":"active"}],"relatedIds":[1,2]}}
```

书不存在 → `data:null`（HTTP 200，沿用 v0.3 行为）。

### POST /api/books — 新增（admin）

请求体（v0.5 扩展字段均可选）：`{"title":"必填","author":"必填","category":"≤50 字符","publisher":"≤100","isbn":"≤20","coverUrl":"≤500","description":"文本","totalCopies":≥1}`。
category 缺省落库「未分类」，totalCopies 缺省 1。响应 `{"code":0,"message":"ok","data":true}`。

### PUT /api/books/{id} — 编辑（admin）

请求体同 POST；id 只从路径读取。响应 `{"code":0,"message":"ok","data":true}`。

### DELETE /api/books/{id} — 删除（admin）

响应 `{"code":0,"message":"ok","data":true}`。

### PUT /api/books/{id}/borrow — 借出（登录）

服务端计算 due_at（BORROW_DAYS 默认 30 天）；并发条件更新互斥，抢锁失败 → 409 + 40901「该书刚被借走」。
响应 `{"code":0,"message":"ok","data":true}`。借出时借阅人若有本人预约（notified/waiting）→ 该预约置 fulfilled。

### PUT /api/books/{id}/return — 归还（登录）

同一事务内回写记录 + 释放在借标志；该书有 waiting 预约 → 队首（最早创建）置 notified 并生成站内通知。

### POST /api/books/{id}/reserve — 预约（登录，v0.6）

规则：在借状态才可约；在馆书 → 400 + 40001；同人同书已有 waiting/notified → 409 + 40901；书不存在 → 404 + 40401。
响应 `{"code":0,"message":"ok","data":true}`。

### GET /api/books/{id}/related — 共借推荐（登录，v0.6）

借过该书的人还借过的书，按共借次数降序前 6，返回 id 数组；**无共借数据必须退化为「同分类最近入库 LIMIT 6」（排除自身），不允许返回空列表**（分类内仅剩自身时才可能为空）。

```json
{"code":0,"message":"ok","data":[2,3]}
```

---

## 3. 记录 / 通知 / 看板

### GET /api/records — 借阅记录（登录）

参数：`status=`（active / returned / overdue，overdue 派生）、`userId=`（仅 admin 可传）、`page=`、`size=`（上限 100）。reader 只看本人（查他人 → 403 + 40301）。

**【拍板项 b】** 每条记录含 `bookTitle` / `bookAuthor`（服务端 JOIN book 一次取齐，前端禁止拼请求）：

```json
{"code":0,"message":"ok","data":{"total":1,"page":1,"size":2,"list":[{"id":61,"bookId":16,"userId":7,"borrowedAt":"2026-11-06T05:20:17","dueAt":"2026-12-06T05:20:18","returnedAt":"2026-11-06T05:21:41","status":"returned","bookTitle":"三体","bookAuthor":"刘慈欣"}]}}
```

### GET /api/notifications — 站内通知（登录，v0.6）

未读优先（read=false 在前），同状态最新在前；参数 `page=`、`size=`（上限 100）。

```json
{"code":0,"message":"ok","data":{"total":1,"page":1,"size":5,"list":[{"id":6,"userId":7,"content":"您预约的《三体》已归还，当前可借，请及时处理","read":false,"createdAt":"2026-11-06T05:20:12"}]}}
```

### GET /api/stats — 全库统计（开放）

```json
{"code":0,"message":"ok","data":{"available":13,"borrowed":0,"categories":[{"count":12,"name":"未分类"},{"count":1,"name":"科幻"}],"total":13}}
```

categories 为 GROUP BY category 的真实分类统计，按 count 降序。

### GET /api/dashboard — 看板（登录，v0.5）

`data: {trend, categoryRatio, topBorrowed}`；三组均来自 SQL 聚合；空库返回空数组不报错。

```json
{"code":0,"message":"ok","data":{"trend":[{"date":"2026-11-06","count":23}],"categoryRatio":[{"name":"未分类","value":12},{"name":"科幻","value":1}],"topBorrowed":[{"id":1,"title":"三体","author":"刘慈欣","count":19}]}}
```

**【拍板项 a】** `trend` 仅含有借出的日期（稀疏返回，近 30 天窗口）；B 阶段前端如需连续 30 天横轴，**由前端补零**，后端不零填充。

---

## 4. 开放数据 / AI（v0.7）

### GET /api/isbn/{isbn} — ISBN 查询（admin）

- `{isbn}`：1–20 位纯数字，否则 400 + 40001。
- 数据源顺序：OpenLibrary → Google Books（依次尝试），超时 `ISBN_PROXY_TIMEOUT`（默认 5000ms）。
- 命中 → `data: {title, author, publisher, coverUrl, description}`（仅供建档预填，不落库）：

```json
{"code":0,"message":"ok","data":{"title":"三体","author":"刘慈欣","publisher":"重庆出版社","coverUrl":"https://...","description":"..."}}
```

- 未命中 → `{"code":0,"message":"未查到该书目，请手动录入","data":null}`；
- 外部失败/超时 → `{"code":0,"message":"外部书源暂时不可用，请稍后重试或手动录入","data":null}`（均 HTTP 200，不 500）。

### POST /api/assistant/chat — AI 馆藏助手（登录）

请求：`{"message": "馆里有哪些科幻小说"}`
响应：`{"code":0,"message":"ok","data":{"reply":"..."}}`。

- 智谱 GLM（`GLM_API_KEY`），模型默认 `glm-4.6`（仅 `GLM_MODEL` 可覆盖），function calling 仅 `search_books(keyword,category)` 与 `get_book_status(id)`，非流式；回答必须基于工具返回的真实馆藏，查不到答「馆内没有」。
- 未配置 key → `{"code":0,"message":"AI 服务未配置","data":null}`（HTTP 200）；GLM 调用失败 → 200 + code 0 + data:null +「AI 服务暂时不可用，请稍后重试」。

---

## 5. 旧端点（不在本契约内）

无 `/api` 前缀的旧端点（`/books`、`/books/page`、`/books/{id}`、`/books/{id}/borrowed`、`/books/{id}/return` 等）仅维持旧 index.html 可用至 v0.10，**契约冻结不覆盖**，v0.10 统一删除。
