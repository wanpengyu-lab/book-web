# boook-web 图书管理系统升级 · 开发交付单（PRD v3.0 · 三阶段分版交付）

| 项 | 内容 |
|---|---|
| 文档版本 | **v3.0**（替代 v2.0；执行顺序重构为「后端 → 前端 → 相连」三阶段） |
| 日期 | 2026-11-05 |
| 适用对象 | 编码 Agent（ZCode）/ 开发者 |
| 编写依据 | 仓库静态诊断（2026-11-05）+ 用户设计基准图（网页.jpg 七条）+ 用户交付要求（分小版本 / 明确交付物 / 强制规范 / 禁止替换方案 / **先创新后端功能，再写前端，最后相连**） |
| 交付形态 | **阶段 A 后端功能创新（v0.1–v0.7）→ 阶段 B 前端界面（v0.8–v0.9）→ 阶段 C 相连集成（v0.10–v1.0）**，共 11 个小版本，按序执行、逐版验收 |

---

## 0. 执行总则（编码 Agent 必读）

### 0.1 版本流水线纪律

1. **三阶段强顺序**：阶段 A（v0.1–v0.7，后端全部功能）**全部验收通过**后才允许开工阶段 B（v0.8–v0.9，前端界面）；阶段 B 全部验收通过后才允许进入阶段 C（v0.10–v1.0，相连与发布）。**禁止前后端并行，禁止提前做后续阶段的内容。**
2. **按序执行**：版本内不允许跳版；一版一验收，当前版本验收清单全勾 + 通用 DoD（0.3）满足，才允许进入下一版。
3. **一版一会话**：建议每版本开一个新的 ZCode 会话，粘贴范围 = 第 0–3 章（全局）+ 目标版本章节。
4. **交付物逐项落盘**：每版「交付物」清单中的文件/接口/DDL/页面必须全部真实存在，缺一项即未完成。

### 0.2 方案锁定原则（本单核心约束）

- **第 2 章「方案锁定总表」是唯一技术事实源**。所有选型已定死，不存在备选项。
- **禁止自由替换**：不得以任何理由（「更熟悉」「依赖装不上」「版本冲突」）替换锁定方案为其他库/框架/模式。
- **遇阻唯一出口 = 停止上报**：锁定方案无法实现时，停下当前任务，在 6.2「遗留问题记录」追加一行（现象 / 已尝试 / 需要用户决策什么），等待用户决定。**不得自行换方案绕过**。
- 锁定方案的变更只能由用户发起，且需更新第 2 章后再继续。

### 0.3 通用 DoD（每个版本都必须满足）

- [ ] 后端有改动的版本：`./mvnw test` 全绿
- [ ] 前端有改动的版本（v0.8 起）：`npm run build` 成功且产物已拷入 `src/main/resources/static/`
- [ ] 应用可启动，旧功能回归无失败（上一版验收清单抽测通过）
- [ ] 交付物清单逐项核对完成
- [ ] 按提交规范完成 commit（3.5）
- [ ] 未引入第 2 章之外的任何新依赖

### 0.4 本单覆盖范围之外

- 不做：Redis / 消息队列 / Elasticsearch / Flyway / Liquibase / 微服务拆分。
- 不改：`docker-compose.yml` 的网络架构（fastapi-net + mysql 容器名）。
- 前端禁止：Element Plus / Ant Design Vue / jQuery / CSS-in-JS / 任何第二套组件库。

---

## 1. 项目概述

### 1.1 背景与目标

boook-web 为 Spring Boot 4.1.2 + MyBatis + MySQL 的图书管理系统，已跑通单表 CRUD、借还标志位、分页、Docker 部署的最小闭环。本单将其升级为：数字可信（全库统计/搜索）、业务闭环（用户 + JWT + 并发安全的借阅记录）、现代前端（Vue 3 + Tailwind + shadcn-vue，按设计基准图「极简、科技、高级、产品感强」）、有差异化亮点（ISBN 扫码、AI 馆藏助手、共借推荐、看板）。

**交付顺序**：先创新后端功能（全部接口可被 curl/Swagger 验收），再写前端界面（基于冻结的 API 契约 + mock 数据开发），最后相连（切换真实 API、删旧端点、端到端集成验收）。

### 1.2 现状问题索引（G 编号，功能依据）

| 编号 | 位置 | 问题 |
|---|---|---|
| G1 | `BookMapper.setBorrowed` | 借还只翻标志位，无借阅人/时间/历史，并发双成功 |
| G2 | `index.html · updateStatsForCurrentPage` | 统计只算当前页 5 条 |
| G3 | `index.html · handleSearch` | 搜索只过滤当前页 |
| G4 | `pom.xml · jjwt` | JWT 依赖闲置，无用户/鉴权 |
| G5 | `init.sql` + 表单 | 主键手填编号 |
| G6 | `BookDao` / `DBHelper` | JDBC 遗留副本，硬编码密码 |
| G7 | `application.properties` / compose | 明文密码 + 默认值兜底 |
| G8 | `BookController.addBook` | 无参数校验，畸形请求 500 |
| G9 | `test/` | 仅占位测试 |
| G10 | `index.html`（约 700 行） | 无响应式/路由/主题 |

环境事实：后端 8080；库 `jdbc_demo`，现有表 `book(id,title,author,is_borrowed)`；部署走 compose + fastapi-net + mysql 容器；Java 17 / Spring Boot 4.1.2 / MyBatis starter 4.1.0 / jjwt 0.12.6（已在 pom）。

### 1.3 版本总览（三阶段 · 任务映射）

| 版本 | 阶段 | 主题 | 覆盖任务 | 核心交付 | 预估 |
|---|---|---|---|---|---|
| v0.1 | **A 后端** | 工程卫生与规范落地 | F-04 | Result 统一响应、错误码、校验、删遗留代码、springdoc、密码外置 | 1 天 |
| v0.2 | A | 数据与接口修正 | F-01 · 02 · 03 | /api/stats、/api/books 统一列表、主键自增 | 1 天 |
| v0.3 | A | 用户与鉴权 | F-05 | 注册/登录/JWT 拦截器/admin 初始化 | 1–2 天 |
| v0.4 | A | 借阅闭环 | F-06 · 07 | borrow_record、并发安全借还、记录查询 | 2 天 |
| v0.5 | A | 图书扩展与看板接口 | F-08 · 09 · 14 | book 字段扩展、详情聚合、/api/dashboard | 1.5 天 |
| v0.6 | A | 预约与共借推荐接口 | F-10 · 13 | 预约状态机、通知、定时提醒、related 接口 | 1.5 天 |
| v0.7 | A | 开放数据与 AI 接口 | F-11 · 12 | ISBN 代理、GLM 馆藏助手（API 契约冻结点） | 2 天 |
| v0.8 | **B 前端** | 前端工程与六页（mock） | 界面章 5.1–5.2 | book-web-ui 工程、mock 层、六页 + 全局框架 | 3–4 天 |
| v0.9 | B | 界面完整化与亮点（mock） | 界面章 5.3–5.8 | 动效/交互/响应式/看板图表/AI 侧栏/PWA/3D | 3 天 |
| v0.10 | **C 相连** | 真实连接与切换 | 集成 | mock→真实 API、删旧端点与旧页面、端到端验收 | 1–2 天 |
| v1.0 | C | 发布回归 | 全量 | 全版本回归、README/部署文档、healthcheck、tag | 0.5 天 |

---

## 2. 方案锁定总表（唯一技术事实源 · 禁止替换）

### 2.1 后端锁定

| 决策点 | 锁定值 | 明确禁止的替代 |
|---|---|---|
| 框架 | Spring Boot 4.1.2 / Java 17，维持现状 | 升级/降级版本 |
| ORM | MyBatis **注解 SQL**（`@Select/@Insert/@Update/@Delete`） | JPA / JOOQ / XML mapper |
| 密码哈希 | `spring-security-crypto` 的 `BCryptPasswordEncoder` | 完整 spring-security 框架、MD5/SHA |
| 鉴权 | jjwt 0.12.6，HS256，有效期 24h，Header `Authorization: Bearer <token>` | Spring Security 过滤链、Session、OAuth |
| 统一响应 | `Result<T>{code, message, data}` + 3.1 错误码表 | 各接口自定义响应结构 |
| 参数校验 | `spring-boot-starter-validation` + `@Valid` + DTO | 手写 if 校验 |
| 接口文档 | springdoc-openapi（与 Boot 4.1.2 兼容的最新稳定版） | knife4j / springfox / 手写文档 |
| 代码简化 | Lombok（新代码 `@Data` / `@RequiredArgsConstructor` + final 构造注入） | 字段注入 `@Autowired`（新代码禁止） |
| DDL 管理 | 手动 SQL 文件 `docs/sql/V{major}_{minor}__描述.sql`，按版本顺序执行一次 | Flyway / Liquibase / JPA 自动建表 |
| 外部 HTTP | Spring `RestClient`（ISBN 代理、GLM 调用） | RestTemplate / WebClient / OkHttp |
| 定时任务 | `@Scheduled`，cron `0 0 9 * * *`，zone `Asia/Shanghai` | Quartz / xxl-job |
| API 前缀 | 新接口一律 `/api` 前缀；旧无前缀端点**保留至 v0.10 统一删除**（A 阶段仅标 `@Deprecated`） | 提前删除旧端点 |
| admin 初始化 | `CommandLineRunner`：无 admin 账号且配置了 `ADMIN_INIT_PASSWORD` 则创建；未配置且无 admin 时**启动失败**并提示 | init.sql 插账号、默认密码兜底 |
| 越权响应 | reader 访问他人记录返回 **403**（code 40301） | 返回空列表静默通过 |
| 借期 | `BORROW_DAYS` 环境变量，默认 30 天 | 写死在代码 |

### 2.2 前端锁定

| 决策点 | 锁定值 | 明确禁止的替代 |
|---|---|---|
| 框架 | Vue 3（`<script setup>` + **TypeScript strict**） | React / Svelte / Web Components |
| 构建 | Vite（当前最新稳定版） | webpack / rsbuild |
| 包管理 | npm（Node ≥ 20） | pnpm / yarn |
| 样式 | Tailwind CSS v4（CSS-first `@theme` 令牌） | Element Plus 内置样式、CSS Modules、CSS-in-JS、UnoCSS |
| 组件库 | shadcn-vue（Reka UI 无头基座）：Button / Card / Input / Dialog / Toast / Empty State | Element Plus / Ant Design Vue / Naive UI / 任何第二套组件库 |
| 复杂表格 | TanStack Vue Table + Tailwind 定制 | vxe-table / 自研表格 |
| 状态 | Pinia | Vuex |
| 路由 | Vue Router（history 模式） | hash 模式 / 自研路由 |
| HTTP | 原生 `fetch` 封装（`src/api/http.ts`） | axios |
| 图表 | ECharts + vue-echarts，按需注册 Line/Pie/Bar | Chart.js / D3 |
| 动效 | **motion-v** | @vueuse/motion / GSAP / CSS 动画库 |
| 3D | TresJS + three，仅 /login 一页，路由级懒加载 | React Three Fiber / Babylon / 全站 3D |
| PWA | vite-plugin-pwa（registerType: autoUpdate） | 手写 SW / Workbox CLI |
| 主题 | 深色为默认；令牌切换亮色；跟随 `prefers-color-scheme` | 只做单主题 |
| 令牌命名 | shadcn 惯例 CSS 变量（`--background/--foreground/--card/--border/--primary/--muted/--radius`…） | 自创命名体系 |

### 2.3 数据库与接口锁定

| 决策点 | 锁定值 |
|---|---|
| 表名 | `book` / `app_user` / `borrow_record` / `reservation` / `notification`（不得改名、不得加前缀） |
| 逾期口径 | 派生态：`status='active' AND due_at < NOW()`，**不落库、不冗余字段** |
| 借阅并发 | 条件更新 `UPDATE book SET is_borrowed=1 WHERE id=? AND is_borrowed=0`，影响行数=0 → 40901「该书刚被借走」 |
| 预约状态机 | `waiting → notified → fulfilled`，取消为 `cancelled`；还书事务内通知队首 |
| ISBN 数据源顺序 | OpenLibrary → Google Books（依次尝试），超时 5s（`ISBN_PROXY_TIMEOUT`） |
| AI 工具集 | 仅两个：`search_books(keyword/category)`、`get_book_status(id)`；非流式 |
| GLM 接入 | 智谱开放平台，key 走 `GLM_API_KEY`，模型名默认常量 `glm-4.6`、可被 `GLM_MODEL` 覆盖；模型不存在 → 阻塞上报，禁止换模型 |
| 旧端点兼容策略 | A/B 阶段旧无前缀端点保留（`@Deprecated`），旧 index.html 仅做维持可用的最小改动；v0.10 统一删除旧端点与旧页面 |

### 2.4 阶段与契约锁定（三阶段顺序的核心约束）

| 决策点 | 锁定值 |
|---|---|
| 开发顺序 | 阶段 A（v0.1–v0.7 后端）全部验收 → 阶段 B（v0.8–v0.9 前端）全部验收 → 阶段 C（v0.10–v1.0 相连发布）。禁止并行、禁止提前 |
| API 契约冻结 | **v0.7 验收通过即冻结全部 API 契约**（路径/参数/响应结构以第 4 章 A 阶段各版为准）。阶段 B 发现契约问题 → 停止上报 6.2，不得擅自改后端 |
| mock 方案 | 手写 TS mock 模块（`src/api/mock/`），与真实接口同签名，`Promise` + 200ms 延迟模拟网络；开关 `VITE_USE_MOCK`（B 阶段锁定 `true`，C 阶段锁定 `false`）。**禁止 MSW / json-server / 任何 mock 服务器依赖** |
| mock 数据集 | 集中放 `src/api/mock/data.ts`：≥12 本书（覆盖各分类与在借/在馆状态）、3 个用户（含 admin）、≥8 条借阅记录（含逾期与已还样例）、看板样例数组、AI 问答样例对 |
| 旧 UI 过渡 | A/B 阶段旧 `index.html` 仅维持可用（逐版最小改动），禁止美化；v0.10 删除 |
| 前端数据源 | B 阶段一切数据来自 mock 层，组件禁止裸 fetch；C 阶段一切数据来自真实 API |

### 2.5 锁定变更流程

任何对第 2 章的偏离：**停止 → 记录到 6.2 → 上报用户 → 用户改表后才继续**。编码 Agent 无权解释性变通。

---

## 3. 全局强制规范

### 3.1 统一响应与错误码（v0.1 起生效）

```json
{ "code": 0, "message": "ok", "data": { } }
```

| code | HTTP | 含义 |
|---|---|---|
| 0 | 200 | 成功 |
| 40001 | 400 | 参数校验失败 |
| 40101 | 401 | 未登录 / token 无效或过期 |
| 40301 | 403 | 无权限 |
| 40401 | 404 | 资源不存在 |
| 40901 | 409 | 业务冲突（书刚被借走、用户名已存在、重复预约） |
| 50001 | 500 | 服务器错误（只回概要，不回堆栈） |

### 3.2 代码与包结构规范（v0.1 起生效）

```
org.example.bookweb
├─ controller/   AuthController · BookController · RecordController ·
│                StatsController · ReservationController ·
│                IsbnController · AssistantController · NotificationController
├─ service/      AuthService · BookService · BorrowService · DashboardService ·
│                ReservationService · IsbnService · AssistantService
├─ mapper/       BookMapper · UserMapper · BorrowRecordMapper ·
│                ReservationMapper · NotificationMapper
├─ entity/       Book · User · BorrowRecord · Reservation · Notification
├─ dto/          Result<T> · 各请求/响应 DTO
├─ interceptor/  JwtInterceptor
├─ config/       WebConfig · OpenApiConfig · AdminInitializer
├─ schedule/     OverdueScanJob
└─ BoookWebApplication
```

- Service 一律 `@RequiredArgsConstructor` + `final` 字段注入；SQL 全部 MyBatis 注解；DTO 与 entity 分离，Controller 不直接收 `Map`。
- 新增 Java 文件必须落入上表对应包；上表未列出的包不创建。

### 3.3 安全底线

- SQL 一律参数化（`#{}` / `?`）；前端输出转义（Vue 模板默认转义，禁用 `v-html`）。
- 仓库内不得出现任何明文凭据：`application.properties` 只留 `${SPRING_DATASOURCE_PASSWORD}` 占位；compose 用 `${MYSQL_ROOT_PASSWORD:?err}` 强制显式传入。
- JWT secret / GLM key / admin 初始密码只走环境变量，**任何前端代码/构建产物不得包含密钥**。

### 3.4 测试底线

- 每版 `./mvnw test` 全绿；关键路径单测为验收硬门槛（见各版验收）。
- 前端不强制单测，但 build 必须零报错（TS strict）。

### 3.5 提交与版本纪律

- 每完成一个版本 commit 一次，格式：`v{X.Y}: {一句话}`（例：`v0.4: 借阅闭环与并发安全`）。
- commit 前核对：交付物清单全在、DoD 全过、无未跟踪的临时文件。
- v1.0 打 tag `v1.0`。

---

## 4. 版本交付计划

> 阶段 A（v0.1–v0.7）：只做后端。验收手段 = Swagger UI + curl + 单测；旧 index.html 仅跟随接口做最小改动维持可用。
> 阶段 B（v0.8–v0.9）：只做前端，数据全部来自 mock。验收手段 = `VITE_USE_MOCK=true` 下页面自查 + build。
> 阶段 C（v0.10–v1.0）：相连、删旧、端到端、发布。

---

### 阶段 A · 后端功能创新

#### v0.1 · 工程卫生与规范落地（F-04）

**前置**：无。**目标**：代码可被信任——统一响应、校验、文档、删遗留、密码治理，后续所有版本踩在这套规范上。

**交付物**：

| 类 | 内容 |
|---|---|
| 删除 | `BookDao.java` · `DBHelper.java` · `HelloController.java` |
| 新增 | `dto/Result.java` · `dto/BookRequest.java` · `config/OpenApiConfig.java` |
| 修改 | `pom.xml`：+ springdoc-openapi、+ spring-security-crypto、+ spring-boot-starter-validation、+ Lombok |
| 修改 | `BookController`：入参改 `@Valid BookRequest`，返回 `Result<T>`；`BookService`/`BookMapper`/`Book` 迁入 service/mapper/entity 包，`Book` 改 `@Data` |
| 修改 | `GlobalExceptionHandler`：按 3.1 错误码表重写（`MethodArgumentNotValidException`→40001、`DuplicateKeyException`→40901、兜底→50001） |
| 修改 | `application.properties`：密码改 `${SPRING_DATASOURCE_PASSWORD}`，无默认值；`docker-compose.yml`：去默认值兜底，改 `${MYSQL_ROOT_PASSWORD:?必须显式提供}` |
| 测试 | `PageBoundaryTest`：page=0 / 负数 / size=0 / size>100 |

**本版强制规范**：
- Result 字段顺序固定 `code, message, data`；所有接口（含旧端点改造后）一律返回 Result。
- `/swagger-ui.html` 必须可打开并展示全部接口。

**本版禁止**：
- 禁止动任何表结构（本版无 DDL）；禁止改 `index.html`；禁止引入第 2 章之外依赖。

**验收**：
- [ ] `grep -ri "123456\|1qaz3edc" --include="*.properties" --include="*.yml"` 无命中
- [ ] 缺字段 / 类型错误请求返回 400 + code 40001（不再是 500）
- [ ] 重复编号返回 409 + 40901
- [ ] `./mvnw test` 全绿；swagger 可访问
- [ ] `BookDao` / `DBHelper` / `HelloController` 已不存在

---

#### v0.2 · 数据与接口修正（F-01 · 02 · 03）

**前置**：v0.1 验收通过。**目标**：统计与搜索全库准确、主键自增（修 G2 / G3 / G5）。

**交付物**：

| 类 | 内容 |
|---|---|
| DDL | `docs/sql/V0_2__book_auto_increment.sql`：`ALTER TABLE book MODIFY id INT NOT NULL AUTO_INCREMENT;`（执行一次，文件头注明） |
| 新增接口 | `GET /api/stats`（开放）：`data: {total, available, borrowed, categories:[{name,count}]}`，categories 按 count 降序；category 字段 v0.5 才加，本版 categories 先返回 `[{"name":"全部","count":total}]` 占位并注释 `// v0.5 切换真实分类` |
| 新增接口 | `GET /api/books?keyword=&page=&size=`（开放）：keyword 匹配 title/author（LIKE 两端 `%`）；返回 `data: {total, page, size, list}`；size 上限 100 |
| 修改接口 | `POST /api/books` 不再接收 id（自增）；本版暂无鉴权（v0.3 加锁，注明过渡态） |
| 修改 | `BookMapper`：+ `search` / `countByKeyword` / 分类统计 SQL |
| 修改 | 旧端点 `/books/page` 标 `@Deprecated`（v0.10 删除） |
| 修改前端 | 旧 `index.html` 最小改动：统计卡改调 `/api/stats`（Result 解包）；搜索改服务端（keyword 随分页请求 `/api/books`）；新增表单删编号输入框 |
| 测试 | `StatsConsistencyTest`（12 条数据翻页后统计仍全库）· `SearchAcrossPagesTest`（keyword 命中跨页数据） |

**本版强制规范**：
- 搜索必须服务端完成，前端不得本地过滤；统计数字只能来自 SQL 聚合，禁止前端累加。

**本版禁止**：
- 禁止上 MySQL FULLTEXT / Meilisearch / ES（LIKE 即可，量大由用户发起升级）。

**验收**：
- [ ] 12 条数据、size=5 翻到第 3 页，统计三数字 = 全库真值
- [ ] keyword 命中分布在不同页的数据时，翻页保持搜索条件
- [ ] 无结果返回空 list + total=0，不报错
- [ ] 连续新增两本书无编号输入，id 自动递增；存量 5 条示例数据完好
- [ ] `./mvnw test` 全绿

---

#### v0.3 · 用户与鉴权（F-05）

**前置**：v0.2 验收通过。**目标**：注册 / 登录 / JWT 拦截 / 角色控制（修 G4，启用 jjwt）。

**交付物**：

| 类 | 内容 |
|---|---|
| DDL | `docs/sql/V0_3__app_user.sql`：建 `app_user`（见下） |
| 新增 | `entity/User` · `mapper/UserMapper` · `dto/LoginRequest` / `RegisterRequest` · `AuthController` · `AuthService` · `interceptor/JwtInterceptor` · `config/WebConfig` · `config/AdminInitializer` |
| 新增接口 | `POST /api/auth/register` `{username,password}` → 注册即 reader，用户名重复 40901 |
| 新增接口 | `POST /api/auth/login` → `data: {token, user:{id,username,role}}` |
| 拦截规则 | 放行：`GET /api/stats`、`GET /api/books`（含详情）、`/api/auth/**`、`/swagger-ui/**`、`/v3/api-docs/**`；`POST/PUT/DELETE /api/books/**` 需 **admin**；其余 `/api/**` 需登录；不匹配→40101 |
| token | HS256，secret=`JWT_SECRET` 环境变量，payload `{sub:userId, role, exp}`，24h |
| admin 初始化 | `AdminInitializer`（CommandLineRunner）：无 admin 且配置 `ADMIN_INIT_PASSWORD` → 创建 `admin`；无 admin 且未配置 → **启动失败**，提示 `缺少 ADMIN_INIT_PASSWORD` |
| 修改前端 | 旧 `index.html` 顶部加最简登录条（用户名/密码/登录按钮，token 存内存变量，API 封装统一带 Bearer 头）；不改视觉 |
| 测试 | `AuthTest`：未登录删书 401 / reader 建书 403 / 过期 token 401 |

```sql
CREATE TABLE app_user (
    id            INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'reader',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**本版强制规范**：
- 密码只存 BCrypt 哈希；日志与异常信息不得出现明文密码。
- 拦截器放行表以上表为准，不得临时加白名单。

**本版禁止**：
- 禁止引完整 spring-security；禁止 token 自动续期等扩展（本版只做内存态）。

**验收**：
- [ ] 未登录 `DELETE /api/books/1` → 401 + 40101
- [ ] reader `POST /api/books` → 403 + 40301
- [ ] 手工把 token exp 改过期 → 401 且 message 明确
- [ ] `ADMIN_INIT_PASSWORD=xxx` 启动后可登录 admin；不配置且无 admin → 启动失败提示
- [ ] `./mvnw test` 全绿

---

#### v0.4 · 借阅闭环（F-06 · 07）

**前置**：v0.3 验收通过。**目标**：借还并发安全、有记录、可审计（修 G1）。

**交付物**：

| 类 | 内容 |
|---|---|
| DDL | `docs/sql/V0_4__borrow_record.sql`：建 `borrow_record`（含 idx_book / idx_user / idx_due 索引） |
| 新增 | `entity/BorrowRecord` · `mapper/BorrowRecordMapper` · `BorrowService` · `RecordController` · `dto/BorrowProperties`（`@ConfigurationProperties(prefix="borrow")`，默认 30） |
| 新增接口 | `PUT /api/books/{id}/borrow`（登录用户）· `PUT /api/books/{id}/return`（登录用户） |
| 新增接口 | `GET /api/records?status=&page=&size=`：reader 只看本人（查他人 403），admin 可加 `userId=` 查全部；status = `active` / `returned` / `overdue`（overdue 查询派生） |
| 旧端点 | `PUT /books/{id}/borrowed`、`PUT /books/{id}/return` 标 `@Deprecated`（v0.10 删除） |
| 核心实现 | `BorrowService.borrow`：`@Transactional` 内条件更新 `UPDATE book SET is_borrowed=1 WHERE id=? AND is_borrowed=0`，行数=0 抛 `40901 该书刚被借走`；否则 `INSERT borrow_record(user_id, book_id, due_at=NOW()+BORROW_DAYS)`。`returnBook`：回写 `is_borrowed=0` + `UPDATE ... SET returned_at=NOW(), status='returned' WHERE book_id=? AND user_id=? AND status='active'` |
| 修改前端 | 旧 `index.html`：借/还按钮改调新端点（带 Bearer），仅此而已 |
| 测试 | `ConcurrentBorrowTest`（CountDownLatch 两线程同借一书，断言恰一成功一 40901）**硬门槛** · `OverdueFilterTest`（构造过期 active 记录可被 overdue 筛出） |

```sql
CREATE TABLE borrow_record (
    id           INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    book_id      INT NOT NULL,
    user_id      INT NOT NULL,
    borrowed_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_at       DATETIME NOT NULL,
    returned_at  DATETIME NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'active',
    KEY idx_book (book_id),
    KEY idx_user (user_id),
    KEY idx_due (status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**本版强制规范**：
- 借 / 还必须同一事务；`due_at` 由服务端计算，禁止前端传入。
- 逾期不落库、不加字段，一律查询派生。

**本版禁止**：
- 禁止乐观锁版本号 / SELECT FOR UPDATE 等其他并发方案（锁定条件更新）；禁止借阅记录物理删除（只改状态）。

**验收**：
- [ ] **并发单测通过（两人同借一书恰一人成功）**
- [ ] 借出后 borrow_record 有一条 active，due_at = 借出时间 + 30 天
- [ ] 归还后 returned_at 回写、status=returned；同书可再次被借
- [ ] reader 查他人记录 → 403
- [ ] `./mvnw test` 全绿

---

#### v0.5 · 图书扩展与看板接口（F-08 · 09 · 14）

**前置**：v0.4 验收通过。**目标**：图书数据模型扩展、详情聚合、看板数据接口（全后端）。

**交付物**：

| 类 | 内容 |
|---|---|
| DDL | `docs/sql/V0_5__book_fields.sql`：book 增 `category`（默认'未分类'）/ `publisher` / `isbn` / `cover_url` / `description` / `total_copies`（默认 1） |
| 修改接口 | `GET /api/books` + `category=` 精确过滤；`/api/stats` 的 categories 切换真实分类统计（去掉 v0.2 占位） |
| 修改接口 | `POST /api/books` / `PUT /api/books/{id}` 接收新字段（`@Valid`：category ≤50 字符、isbn ≤20、cover_url ≤500） |
| 新增接口 | `GET /api/books/{id}` 聚合：`data: {...全量字段, borrowed, recentRecords:[最近5条], relatedIds:[]}`（relatedIds 空数组占位，注释 `// v0.6 接入共借`） |
| 新增接口 | `GET /api/dashboard`：`data: {trend:[近30天每日借出量], categoryRatio:[{name,value}], topBorrowed:[前10 {id,title,author,count}]}`；空库返回空数组 |
| 修改 | `BookMapper`：+ 字段映射 / `findByCategory` / `recentRecords` / 看板三组统计 SQL |
| 修改前端 | 旧 `index.html`：无改动（新字段旧页不展示） |
| 测试 | `DashboardTest`（空库空数组、样例数据三组与 SQL 手工核对）· `CategoryFilterTest` |

**本版强制规范**：
- 详情一次请求取齐，禁止前端拼多个请求（为 B 阶段定契约）。
- 旧数据 category 显示「未分类」。

**本版禁止**：
- 禁止加副本级借用逻辑（total_copies 仅存储展示）；禁止前端任何改动。

**验收**：
- [ ] `/api/books?category=科幻` 过滤正确；旧书 category='未分类'
- [ ] 详情响应含全量字段 + recentRecords(≤5) + relatedIds(空)
- [ ] 看板三组数据与 SQL 手工核对一致；空库返回空数组不报错
- [ ] `./mvnw test` 全绿

---

#### v0.6 · 预约与共借推荐接口（F-10 · 13）

**前置**：v0.5 验收通过。**目标**：预约状态机 + 通知 + 定时提醒 + 共借推荐（全后端）。

**交付物**：

| 类 | 内容 |
|---|---|
| DDL | `docs/sql/V0_6__reservation_notification.sql`：建 `reservation`（waiting/notified/fulfilled/cancelled，idx_book_status）与 `notification`（idx_user_read，见下） |
| 新增 | `entity/Reservation` · `entity/Notification` · `mapper/ReservationMapper` · `mapper/NotificationMapper` · `ReservationService` · `ReservationController` · `NotificationController` · `schedule/OverdueScanJob` |
| 新增接口 | `POST /api/books/{id}/reserve`（登录用户；在借状态才可约；重复预约 40901；在馆书预约 40001） |
| 新增接口 | `GET /api/notifications`（登录用户，未读优先，分页） |
| 新增接口 | `GET /api/books/{id}/related`：共借 SQL（下）+ 冷启动退化「同分类最近入库 LIMIT 6」 |
| 修改 | `BorrowService.returnBook` 事务内追加：该书有 waiting 预约 → 队首置 `notified` + 生成 notification |
| 修改 | v0.5 详情聚合 `relatedIds` 从空占位切真实调用 |
| 定时 | `OverdueScanJob`：`@Scheduled(cron="0 0 9 * * *", zone="Asia/Shanghai")`，扫 due_at 前 3 天的 active 记录 → 生成提醒通知（同书同人当日去重） |
| 修改前端 | 旧 `index.html`：无改动 |
| 测试 | `ReservationTest`（waiting→notified→fulfilled 状态机 + 重复预约 40901 + 在馆书 40001）· `RelatedTest`（构造两人共借 A、B，断言 A 的 related 含 B；无数据返回同分类） |

```sql
CREATE TABLE reservation (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    book_id    INT NOT NULL,
    user_id    INT NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'waiting',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_book_status (book_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    content    VARCHAR(500) NOT NULL,
    is_read    TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

```sql
-- 共借推荐（MyBatis 注解内嵌）
SELECT r2.book_id, COUNT(*) AS cnt
FROM borrow_record r1
JOIN borrow_record r2 ON r1.user_id = r2.user_id AND r1.book_id != r2.book_id
WHERE r1.book_id = #{id}
GROUP BY r2.book_id
ORDER BY cnt DESC
LIMIT 6;
```

> 2026-11-06 更正：原文 b2 别名不存在，按语义修正。

**本版强制规范**：
- 预约通知在还书事务内生成，禁止「还书后再补通知」的异步方案。
- 共借无数据必须退化同分类，不允许返回空列表。

**本版禁止**：
- 禁止邮件/短信通道（只做站内通知）；禁止引入任何 MQ。

**验收**：
- [ ] 借走的书可预约、在馆书预约 40001、重复预约 40901
- [ ] 归还后队首 waiting → notified 且有 notification 记录
- [ ] 手工触发定时任务（或调短 cron 验证）：due_at 前 3 天记录生成提醒，且同书同人当日不重复
- [ ] A、B 共借后 A 的 related 含 B；无共借数据时返回同分类
- [ ] `./mvnw test` 全绿

---

#### v0.7 · 开放数据与 AI 接口（F-11 · 12）★ API 契约冻结点

**前置**：v0.6 验收通过。**目标**：ISBN 代理 + GLM 馆藏助手；**本版验收通过后 API 契约冻结（2.4）**。

**交付物**：

| 类 | 内容 |
|---|---|
| 新增 | `IsbnService`（RestClient；先 OpenLibrary `https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data`，失败再 Google Books `https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}`；超时 `ISBN_PROXY_TIMEOUT` 默认 5000ms；映射 `{title,author,publisher,cover_url,description}`；全部未命中 → 200 + `data:null` + message「未查到该书目，请手动录入」）· `IsbnController` |
| 新增接口 | `GET /api/isbn/{isbn}`（admin） |
| 新增 | `AssistantService`（RestClient 调智谱开放平台；`GLM_API_KEY`；模型默认 `glm-4.6`、`GLM_MODEL` 可覆盖；function calling 仅两工具：`search_books(keyword/category)`、`get_book_status(id)`；**非流式**）· `AssistantController` |
| 新增接口 | `POST /api/assistant/chat` `{message}` → `data: {reply: string}`（登录用户；未配置 key → 200 + `data:null` + message「AI 服务未配置」） |
| 契约 | 本版完成后在 `docs/api-contract.md` 输出**全量 API 契约清单**（方法/路径/权限/请求/响应示例），作为 B 阶段 mock 的唯一依据 |
| 修改前端 | 旧 `index.html`：无改动 |
| 测试 | `IsbnContractTest`（参数校验：非数字/超长 ISBN → 40001）· `AssistantConfigTest`（未配置 key 的错误路径） |

**本版强制规范**：
- 外部 API 只由后端代理调用；外部调用失败/超时不得抛 500，一律 200 + `data:null` + 明确 message。
- AI 回答必须基于工具返回的真实馆藏，工具查不到就答「馆内没有」，系统提示词中写死此约束。
- `docs/api-contract.md` 必须与代码一致（以代码为准生成，禁止手写漂移）。

**本版禁止**：
- 禁止换用豆瓣 / 其他书源（豆瓣 API 已关闭）；禁止换 OkHttp / WebClient；禁止流式 SSE（列为 v1.x 增强）；禁止把 GLM key 写进任何前端代码或日志。

**验收**：
- [ ] 输入真实 ISBN 回填书名/作者/出版社/封面；无效 ISBN → data:null + 友好 message，不 500
- [ ] 外网断开时同样返回 data:null + message，服务不崩
- [ ] 问「馆里有哪些科幻小说」列出真实馆藏；问不存在的书答「馆内没有」
- [ ] 未配置 GLM_API_KEY → 明确提示，不 500 堆栈
- [ ] `docs/api-contract.md` 覆盖全部 /api 端点
- [ ] `./mvnw test` 全绿
- [ ] **★ 契约冻结确认（用户或验收人签字后方可进入 v0.8）**

---

### 阶段 B · 前端界面（全部基于 mock，不连真实后端）

#### v0.8 · 前端工程与六页（mock）

**前置**：v0.7 验收通过且 API 契约已冻结。**目标**：book-web-ui 工程建立，六页 + 全局框架在 mock 数据下完整可用。

**交付物**：

| 类 | 内容 |
|---|---|
| 工程 | 全新 `book-web-ui/`（锁定栈：Vue 3 + TS strict + Vite + Tailwind v4 + shadcn-vue + Pinia + Vue Router + TanStack Vue Table）；`.env` 固定 `VITE_USE_MOCK=true` |
| mock 层 | `src/api/http.ts`（fetch 封装：Bearer、Result 解包、401 跳 /login——B 阶段对 mock 生效）+ `src/api/mock/`（按 `docs/api-contract.md` 同签名实现，200ms 延迟）+ `src/api/mock/data.ts`（数据集按 2.4 锁定：≥12 本书 / 3 用户 / ≥8 条记录含逾期 / 看板样例 / AI 问答样例） |
| 页面 | `/login`（表单，3D 背景位留空 v0.9）、`/`（Bento 统计条 + 搜索 + 分类筛选 + 表格⇄卡片双视图 + 分页）、`/books/:id`（两栏：封面元信息 / 简介与借阅历史 + 借/还/预约按钮）、`/records`（状态筛选 + 表格 + 应还日期）、`/dashboard`（Bento 布局三格，图表 v0.9）、`/me`（Bento 小格） |
| 全局 | 顶部玻璃导航（backdrop-blur）+ 面包屑 + 主题切换（深色默认）+ 路由守卫（mock token）+ 通知红点（mock） |
| 组件 | shadcn-vue：Button · Card · Input · Dialog · Toast · EmptyState；业务组件：BookCard · StatusTag · SkeletonRow · BentoStat · AiPanel（占位空组件） |
| 集成 | `npm run build` 产物拷入后端 `src/main/resources/static/mock-preview/`（**不覆盖旧 index.html**，仅作预览路径）；`vite.config.js` 预写 `/api` 代理（B 阶段不启用） |

```
book-web-ui/
├─ src/api/            http.ts · auth.ts · books.ts · records.ts · stats.ts ·
│                      dashboard.ts · assistant.ts · mock/(index.ts · data.ts)
├─ src/views/          LoginView · BookListView · BookDetailView ·
│                      RecordsView · DashboardView · MeView
├─ src/components/ui/  shadcn-vue 组件
├─ src/components/     BookCard · StatusTag · SkeletonRow · BentoStat · AiPanel
├─ src/router/index.ts 路由 + 守卫
├─ src/stores/         user.ts · theme.ts
├─ src/styles/         tokens.css（@theme 令牌）
└─ vite.config.js
```

**本版强制规范**：
- 所有数据只经 `src/api/*.ts`（内部走 mock），组件禁止裸 fetch。
- Tailwind 令牌走 v4 `@theme`，变量名按 2.2 表；圆角全站一档 `--radius`；阴影两档（rest / hover）。
- 状态标签 `StatusTag` 唯一产出（在馆/在借/逾期/预约中）；Bento 只用于统计条/看板/个人中心，数据表用紧凑表格。

**本版禁止**：
- 禁止连接真实后端（`VITE_USE_MOCK` 锁 true）；禁止引入 2.2 表之外依赖（ECharts / motion-v / TresJS / vite-plugin-pwa 属 v0.9，本版不装）；禁止动后端任何代码。

**验收**：
- [ ] `npm run build` 零报错（TS strict）；mock 预览路径可访问
- [ ] 六页路由齐全、切换不整页刷新；未登录访问 `/` 跳 `/login`（mock 守卫生效）
- [ ] 列表页：搜索/分类/双视图/分页在 mock 数据下全可用；统计条数字来自 mock stats
- [ ] 详情页：recentRecords / related（mock）/ 借还按钮状态流转正确
- [ ] 深色默认、亮色可切；玻璃导航与令牌生效
- [ ] 后端代码零改动（`git diff` 后端目录为空）

---

#### v0.9 · 界面完整化与亮点（mock）

**前置**：v0.8 验收通过。**目标**：动效、交互细节、响应式、看板图表、AI 侧栏、扫码 UI、PWA、3D 登录页（仍在 mock 下）。

**交付物**：

| 类 | 内容 |
|---|---|
| 依赖 | + echarts / vue-echarts（按需注册 Line/Pie/Bar）· motion-v · tresjs + three · vite-plugin-pwa |
| 图表 | `/dashboard`：趋势大格 LineChart + 分类占比 PieChart + 排行榜格（mock 数据） |
| 动效 | 5 项：卡片 stagger 入场（30–50ms/项，仅首次）；hover 边框提亮 + 阴影加深（位移 ≤2px）；路由过渡 fade+8px 150–200ms；看板 scroll reveal；借/还按钮 loading 与状态标签过渡。全部尊重 `prefers-reduced-motion` |
| 交互 | 骨架屏（SkeletonRow 替代文字 loading）；删除乐观更新 + 5s toast 撤销（替代 confirm）；批量多选 → 批量借阅/删除/导出 CSV（UTF-8 BOM）；键盘流 `/` 聚焦搜索、Esc 关弹窗、方向键选行回车借阅；EmptyState 全站空态 |
| AI 侧栏 | AiPanel 完整实现：全局悬浮按钮 + backdrop-blur 面板，mock 问答样例对 + 本地打字机效果，回答内嵌馆藏链接（mock 路由跳详情） |
| 扫码 UI | 列表页「扫码建档」入口：`'BarcodeDetector' in window` 检测，支持则摄像头扫码，否则 ISBN 手输框；查到后（mock ISBN 查询）预填 Dialog 表单允许修正提交 |
| 3D | `/login` TresJS 粒子场（桌面 800 / 移动 200 粒子，路由级 lazy chunk），鼠标视差 ±8px；`prefers-reduced-motion` → 静态渐变网格；移动端降粒子或关闭 |
| PWA | vite-plugin-pwa：manifest + autoUpdate；静态资源 precache，`GET /api/books` network-first 兜底 cache（真实验证留 v0.10） |
| 响应式 | `<768px`：表格转卡片布局、操作底部动作、触控目标 ≥44px |

**本版强制规范**：
- 动效参数以上表数值为准，不得自行加大幅度；ECharts 按需注册，禁止全量 import。
- 3D 资源只允许出现在 /login 的懒加载 chunk，主包不得含 three。
- 每屏一个焦点，禁止为填满屏幕堆信息。

**本版禁止**：
- 禁止 @vueuse/motion / GSAP；禁止渐变用于内容区（仅登录页与空态氛围层）；禁止 emoji 图标（全站 inline SVG，含搜索图标）；禁止连接真实后端；禁止动后端代码。

**验收**：
- [ ] 看板三图在 mock 数据下渲染正确；空态显示 EmptyState
- [ ] devtools 模拟 `prefers-reduced-motion` 后动效消失、登录页退静态背景
- [ ] 375px 宽度下六页可用，无横向滚动
- [ ] build 报告：主 chunk 不含 three；AI 侧栏打字机与链接跳转可用；扫码入口含手输降级
- [ ] 设计基准禁止项自查（渐变/emoji/发光/大圆角堆叠/信息过满）逐条通过
- [ ] `npm run build` 零报错；后端代码零改动

---

### 阶段 C · 相连与发布

#### v0.10 · 真实连接与切换（相连）

**前置**：v0.9 验收通过。**目标**：前端切真实 API，删旧端点与旧页面，端到端全链路验收。

**交付物**：

| 类 | 内容 |
|---|---|
| 切换 | `.env.production` / 构建配置：`VITE_USE_MOCK=false`；启用 vite `/api` 代理（dev）；`npm run build` 产物**替换**后端 `src/main/resources/static/`（SPA fallback：非 /api 路径回 index.html） |
| 删除 | 旧 `static/index.html`（v0.8 前的原文件）；全部旧无前缀端点及 `@Deprecated` 标记（`/books`、`/books/page`、`/books/{id}`、`/books/{id}/borrowed`、`/books/{id}/return`）；`static/mock-preview/` 预览目录 |
| 后端 | SPA fallback 配置（forward 未匹配的非 /api GET 到 index.html）；确认拦截器放行 SPA 静态资源 |
| E2E | 按「相连验收清单」（下）逐条跑通并记录 |

**相连验收清单（全链路，真实后端 + 真实前端）**：
- [ ] 注册 → 登录 → token → 列表加载（真实 `/api/books`）
- [ ] 搜索 / 分类 / 分页（服务端，跨页保持条件）
- [ ] 详情 → 借阅 → 记录页可见 active 记录与应还日期 → 归还 → 状态翻转
- [ ] 并发语义真机复现：两浏览器同借一书，后者收 40901 toast「该书刚被借走」
- [ ] 扫码/手输 ISBN → 真实 OpenLibrary/Google Books 回填 → 建档
- [ ] AI 助手真实 GLM：报馆藏正确；未配置 key 时明确提示
- [ ] 预约：在借书可约、重复约 40901；归还后通知红点出现
- [ ] 看板呈现真实借阅数据（与 SQL 抽查一致）
- [ ] 401 过期 → 跳 /login；错误码 → toast 映射正确
- [ ] PWA：可安装；断网刷新书单走缓存；联网恢复同步
- [ ] 3D 登录页真机可用；主 chunk 不含 three 复查
- [ ] 旧端点全部 404；旧 index.html 已不存在；`/` 直接进入新 SPA
- [ ] compose 一键部署验证：build → 打包 → `docker compose up -d --build` → healthcheck 路径可访问
- [ ] `./mvnw test` + `npm run build` 全绿

**本版强制规范**：
- 切换后 mock 层代码保留在仓库（`VITE_USE_MOCK` 开关与文档说明），供后续离线开发用，但生产构建必须 false。
- E2E 发现的 bug 按版本内修复，不得遗留到 v1.0。

**本版禁止**：
- 禁止为绕过集成问题修改 API 契约（发现契约问题 → 6.2 上报）。

---

#### v1.0 · 发布版

**前置**：v0.10 验收通过。**目标**：全量回归 + 文档 + 发版。

**交付物**：

| 类 | 内容 |
|---|---|
| 回归 | 第 4 章 v0.1–v0.10 所有验收清单全量复跑（逐版勾选） |
| 文档 | 重写 `README.md`（本地启动：环境变量表 + `mvnw spring-boot:run` + `npm run dev`；生产：前端 build → 后端打包 → compose）；更新 `部署指南.md`（前端构建步骤 + 全部环境变量 + mock 开关说明） |
| 运维 | `docker-compose.yml` 加 healthcheck：`curl -sf http://localhost:8080/api/stats || exit 1` |
| 发版 | git tag `v1.0` |

**验收**：
- [ ] v0.1–v0.10 全部验收项复跑通过
- [ ] README 从零起步可依样跑起（新环境模拟）
- [ ] compose healthcheck 生效；tag 已打

---

## 5. 发布标准汇总

v1.0 打 tag 前必须同时满足：

1. 第 4 章 11 个版本（v0.1–v0.10 + v1.0 自身）的验收清单 100% 勾选；
2. 三阶段顺序证据成立：git log 中后端版本（v0.1–v0.7）提交全部早于前端版本（v0.8–v0.9），相连版本（v0.10）最后；
3. 仓库无明文凭据（grep 复查）、无第 2 章之外的依赖（pom + package.json 对照）、生产构建 `VITE_USE_MOCK=false`；
4. `./mvnw test` 与 `npm run build` 全绿；
5. 6.2 遗留问题表无「未处理」项（已上报未决的需用户签字豁免）。

---

## 6. 附录

### 6.1 环境变量总表

| 变量 | 用途 | 引入版本 | 必填 |
|---|---|---|---|
| SPRING_DATASOURCE_URL / USERNAME / PASSWORD | 数据库连接 | v0.1 起强制显式 | 是 |
| JWT_SECRET | JWT 签名密钥 | v0.3 | 是 |
| ADMIN_INIT_PASSWORD | admin 初始密码（无 admin 时必填，否则启动失败） | v0.3 | 条件必填 |
| BORROW_DAYS | 借期天数，默认 30 | v0.4 | 否 |
| ISBN_PROXY_TIMEOUT | ISBN 代理超时 ms，默认 5000 | v0.7 | 否 |
| GLM_API_KEY | AI 助手（未配置时接口返回明确提示） | v0.7 | 用 AI 则必填 |
| GLM_MODEL | 覆盖默认模型 `glm-4.6` | v0.7 | 否 |
| VITE_USE_MOCK | 前端数据源开关：B 阶段 true / 生产必须 false | v0.8 | 构建时 |

### 6.2 遗留问题与阻塞记录（编码过程中追加，初始为空）

> 格式：日期 | 版本 | 现象 | 已尝试 | 需要用户决策什么 | 状态

| 日期 | 版本 | 现象 | 已尝试 | 待决策 | 状态 |
|---|---|---|---|---|---|
| — | — | — | — | — | — |

---

*本交付单 v3.0 为唯一需求来源，替代 v2.0；三阶段顺序与 API 契约冻结以 0.1 / 2.4 为准，所有方案以第 2 章锁定表为准，偏离即阻塞上报。*
