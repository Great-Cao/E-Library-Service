[English](README.md) | 简体中文

# E-Library Service（电子图书馆服务）

一个小型数字图书馆 REST 服务：浏览书籍、查询书籍详情、借阅、归还，以及查看当前未归还的借阅记录。

本仓库是后端技术作业的完整实现，包含源代码、设计文档（`docs/design.md`）与本说明文档。

## 功能范围

题目要求的五项功能均已端到端实现：

| 功能 | 接口 |
| --- | --- |
| 浏览书籍（分页） | `GET /api/v1/books` |
| 查询书籍详情 | `GET /api/v1/books/{bookId}` |
| 借阅书籍 | `POST /api/v1/books/{bookId}/loans` |
| 归还书籍 | `POST /api/v1/loans/{loanId}/return` |
| 查看当前借阅 | `GET /api/v1/users/me/loans` |

明确不在本期范围：注册/登录/OAuth、角色权限、借阅期限与罚款、预约、电子书文件存储、生产级前端工程，以及书籍/用户的管理接口。仓库里另有一个用于演示的简单 Vue 页面（见下文"演示前端"），属于额外交付。书籍与用户由演示数据预置，因此 `mvn spring-boot:run` 之后无需任何准备即可直接调用。

## 技术选型

| 领域 | 选型 |
| --- | --- |
| 语言 | Java 17 |
| 应用框架 | Spring Boot 3.3.5（Web / MVC） |
| 构建 | Maven |
| 持久化 | Spring Data JPA / Hibernate 6.5，配合 `hibernate-community-dialects`（SQLite 方言） |
| 数据库 | SQLite（`sqlite-jdbc` 3.46） |
| 数据库迁移 | Flyway 10（结构版本化 + 可重复执行的种子迁移） |
| 参数校验 | Jakarta Bean Validation |
| API 文档 | Springdoc OpenAPI 2.6（Swagger UI） |
| 测试 | JUnit 5、Mockito、Spring Boot Test、MockMvc |
| 演示前端（可选） | Vue 3 + Vite + Vue Router |

需要 **JDK 17+** 与 **Maven 3.9+**，不需要额外安装数据库或容器。可选的演示前端另需
Node 18+（建议 Node 20 LTS）。

## 快速开始

```bash
mvn spring-boot:run
```

服务监听 `http://localhost:8080`。首次启动时 Flyway 会创建 `data/library.db`、建立表结构并写入演示数据。交互式接口文档地址：

- Swagger UI：http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON：http://localhost:8080/v3/api-docs

也可以打包后运行：

```bash
mvn clean package
java -jar target/e-library-service-0.1.0.jar
```

## 演示前端（可选）

`frontend/` 下有一个简单的 Vue 单页应用，用于点击演示五项功能。它属于**额外交付**：
原题只要求后端，没有它后端依然完整可用。

```bash
# 终端 1 —— 后端
mvn spring-boot:run

# 终端 2 —— 前端
cd frontend
npm install
npm run dev
```

然后打开 http://localhost:5173 。Vite 开发服务器会把 `/api` 代理到
`http://localhost:8080`，因此浏览器请求保持同源，后端无需任何 CORS 配置。用顶部栏的
下拉框可以切换成任一演示用户。

界面覆盖：书籍列表（分页）、书籍详情、借阅、我的借阅、归还，以及统一的提示横幅——
它直接展示后端返回的 `error.code` / `error.message`。零库存的书籍 6 在详情页直接禁用
借阅按钮并显示"暂无库存"；若库存在页面打开后被借空，再点借阅会收到 409 并由横幅提示。

**可选的单包交付。** 如果希望由 Spring Boot 直接托管页面、不启开发服务器：

```bash
cd frontend
npm install
npm run build:single      # 构建产物输出到 ../src/main/resources/static
cd ..
mvn spring-boot:run       # 此时 http://localhost:8080/ 也能打开页面
```

构建产物已被 git 忽略，也不参与 Maven 构建，因此在没有安装 Node 的机器上
`mvn clean package` 依然可用。

## 身份模拟（`X-User-Id`）

本项目不做认证。当前用户取自 **`X-User-Id`** 请求头，其值必须是引用某个已存在用户的正整数。需要用户的接口遇到缺失、非数字或非正数时返回 `400 VALIDATION_ERROR`，用户不存在时返回 `404 USER_NOT_FOUND`。

这样可以把作业的重点放在领域建模与 API 设计上。真实系统应从已认证的上下文中取得用户身份，而服务层的所有权校验逻辑可以原样保留。

## 演示数据

由 `R__seed_demo_data.sql` 写入（可重复迁移、幂等——重跑不会产生重复行，也不会覆盖已有数据）。

用户：

| id | 姓名 |
| --- | --- |
| 1 | Alice Chen |
| 2 | Brian Lee |

书籍：

| id | 书名 | 作者 | 总量 | 可借 |
| --- | --- | --- | --- | --- |
| 1 | Clean Code | Robert C. Martin | 3 | 3 |
| 2 | The Pragmatic Programmer | Andrew Hunt, David Thomas | 2 | 2 |
| 3 | Designing Data-Intensive Applications | Martin Kleppmann | 4 | 4 |
| 4 | Refactoring | Martin Fowler | 1 | 1 |
| 5 | Domain-Driven Design | Eric Evans | 2 | 2 |
| 6 | Working Effectively with Legacy Code | Michael C. Feathers | 1 | 0 |

书籍 6 初始就是零库存，便于在不做任何借阅的情况下直接复现 `409 BOOK_UNAVAILABLE`。

## 接口示例

所有响应均为 JSON，基础路径为 `/api/v1`。

### 1. 浏览书籍

```bash
curl "http://localhost:8080/api/v1/books?page=0&size=3"
```

```json
{
  "items": [
    {
      "id": 1,
      "title": "Clean Code",
      "author": "Robert C. Martin",
      "isbn": "9780132350884",
      "totalCopies": 3,
      "availableCopies": 3
    }
  ],
  "page": 0,
  "size": 3,
  "totalElements": 6,
  "totalPages": 2
}
```

`page` 从 `0` 开始（默认 `0`）；`size` 默认 `20`，取值必须在 `1` 到 `100` 之间。结果按 `id` 升序返回，保证分页稳定。

### 2. 查询书籍详情

```bash
curl "http://localhost:8080/api/v1/books/1"
```

```json
{
  "id": 1,
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "9780132350884",
  "description": "A handbook of agile software craftsmanship.",
  "totalCopies": 3,
  "availableCopies": 3,
  "createdAt": "2026-01-05T08:05:00Z"
}
```

id 不存在时返回 `404 BOOK_NOT_FOUND`。

### 3. 借阅书籍

```bash
curl -X POST "http://localhost:8080/api/v1/books/1/loans" -H "X-User-Id: 1"
```

```json
{
  "id": 1,
  "bookId": 1,
  "userId": 1,
  "borrowedAt": "2026-10-03T11:05:28.958Z",
  "returnedAt": null
}
```

返回 `201 Created`，并将 `availableCopies` 减一。同一用户可以同时持有一本书的多册。

### 4. 归还书籍

```bash
curl -X POST "http://localhost:8080/api/v1/loans/1/return" -H "X-User-Id: 1"
```

```json
{
  "id": 1,
  "bookId": 1,
  "userId": 1,
  "borrowedAt": "2026-10-03T11:05:28.958Z",
  "returnedAt": "2026-10-03T11:05:29.354Z"
}
```

恢复一册库存。归还后的记录会保留用于历史查询，只是不再属于"当前借阅"。只有借阅记录的所有者本人才能归还。

### 5. 查看当前借阅

```bash
curl "http://localhost:8080/api/v1/users/me/loans" -H "X-User-Id: 1"
```

```json
{
  "items": [
    {
      "id": 1,
      "bookId": 1,
      "bookTitle": "Clean Code",
      "bookAuthor": "Robert C. Martin",
      "borrowedAt": "2026-10-03T11:05:28.958Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`status` 默认为 `active`，且目前只支持该值。每条未归还记录单独返回（包含同一本书的多册）。身份始终来自请求头，不能通过查询参数切换用户。

## 错误模型

所有错误使用同一套响应结构，便于客户端统一处理：

```json
{
  "error": {
    "code": "BOOK_UNAVAILABLE",
    "message": "Book 6 has no copies available."
  }
}
```

当问题出在具体参数或请求头时，会额外返回 `details`：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The X-User-Id header must be provided.",
    "details": [
      { "field": "X-User-Id", "reason": "must be provided" }
    ]
  }
}
```

| 错误码 | HTTP | 含义 |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | 参数或请求头无效 |
| `FORBIDDEN` | 403 | 借阅记录属于其他用户 |
| `BOOK_NOT_FOUND` | 404 | 图书不存在 |
| `USER_NOT_FOUND` | 404 | 用户不存在 |
| `LOAN_NOT_FOUND` | 404 | 借阅记录不存在 |
| `NOT_FOUND` | 404 | 路由或资源不存在 |
| `METHOD_NOT_ALLOWED` | 405 | 该路由不支持此 HTTP 方法 |
| `BOOK_UNAVAILABLE` | 409 | 没有可借库存 |
| `LOAN_ALREADY_RETURNED` | 409 | 重复归还 |
| `SERVICE_UNAVAILABLE` | 503 | 数据库繁忙（如 `SQLITE_BUSY`），可稍后重试 |
| `INTERNAL_ERROR` | 500 | 未预期的服务端错误 |

不会向客户端返回堆栈、SQL 语句或文件路径。数据库繁忙会被映射为 `503`，绝不会误报为 `409 BOOK_UNAVAILABLE`，从而让客户端能区分"稍后重试"与"确实没有库存"。

## 构建与测试

```bash
mvn clean test        # 单元测试 + 集成测试（使用独立的 SQLite 文件）
mvn clean package     # 运行测试并构建可执行 jar
```

测试分两层：用 Mockito 覆盖服务层业务规则，用 MockMvc 针对真实 SQLite 数据库验证 HTTP 契约。覆盖内容包括：

- 浏览、详情与分页参数校验；
- 借阅/归还的正常路径以及全部约定错误码（客户端可触发的走 HTTP 端到端，503 / 500 在处理器层断言）；
- 借阅与归还后的库存不变量（`0 <= availableCopies <= totalCopies`）；
- 重复归还不重复恢复库存；
- SQLite 真实生效的 `NOT NULL`、`UNIQUE(isbn)`、`CHECK` 与外键约束；
- 确定性并发测试：8 个线程争抢 2 册，恰好 2 个成功；
- OpenAPI 文档与 Swagger UI 可访问。

测试不会触碰 `data/library.db`，而是在 `target/` 下为每个 JVM 使用独立的库文件（`e-library-it-<pid>.db`），并在每个测试前依据迁移重建。

## 项目结构

```text
src/main/java/com/example/elibrary/
├── ELibraryApplication.java
├── config/          # Clock、身份解析器、MVC 与 OpenAPI 配置
├── controller/      # HTTP 路由与状态码
├── domain/          # Book、User、Loan 实体及 Instant<->文本转换器
├── dto/response/    # 响应模型（不直接序列化持久化实体）
├── exception/       # 错误码、业务异常、全局异常处理
├── repository/      # Spring Data JPA 仓储，含条件更新
└── service/         # 事务边界与业务规则

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__create_schema.sql      # 表结构、约束、外键、索引
    └── R__seed_demo_data.sql      # 幂等演示数据

src/test/java/com/example/elibrary/
├── service/         # Mockito 单元测试
└── integration/     # MockMvc + 真实 SQLite 集成测试

frontend/            # 可选的 Vue 演示前端（见"演示前端"一节）

docs/design.md       # 设计文档与评审记录
```

## 设计决策与取舍

原题明确说"更重视思考过程与设计取舍"，所以这些推理写在明面上，而不是留在脑子里。先看简版：

| 决策 | 关键理由 | 主要代价 |
| --- | --- | --- |
| 分层单体 | 领域小，优先把职责边界做清楚 | 无法独立扩展 |
| SQLite + 单连接池 | 评审方零配置即可运行 | 读也串行，不适合高写入并发 |
| 冗余 `availableCopies` + 条件更新 | 列表要显示可借数，且绝不能超借 | 数据冗余，靠 `CHECK` 约束与并发测试兜底 |
| `returnedAt` 表示状态而非状态字段 | 从根本上消除状态与时间不一致 | 无法表达续借、丢失等中间态 |
| 借阅刻意不做幂等 | 持有多册是合法语义，重试即"再借一册" | 客户端重试会多借；需要 `Idempotency-Key` |
| 身份用 `X-User-Id` 请求头 | 无认证要求，但需演示多用户 | 不是安全机制 |
| Flyway 独占 schema（`ddl-auto=none`） | SQLite 类型宽松，Hibernate 校验不可靠 | 失去启动期校验，改由集成测试覆盖 |
| 前端默认走 Vite 代理 | 同源，后端零改动、无需 CORS | 分离部署仍需 CORS |

完整的备选方案、每条决策的代价以及"什么情况下我会改"见
[docs/design.md](docs/design.md) 第 16 节。主动排除的功能：列表搜索/筛选、书籍与用户
管理接口、认证。

## 后续可扩展方向

- 为借阅请求引入 `Idempotency-Key`。
- 为 `GET /api/v1/books` 增加关键字与可借状态筛选。
- 使用 RFC 8288 的分页链接替代裸页码。
- 以真实认证替换 `X-User-Id`。
- 当写入并发成为瓶颈时，从 SQLite 迁移到服务型数据库。
