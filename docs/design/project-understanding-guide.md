# OfficeFlow 项目完整理解文档

本文档用于帮助组内成员快速理解当前 OfficeFlow 项目的整体结构、启动方式、技术栈、模块职责、请求链路、数据库设计和后续开发注意事项。

当前项目已经从最初的基础 OA 框架扩展为一个包含用户权限、考勤、审批、公告、报表、AI 问答的前后端分离微服务项目。

## 1. 项目一句话说明

OfficeFlow 是一个企业 OA 办公系统，前端使用 Vue 3，后端使用 Spring Boot 微服务，网关统一转发请求，MySQL 存储业务数据，Redis 和 Nacos 作为基础中间件，AI 模块额外使用 PostgreSQL + pgvector 做知识库向量检索。

核心目标不是做一个特别复杂的生产级 OA，而是完整体现课程要求里的这些能力：

- 前后端分离
- Spring Boot 微服务拆分
- 登录认证和 JWT 鉴权
- 用户、部门、岗位、角色、菜单、接口权限管理
- 考勤打卡和记录查询
- 请假、加班审批
- 公告发布和已读未读
- 统一返回、统一异常
- Nacos 服务注册
- Docker 一键启动中间件
- AI 智能问答扩展能力

## 2. 当前目录结构

```text
OfficeFlow/
  backend/                 后端 Maven 多模块工程
    oa-common/             公共代码模块
    oa-gateway/            API 网关
    user-service/          用户、组织、权限服务
    attendance-service/    考勤服务
    flow-service/          审批服务
    notice-service/        公告服务
    report-service/        报表服务
    ai-service/            AI 问答服务

  frontend/                Vue 3 前端工程
    src/api/               前端请求封装
    src/layout/            后台布局
    src/router/            路由配置
    src/stores/            Pinia 状态
    src/views/             页面

  docs/
    sql/init.sql           MySQL 初始化脚本
    pdf/                   AI 知识库 PDF 文件
    api/                   接口文档
    design/                设计和交接文档
    ppt/                   答辩资料目录

  docker-compose.yml       MySQL、Redis、Nacos、pgvector
  start-all.cmd            Windows 一键启动入口
  start-all.ps1            一键启动主脚本
  stop-all.cmd             Windows 一键停止入口
  stop-all.ps1             一键停止主脚本
  README.md                项目启动说明
```

## 3. 技术栈

后端：

- Java 21
- Spring Boot 3.2.4
- Spring Cloud 2023.0.1
- Spring Cloud Alibaba 2023.0.1.0
- Spring Cloud Gateway
- Nacos Discovery / Config
- MyBatis
- MySQL 8.0
- Redis 7.2
- JWT
- Lombok
- Spring AI Alibaba
- PostgreSQL + pgvector
- PDFBox

前端：

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
- ECharts

工程和环境：

- Maven 多模块
- pnpm
- Docker Compose
- VS Code / IDEA 均可开发

## 4. 本地启动方式

项目根目录已经有一键启动脚本。

第一次启动推荐：

```powershell
.\start-all.cmd
```

这个脚本会做四件事：

1. 执行 `docker compose up -d`，启动 MySQL、Redis、Nacos、pgvector。
2. 进入 `backend` 执行 `mvn clean install -DskipTests`。
3. 分别打开 PowerShell 窗口启动每个后端服务。
4. 进入 `frontend` 执行 `pnpm dev`。

启动完成后访问：

```text
http://127.0.0.1:5173/
```

停止前后端服务：

```powershell
.\stop-all.cmd
```

连 Docker 中间件一起停止：

```powershell
.\stop-all.cmd -WithDocker
```

如果只是快速重启服务，不想重新编译：

```powershell
.\start-all.cmd -SkipBuild
```

如果 Docker 已经启动，不想重复执行 `docker compose up -d`：

```powershell
.\start-all.cmd -NoDocker
```

## 5. 中间件和端口

Docker 中间件：

| 服务     | 容器名                  | 宿主机端口          | 说明                |
| -------- | ----------------------- | ------------------- | ------------------- |
| MySQL    | `officeflow-mysql`    | `3307`            | OA 业务数据库       |
| Redis    | `officeflow-redis`    | `6379`            | 缓存、锁、后续扩展  |
| Nacos    | `officeflow-nacos`    | `8848` / `9848` | 服务注册、配置中心  |
| pgvector | `officeflow-pgvector` | `5433`            | AI 知识库向量数据库 |

后端服务端口：

| 服务                   | 端口     | 作用                                           |
| ---------------------- | -------- | ---------------------------------------------- |
| `oa-gateway`         | `9000` | 统一入口、跨域、JWT 校验、路由转发             |
| `user-service`       | `9101` | 登录、用户、部门、岗位、角色、菜单、接口权限   |
| `attendance-service` | `9102` | 上班打卡、下班打卡、考勤规则、考勤组、记录查询 |
| `flow-service`       | `9103` | 请假、加班、审批、待办、已办                   |
| `notice-service`     | `9104` | 公告发布、公告查询、已读未读                   |
| `report-service`     | `9105` | 报表和大屏接口，目前较轻                       |
| `ai-service`         | `9106` | RAG 智能问答、PDF 知识库、向量检索             |
| `frontend`           | `5173` | Vue 前端开发服务器                             |

前端原则上只访问网关：

```text
http://localhost:9000
```

不要让前端直接访问 `9101`、`9102` 这些服务端口。这样才符合“统一网关入口”的架构。

## 6. 后端整体架构

后端是 Maven 父子模块结构。

父工程：

```text
backend/pom.xml
```

它只负责统一版本、依赖管理和模块聚合，本身不启动。

子模块：

```text
oa-common
oa-gateway
user-service
attendance-service
flow-service
notice-service
report-service
ai-service
```

每个可启动服务都有自己的：

```text
src/main/java/...Application.java
src/main/resources/application.yml
```

启动某个单服务的方式：

```powershell
cd backend
mvn -pl user-service spring-boot:run
```

`oa-common` 不是服务，没有端口，不能单独启动。它只给其他服务提供公共类。

## 7. 请求从前端到后端的完整链路

以“员工查询当前登录用户信息”为例：

1. 前端页面调用 `frontend/src/api/user.js`。
2. Axios 实例位于 `frontend/src/api/request.js`。
3. 请求拦截器从 Pinia 的 `userStore` 中取 token。
4. 请求头添加：

```text
Authorization: Bearer <token>
```

5. 前端请求 `/api/user/profile`。
6. Vite 开发代理或部署环境把请求发到网关 `oa-gateway:9000`。
7. 网关 `JwtAuthenticationFilter` 判断是否白名单。
8. 非白名单接口必须携带 JWT。
9. 网关解析 JWT，并向下游服务转发时追加用户信息请求头：

```text
X-Login-User-Id
X-Login-Username
X-Login-Roles
X-Login-Dept-Id
```

10. 网关根据路径转发到对应微服务。
11. `user-service` Controller 接收请求，调用 Service。
12. Service 调用 Mapper。
13. Mapper 查 MySQL。
14. Controller 返回 `ApiResponse<T>`。
15. 前端响应拦截器判断 `code` 是否为 `200`。

这是整个项目最重要的主链路。

## 8. 网关职责

网关模块：

```text
backend/oa-gateway
```

主要职责：

- 统一入口端口 `9000`
- 跨域处理
- JWT Token 校验
- 根据路径转发到不同服务
- 把 JWT 中的用户信息转成请求头传给服务

当前路由规则：

| 请求路径               | 下游服务               |
| ---------------------- | ---------------------- |
| `/api/user/**`       | `user-service`       |
| `/api/attendance/**` | `attendance-service` |
| `/api/flow/**`       | `flow-service`       |
| `/api/notice/**`     | `notice-service`     |
| `/api/report/**`     | `report-service`     |
| `/api/ai/**`         | `ai-service`         |

当前白名单包括：

- 登录接口
- 各服务 health 接口
- `/api/ai/` 开头接口
- `/actuator`

注意：目前 `/api/ai/` 被放进了白名单，所以 AI 问答接口不需要登录也能访问。这对演示方便，但如果后续要更严格，可以改为需要登录。

## 9. 公共模块 oa-common

模块位置：

```text
backend/oa-common
```

核心类：

| 类                         | 作用                        |
| -------------------------- | --------------------------- |
| `ApiResponse<T>`         | 统一返回结构                |
| `ResultCode`             | 统一状态码                  |
| `BusinessException`      | 业务异常                    |
| `GlobalExceptionHandler` | 全局异常处理                |
| `JwtUtil`                | JWT 生成和解析              |
| `SecurityConstants`      | JWT Claim 字段常量          |
| `CommonConstants`        | 通用 Header、Token 前缀常量 |

统一返回格式大致是：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

当前大多数基础业务服务都使用 `ApiResponse<T>`，但 `ai-service` 里部分 Controller 直接返回 `Map<String, Object>`。这不影响当前前端调用，但如果老师严格检查“统一返回”，后续建议统一改成 `ApiResponse.ok(...)`。

## 10. user-service 用户组织权限模块

模块位置：

```text
backend/user-service
```

负责范围：

- 用户登录
- JWT 签发
- 退出登录
- 当前用户信息
- 当前用户菜单
- 员工 CRUD
- 部门 CRUD
- 岗位 CRUD
- 角色 CRUD
- 角色分配
- 菜单权限
- 接口权限
- 登录日志和操作日志查询

主要接口：

| 功能         | 接口                                  |
| ------------ | ------------------------------------- |
| 登录         | `POST /api/user/login`              |
| 退出         | `POST /api/user/logout`             |
| 当前用户     | `GET /api/user/profile`             |
| 当前菜单     | `GET /api/user/menus/current`       |
| 员工分页     | `GET /api/user/users`               |
| 员工选项     | `GET /api/user/users/options`       |
| 新增员工     | `POST /api/user/users`              |
| 修改员工     | `PUT /api/user/users/{id}`          |
| 删除员工     | `DELETE /api/user/users/{id}`       |
| 启停员工     | `PUT /api/user/users/{id}/status`   |
| 重置密码     | `PUT /api/user/users/{id}/password` |
| 分配角色     | `PUT /api/user/users/{id}/roles`    |
| 部门树       | `GET /api/user/depts/tree`          |
| 岗位列表     | `GET /api/user/posts`               |
| 角色列表     | `GET /api/user/roles`               |
| 菜单列表     | `GET /api/user/menus`               |
| 接口权限列表 | `GET /api/user/api-permissions`     |

数据库对应表：

| 表                          | 作用             |
| --------------------------- | ---------------- |
| `sys_user`                | 员工账号         |
| `sys_dept`                | 部门             |
| `sys_post`                | 岗位             |
| `sys_role`                | 角色             |
| `sys_menu`                | 菜单和按钮权限   |
| `sys_api_permission`      | 接口权限         |
| `sys_user_role`           | 用户角色关系     |
| `sys_role_menu`           | 角色菜单关系     |
| `sys_role_api_permission` | 角色接口权限关系 |
| `sys_login_log`           | 登录日志         |
| `sys_operation_log`       | 操作日志         |
| `sys_config`              | 系统配置参数     |

默认测试账号都在 `docs/sql/init.sql` 中，密码目前是明文 `123456`，适合课程演示，但不是生产写法。

## 11. attendance-service 考勤模块

模块位置：

```text
backend/attendance-service
```

负责范围：

- 上班打卡
- 下班打卡
- 今日打卡状态
- 个人打卡记录查询
- 部门今日考勤概览
- 考勤规则管理
- 考勤组管理

主要接口：

| 功能         | 接口                                |
| ------------ | ----------------------------------- |
| 健康检查     | `GET /api/attendance/health`      |
| 上班打卡     | `POST /api/attendance/check-in`   |
| 下班打卡     | `POST /api/attendance/check-out`  |
| 今日状态     | `GET /api/attendance/today`       |
| 我的记录     | `GET /api/attendance/my-records`  |
| 部门今日概览 | `GET /api/attendance/dept-today`  |
| 考勤规则列表 | `GET /api/attendance/rules`       |
| 新增考勤规则 | `POST /api/attendance/rules`      |
| 修改考勤规则 | `PUT /api/attendance/rules/{id}`  |
| 考勤组列表   | `GET /api/attendance/groups`      |
| 新增考勤组   | `POST /api/attendance/groups`     |
| 修改考勤组   | `PUT /api/attendance/groups/{id}` |

数据库对应表：

| 表                              | 作用                                     |
| ------------------------------- | ---------------------------------------- |
| `attendance_rule`             | 考勤规则，如上班时间、下班时间、迟到阈值 |
| `attendance_group`            | 考勤组，绑定部门和规则                   |
| `attendance_record`           | 每日打卡记录                             |
| `attendance_correction_apply` | 补卡申请预留                             |
| `attendance_monthly_report`   | 月度考勤报表预留                         |

当前实现重点：

- 根据当前登录用户 ID 打卡。
- 每个用户每天只有一条考勤记录。
- 上班打卡时计算迟到分钟和状态。
- 下班打卡时计算工作分钟和早退分钟。
- 规则优先从数据库中读取，查不到则使用默认规则。

## 12. flow-service 审批模块

模块位置：

```text
backend/flow-service
```

负责范围：

- 创建请假申请
- 创建加班申请
- 查询我的申请
- 查询申请详情
- 修改、删除、取消申请
- 查询待办审批
- 查询已办审批
- 同意审批
- 驳回审批

主要接口：

| 功能       | 接口                                          |
| ---------- | --------------------------------------------- |
| 健康检查   | `GET /api/flow/health`                      |
| 创建申请   | `POST /api/flow/applies`                    |
| 我的申请   | `GET /api/flow/applies/my`                  |
| 申请详情   | `GET /api/flow/applies/{id}`                |
| 修改申请   | `PUT /api/flow/applies/{id}`                |
| 删除申请   | `DELETE /api/flow/applies/{id}`             |
| 取消申请   | `PUT /api/flow/applies/{id}/cancel`         |
| 待办审批   | `GET /api/flow/admin/applies/pending`       |
| 已处理审批 | `GET /api/flow/admin/applies/processed`     |
| 已通过列表 | `GET /api/flow/admin/applies/approved`      |
| 同意审批   | `POST /api/flow/admin/applies/{id}/approve` |
| 驳回审批   | `POST /api/flow/admin/applies/{id}/reject`  |

数据库对应表：

| 表                      | 作用     |
| ----------------------- | -------- |
| `flow_apply`          | 审批主表 |
| `flow_approve_record` | 审批记录 |
| `flow_cc`             | 抄送人   |
| `flow_attachment`     | 附件     |

当前审批模型是一级审批。也就是说：

- 员工提交申请。
- 直属领导或指定审批人审批。
- 审批结果为同意或驳回。
- 没有多级流程引擎。

这符合课程“简易审批模块”的要求。

## 13. notice-service 公告通知模块

模块位置：

```text
backend/notice-service
```

负责范围：

- 管理员发布公告
- 公告上下线
- 公告删除
- 员工查看公告
- 标记已读
- 批量已读
- 未读数量
- 公告阅读详情

主要接口：

| 功能           | 接口                                                |
| -------------- | --------------------------------------------------- |
| 健康检查       | `GET /api/notice/health`                          |
| 员工公告列表   | `GET /api/notice/notices`                         |
| 员工公告详情   | `GET /api/notice/notices/{id}`                    |
| 公告预览       | `GET /api/notice/notices/{id}/preview`            |
| 标记已读       | `POST /api/notice/notices/{id}/read-status`       |
| 批量已读       | `POST /api/notice/notices/batch-read`             |
| 未读数量       | `GET /api/notice/notices/unread-count`            |
| 管理端新增公告 | `POST /api/notice/admin/notices`                  |
| 管理端修改公告 | `PUT /api/notice/admin/notices/{id}`              |
| 发布公告       | `POST /api/notice/admin/notices/{id}/publish`     |
| 下线公告       | `POST /api/notice/admin/notices/{id}/offline`     |
| 删除公告       | `DELETE /api/notice/admin/notices/{id}`           |
| 管理端公告列表 | `GET /api/notice/admin/notices`                   |
| 阅读详情       | `GET /api/notice/admin/notices/{id}/read-details` |

数据库对应表：

| 表               | 作用         |
| ---------------- | ------------ |
| `notice`       | 公告主表     |
| `notice_scope` | 公告可见范围 |
| `notice_read`  | 公告阅读状态 |

当前公告表有全文索引，支持标题和内容搜索。

## 14. report-service 报表模块

模块位置：

```text
backend/report-service
```

当前模块较轻，主要提供 health 接口和后续报表扩展位置。

数据库对应表：

| 表                            | 作用         |
| ----------------------------- | ------------ |
| `report_dashboard_snapshot` | 数据大屏快照 |

前端当前的 `DashboardView` 更多是展示型页面，后续如果要做真实统计，可以让 `report-service` 从用户、考勤、审批、公告表中聚合数据。

## 15. ai-service AI 智能问答模块

模块位置：

```text
backend/ai-service
```

负责范围：

- 接入通义千问 DashScope
- 提供普通 AI 对话测试
- 加载 `docs/pdf` 中的 PDF 文档
- 使用 PDFBox 提取文本
- 文本切块
- 调用 Embedding 模型生成向量
- 存入 PostgreSQL + pgvector
- 根据用户问题做向量检索
- 拼接上下文并调用大模型回答

主要接口：

| 功能             | 接口                                   |
| ---------------- | -------------------------------------- |
| AI 健康检查      | `GET /api/ai/health`                 |
| 普通对话测试     | `GET /api/ai/chat?question=...`      |
| RAG 问答         | `GET /api/ai/rag/query?question=...` |
| 知识库状态       | `GET /api/ai/rag/status`             |
| 上传文本到知识库 | `POST /api/ai/rag/upload`            |
| 清空知识库       | `DELETE /api/ai/rag/knowledge`       |

AI 模块数据库不是 MySQL，而是 PostgreSQL：

```text
jdbc:postgresql://localhost:5433/officeflow_ai
```

对应初始化脚本：

```text
backend/ai-service/src/main/resources/schema-pgvector.sql
```

表：

| 表                   | 作用                         |
| -------------------- | ---------------------------- |
| `vector_store`     | 存储文本片段和向量           |
| `loaded_documents` | 记录已加载 PDF，避免重复入库 |

当前 AI 模块有两个需要尽快调整的配置点：

1. `app.document.path` 现在写死了别人电脑上的绝对路径，建议改为：

```yaml
app:
  document:
    path: ${AI_DOCUMENT_PATH:../docs/pdf}
```

2. `spring.ai.dashscope.api-key` 现在直接写在仓库里，建议改为：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}
```

然后每个人本地配置环境变量：

```powershell
setx DASHSCOPE_API_KEY "自己的通义千问 Key"
```

这样别人拉代码时不需要修改源码，也不会把 Key 暴露到 Git。

## 16. 前端结构

前端入口：

```text
frontend/src/main.js
frontend/src/App.vue
```

路由：

```text
frontend/src/router/index.js
```

页面：

| 页面目录             | 说明                         |
| -------------------- | ---------------------------- |
| `views/login`      | 登录页                       |
| `views/dashboard`  | 数据大屏                     |
| `views/system`     | 用户、部门、岗位、角色、权限 |
| `views/attendance` | 考勤打卡                     |
| `views/flow`       | 审批中心                     |
| `views/notice`     | 公告通知                     |
| `views/ai`         | AI 智能问答                  |

请求封装：

```text
frontend/src/api/request.js
```

各模块 API：

| 文件                  | 后端模块               |
| --------------------- | ---------------------- |
| `api/user.js`       | `user-service`       |
| `api/attendance.js` | `attendance-service` |
| `api/flow.js`       | `flow-service`       |
| `api/notice.js`     | `notice-service`     |
| `api/dashboard.js`  | `report-service`     |
| `api/ai.js`         | `ai-service`         |

用户状态：

```text
frontend/src/stores/user.js
```

主要保存：

- token
- 当前用户信息
- 当前用户菜单
- 登录状态

## 17. 登录和权限怎么理解

登录流程：

1. 用户在前端输入账号密码。
2. 前端调用 `POST /api/user/login`。
3. 网关放行登录接口。
4. `user-service` 校验账号密码。
5. 登录成功后生成 JWT。
6. 前端保存 token。
7. 后续请求自动携带 `Authorization: Bearer <token>`。
8. 网关解析 token。
9. 下游服务通过请求头获得用户 ID、用户名、角色、部门。

菜单权限：

- 菜单数据在 `sys_menu`。
- 角色和菜单关系在 `sys_role_menu`。
- 用户和角色关系在 `sys_user_role`。
- 登录后前端调用 `/api/user/menus/current` 获取当前用户菜单。
- 前端根据菜单决定侧边栏显示哪些入口。

接口权限：

- 接口权限定义在 `sys_api_permission`。
- 角色和接口权限关系在 `sys_role_api_permission`。
- 目前不同服务的接口权限拦截实现程度不完全一致。
- 课程答辩时可以说明：网关负责 Token 鉴权，接口级权限由用户权限模块维护，部分业务服务通过拦截器做校验。

## 18. 数据库整体设计

MySQL 主库：

```text
officeflow
```

初始化脚本：

```text
docs/sql/init.sql
```

模块划分：

| 模块     | 表                                                                                                                                                                                                                        |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 系统权限 | `sys_user`, `sys_dept`, `sys_post`, `sys_role`, `sys_menu`, `sys_api_permission`, `sys_user_role`, `sys_role_menu`, `sys_role_api_permission`, `sys_config`, `sys_login_log`, `sys_operation_log` |
| 考勤     | `attendance_rule`, `attendance_group`, `attendance_record`, `attendance_correction_apply`, `attendance_monthly_report`                                                                                          |
| 审批     | `flow_apply`, `flow_approve_record`, `flow_cc`, `flow_attachment`                                                                                                                                                 |
| 公告     | `notice`, `notice_scope`, `notice_read`                                                                                                                                                                             |
| 报表     | `report_dashboard_snapshot`                                                                                                                                                                                             |

AI 模块单独使用 PostgreSQL：

| 模块      | 表                                     |
| --------- | -------------------------------------- |
| AI 知识库 | `vector_store`, `loaded_documents` |

为什么 AI 不放 MySQL：

- MySQL 不适合做向量相似度检索。
- pgvector 可以直接存向量并建立 HNSW 索引。
- RAG 问答需要根据用户问题找最相似的文档片段，所以使用 PostgreSQL + pgvector。

## 19. 微服务之间是否互相调用

当前项目的主要链路是：

```text
前端 -> 网关 -> 某个服务 -> 数据库
```

目前没有复杂的服务间 RPC 调用，也没有 Feign 调用。

例如：

- 考勤服务需要用户部门信息时，可以查共享 MySQL 表。
- 审批服务需要申请人、审批人信息时，也可以查共享 MySQL 表。
- 公告服务需要用户范围和已读状态时，同样查共享 MySQL 表。

这不是最标准的微服务拆分方式，但对课程项目很实用：

- 实现难度低。
- 并行开发简单。
- 不需要处理服务间调用失败、超时、熔断。
- 更容易在答辩时启动成功。

答辩时可以说：当前采用轻量微服务拆分，服务通过网关统一暴露，数据层按模块表隔离；服务间调用不是课程基础功能重点，后续可使用 OpenFeign 扩展。

## 20. Nacos 当前怎么用

当前各后端服务都引入了 Nacos Discovery / Config，并在 `application.yml` 中配置：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
```

当前主要用途：

- 服务注册到 Nacos。
- 网关可以通过 `lb://service-name` 找服务。

配置中心目前只是接入了依赖和基础配置，业务参数动态刷新还没有完全做成。

课程要求里提到“打卡锁过期时间、迟到阈值支持动态刷新”。当前项目的实际状态是：

- 考勤参数已经有数据库表 `attendance_rule` 和 `sys_config`。
- 部分参数现在从数据库或代码默认值读取。
- Nacos 动态配置可以作为后续增强项。

## 21. Redis 当前怎么理解

Redis 已经在 Docker 和部分服务依赖中配置。

当前可用于：

- 登录状态扩展
- Token 黑名单
- 分布式打卡锁
- 验证码
- 热点配置缓存

但当前基础功能里 Redis 使用程度不深。答辩时如果被问，可以说：

- Redis 已作为基础中间件接入。
- 打卡防重复目前主要依赖数据库唯一键和业务判断。
- 后续可以把 `attendance.check-lock-seconds` 用 Redis 分布式锁实现，避免高并发重复打卡。

## 22. 一次完整业务例子：登录后打卡

1. 用户打开前端登录页。
2. 输入 `employee / 123456`。
3. 前端调用 `/api/user/login`。
4. 后端返回 JWT。
5. 前端保存 token。
6. 用户进入“考勤打卡”页面。
7. 页面加载时调用 `/api/attendance/today`。
8. 网关校验 token。
9. 网关把用户 ID 写入请求头。
10. `attendance-service` 根据用户 ID 和当天日期查询 `attendance_record`。
11. 用户点击“上班打卡”。
12. 前端调用 `/api/attendance/check-in`。
13. 服务判断今天是否已打卡。
14. 服务读取考勤规则。
15. 服务计算迟到分钟和状态。
16. 写入 `attendance_record`。
17. 前端刷新今日状态和记录列表。

## 23. 一次完整业务例子：提交审批

1. 用户进入“审批中心”。
2. 填写请假或加班申请。
3. 前端调用 `POST /api/flow/applies`。
4. `flow-service` 创建 `flow_apply`。
5. 系统指定审批人，通常是直属领导。
6. 领导登录后进入待办。
7. 前端调用 `GET /api/flow/admin/applies/pending`。
8. 领导点击同意或驳回。
9. 服务写入 `flow_approve_record`。
10. 更新 `flow_apply.status`。

## 24. 一次完整业务例子：AI 问答

1. Docker 启动 pgvector。
2. `ai-service` 启动。
3. 启动时扫描 `docs/pdf`。
4. PDFBox 读取 PDF 文本。
5. 文本按 500 字左右切块。
6. 调用 DashScope Embedding 模型生成 1024 维向量。
7. 向量写入 `vector_store`。
8. 前端打开“AI 问答”。
9. 页面调用 `/api/ai/rag/status` 显示知识库条数。
10. 用户输入问题。
11. 前端调用 `/api/ai/rag/query`。
12. 服务把问题转成向量。
13. pgvector 按余弦距离找最相关的片段。
14. 服务把片段拼成 Prompt。
15. 调用通义千问生成回答。
16. 前端显示回答。

## 25. 当前已经验证过的构建状态

在当前代码状态下，已验证：

后端全模块编译：

```powershell
cd backend
mvn clean compile -DskipTests
```

结果：成功。

前端构建：

```powershell
cd frontend
pnpm build
```

结果：成功。

前端构建时会有 chunk 过大提示，这是 Vite/Rollup 的性能提示，不是构建失败。

## 26. 当前项目需要注意的问题

### 26.1 AI 模块配置需要改成可移植

当前：

```yaml
app:
  document:
    path: D:/Desktop/Project/os/OfficeFlow/docs/pdf
```

问题：这是某个同学电脑上的绝对路径，别人拉下来会找不到 PDF。

建议：

```yaml
app:
  document:
    path: ${AI_DOCUMENT_PATH:../docs/pdf}
```

### 26.2 DashScope API Key 不应该提交到 Git

当前 `ai-service` 的 `application.yml` 中写了真实 Key。

建议改为：

```yaml
api-key: ${DASHSCOPE_API_KEY:}
```

本地通过环境变量配置。

### 26.3 README 还需要补充 AI 服务说明

README 里应补充：

- `ai-service` 端口 `9106`
- Docker 新增 pgvector
- AI 需要 `DASHSCOPE_API_KEY`
- PDF 知识库目录是 `docs/pdf`

### 26.4 AI 接口返回格式还没统一

基础业务接口大多返回 `ApiResponse<T>`，AI Controller 目前直接返回 `Map`。如果要严格统一，后续应改成 `ApiResponse.ok(result)`。

### 26.5 服务间调用暂时没有做

这是刻意简化，不是当前启动失败问题。课程项目可以先保证基础功能完整。

## 27. 新成员开发时应该先看什么

后端成员：

1. `README.md`
2. `docs/design/project-understanding-guide.md`
3. `docs/sql/init.sql`
4. 自己负责服务的 Controller
5. 自己负责服务的 Service
6. 自己负责服务的 Mapper
7. 前端对应 `src/api/*.js`

前端成员：

1. `frontend/src/router/index.js`
2. `frontend/src/layout/AppLayout.vue`
3. `frontend/src/api/request.js`
4. 自己负责模块的页面
5. 对应后端接口文件

答辩/PPT 成员：

1. 本文档
2. `README.md`
3. `docs/sql/init.sql`
4. `docs/api/*.md`
5. 项目运行截图

## 28. 答辩时可以怎么讲

可以按这个顺序讲：

1. 我们做的是企业 OA 办公系统。
2. 系统采用前后端分离和微服务架构。
3. 前端 Vue 3，后端 Spring Boot，多模块 Maven 管理。
4. 所有请求先进入 Gateway。
5. Gateway 负责跨域、JWT 校验和路由转发。
6. 用户权限模块实现登录、组织、RBAC。
7. 考勤模块实现上下班打卡和记录查询。
8. 审批模块实现请假、加班和一级审批。
9. 公告模块实现发布、查看和已读未读。
10. AI 模块基于 PDF 知识库实现 RAG 问答。
11. 数据库按业务模块拆表。
12. 中间件通过 Docker Compose 一键启动。
13. Nacos 用于服务注册，后续可扩展动态配置。

## 29. 常见问题

### 29.1 Maven 找不到 `oa-common`

先在 `backend` 下执行：

```powershell
mvn clean install -DskipTests
```

因为其他服务依赖本地的 `oa-common`。

### 29.2 Lombok getter/setter 找不到

检查 Maven 使用的 Java 版本：

```powershell
mvn -version
```

必须是 Java 21。不要用 Java 24，否则 Lombok 可能编译失败。

### 29.3 MySQL 没有初始化表

如果数据库卷以前已经存在，Docker 不会重复执行 `init.sql`。

重建数据库：

```powershell
docker compose down -v
docker compose up -d
```

注意：这个操作会删除本地测试数据。

### 29.4 AI 知识库条数为 0

优先检查：

1. `docs/pdf` 里是否有 PDF。
2. `app.document.path` 是否指向正确路径。
3. `DASHSCOPE_API_KEY` 是否配置。
4. `pgvector` 容器是否启动。
5. `ai-service` 启动日志是否有 PDF 加载报错。

### 29.5 前端请求 401

说明 token 没带、token 过期或网关没解析成功。

处理方式：

1. 重新登录。
2. 检查浏览器 localStorage。
3. 检查 `frontend/src/api/request.js` 是否带上 Authorization。
4. 检查网关 `JwtAuthenticationFilter`。

## 30. 当前项目的最小演示路径

答辩前最稳的演示路径：

1. `docker compose up -d`
2. 启动全部后端服务。
3. 启动前端。
4. 登录 `admin / 123456`。
5. 展示员工管理和角色权限。
6. 登录普通员工或用现有账号展示考勤打卡。
7. 提交一条请假或加班申请。
8. 用主管账号审批。
9. 管理员发布公告。
10. 员工查看公告并标记已读。
11. 打开 AI 问答，问“员工迟到怎么处理？”

如果 AI Key 或网络不稳定，AI 模块可以作为扩展演示，不要放在最前面。

## 31. 总结

OfficeFlow 当前已经具备一个课程级 OA 系统的完整骨架：

- 有前端页面。
- 有网关。
- 有多个微服务。
- 有认证鉴权。
- 有 MyBatis 数据访问。
- 有完整初始化 SQL。
- 有 Docker 中间件编排。
- 有一键启动脚本。
- 有 AI 扩展模块。

你理解这个项目时，不要先陷入每个类的细节。先记住主线：

```text
前端页面 -> src/api -> Gateway -> 具体微服务 Controller -> Service -> Mapper -> 数据库
```

所有模块都是围绕这条主线展开的。
