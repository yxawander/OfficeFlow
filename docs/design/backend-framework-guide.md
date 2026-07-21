# 后端开发与交接说明

本文档作为后端同学后续开发、联调和交接的统一说明。

## 一、后端整体架构

后端采用 Spring Boot + Spring Cloud 微服务架构：

```text
前端 Vue
  |
  v
oa-gateway
  |
  +-- user-service
  +-- attendance-service
  +-- flow-service
  +-- notice-service
  +-- report-service
```

公共能力放在 `oa-common`，各业务服务只实现自己负责的业务逻辑。

## 二、后端 1 已完成内容

- Maven 多模块父工程：`backend/pom.xml`
- 公共模块：`backend/oa-common`
- 网关模块：`backend/oa-gateway`
- 用户权限服务框架：`backend/user-service`
- 考勤服务框架：`backend/attendance-service`
- 审批服务框架：`backend/flow-service`
- 公告服务框架：`backend/notice-service`
- 报表统计服务框架：`backend/report-service`
- Docker 中间件编排：`docker-compose.yml`
- 数据库初始化脚本：`docs/sql/init.sql`
- 数据库设计说明：`docs/design/database-design.md`

当前基础框架已完成，大家先把本地环境配置好，确认中间件、后端服务、前端服务都能启动。后端细分工已经写在本文档中，后端 2、后端 3、前端同学可以基于 `main` 分支并行开发。

## 三、模块职责

| 模块 | 端口 | 职责 | 负责人 |
| --- | --- | --- | --- |
| `oa-common` | 无 | 统一返回、统一异常、JWT、公共常量、基础模型 | 后端 1 |
| `oa-gateway` | `9000` | 网关路由、跨域、Token 校验、用户信息透传 | 后端 1 |
| `user-service` | `9101` | 登录、员工、部门、岗位、角色、菜单、接口权限 | 后端 1 |
| `attendance-service` | `9102` | 上下班打卡、考勤记录、迟到早退判断、补卡和统计扩展 | 后端 2 |
| `flow-service` | `9103` | 请假、加班申请、一级审批、待办已办 | 后端 3 |
| `notice-service` | `9104` | 公告发布、公告列表、已读未读 | 后端 3 |
| `report-service` | `9105` | 首页统计、报表查询、数据看板接口 | 后端 3 |

## 四、当前阶段后端详细分工表

### 4.1 当前阶段开发边界

为了先把课程要求的基础功能跑通，当前阶段先按下面规则开发：

| 暂不做的内容 | 当前替代方案 | 后续再做的时间点 |
| --- | --- | --- |
| 微服务之间互相调用 | 课程要求里没有强制微服务互调，各服务直接使用当前登录用户请求头里的 `X-Login-User-Id`、`X-Login-Username`，需要跨模块数据时统一查数据库表 | 基础接口全部跑通后，如果时间充足再考虑 Feign 调用 |
| Nacos 动态托管业务参数 | 先不做。迟到阈值、早退阈值、打卡锁过期时间等参数可以先在代码里写成常量，把基础功能先全部实现 | 基础功能完成并完成前后端联调后，最后再接 Nacos 配置中心 |
| 复杂接口权限动态拦截 | 先完成角色、菜单、接口权限数据维护，网关先做 Token 校验 | 登录、菜单、角色分配跑通后，再补接口级权限校验 |
| 分布式事务 | 每个服务只提交自己模块的数据 | 如果后续加入跨服务写操作，再讨论事务方案 |
| 复杂工作流引擎 | 只做直属领导一级审批 | 基础版完成后再扩展多级审批 |

现在必须先完成的目标是：每个服务能独立启动、接口能通过网关访问、数据库能正确读写、前端能拿到真实数据。

### 4.2 后端 1：架构、网关、用户权限

负责目录：

```text
backend/oa-common/
backend/oa-gateway/
backend/user-service/
docs/sql/init.sql 中 user-service 相关表和初始化数据
```

负责数据表：

| 表 | 必须完成的内容 |
| --- | --- |
| `sys_user` | 员工账号、密码、姓名、部门、岗位、直属领导、状态 |
| `sys_dept` | 部门树、部门负责人、启用停用 |
| `sys_post` | 岗位列表、岗位编码、启用停用 |
| `sys_role` | 角色基础信息 |
| `sys_menu` | 前端菜单、按钮权限标识 |
| `sys_api_permission` | 接口权限数据维护，先做数据表和 CRUD |
| `sys_user_role` | 用户分配角色 |
| `sys_role_menu` | 角色分配菜单 |
| `sys_role_api_permission` | 角色分配接口权限 |
| `sys_login_log` | 登录成功、登录失败记录 |
| `sys_operation_log` | 基础操作日志，至少记录新增、修改、删除、审批等关键操作 |

必须完成的公共能力：

| 任务 | 具体要求 |
| --- | --- |
| 统一返回 | 所有接口返回 `ApiResponse<T>`，不能直接返回裸对象 |
| 统一异常 | 业务错误抛 `BusinessException`，由全局异常处理返回统一格式 |
| JWT 工具 | 登录成功签发 Token，网关能解析 Token |
| 登录用户透传 | 网关解析 Token 后向下游传 `X-Login-User-Id`、`X-Login-Username` |
| 跨域 | 前端 `5173` 访问网关 `9000` 不报 CORS |
| 路由 | `/api/user/**`、`/api/attendance/**`、`/api/flow/**`、`/api/notice/**`、`/api/report/**` 全部能转发 |

必须完成的接口：

| 功能 | 方法与路径 | 说明 |
| --- | --- | --- |
| 登录 | `POST /api/user/login` | 入参 `username`、`password`，返回 Token、用户信息、角色、权限 |
| 退出登录 | `POST /api/user/logout` | 无状态退出，前端删除 Token，后端可记录日志 |
| 当前用户 | `GET /api/user/profile` | 返回当前登录人基础信息 |
| 当前用户菜单 | `GET /api/user/menus/current` | 前端登录后生成菜单 |
| 部门树 | `GET /api/user/depts/tree` | 返回树形部门 |
| 部门新增 | `POST /api/user/depts` | 新增部门 |
| 部门修改 | `PUT /api/user/depts/{id}` | 修改部门 |
| 部门删除 | `DELETE /api/user/depts/{id}` | 逻辑删除 |
| 岗位列表 | `GET /api/user/posts` | 支持分页或全部查询 |
| 岗位新增 | `POST /api/user/posts` | 新增岗位 |
| 岗位修改 | `PUT /api/user/posts/{id}` | 修改岗位 |
| 岗位删除 | `DELETE /api/user/posts/{id}` | 逻辑删除 |
| 员工分页 | `GET /api/user/users` | 支持用户名、姓名、部门、状态筛选 |
| 员工新增 | `POST /api/user/users` | 新增员工，设置部门、岗位、直属领导 |
| 员工修改 | `PUT /api/user/users/{id}` | 修改员工资料 |
| 员工删除 | `DELETE /api/user/users/{id}` | 逻辑删除 |
| 员工状态 | `PUT /api/user/users/{id}/status` | 启用、停用 |
| 重置密码 | `PUT /api/user/users/{id}/password` | 管理员重置员工密码 |
| 角色分页 | `GET /api/user/roles` | 查询角色 |
| 角色新增 | `POST /api/user/roles` | 新增角色 |
| 角色修改 | `PUT /api/user/roles/{id}` | 修改角色 |
| 角色删除 | `DELETE /api/user/roles/{id}` | 逻辑删除 |
| 用户分配角色 | `PUT /api/user/users/{id}/roles` | 入参角色 ID 列表 |
| 角色分配菜单 | `PUT /api/user/roles/{id}/menus` | 入参菜单 ID 列表 |
| 角色分配接口权限 | `PUT /api/user/roles/{id}/api-permissions` | 入参接口权限 ID 列表 |

后端 1 交付标准：

- 能用 `admin / 123456` 登录并拿到 Token。
- 带 Token 访问网关下游接口能通过，不带 Token 被拦截。
- 前端能拿到用户信息、菜单、按钮权限。
- 员工、部门、岗位、角色可以完成增删改查。
- 后端 2、后端 3 可以通过 `sys_user.manager_id` 找到直属领导。

### 4.3 后端 2：考勤服务

负责目录：

```text
backend/attendance-service/
docs/sql/init.sql 中 attendance-service 相关表和初始化数据
```

负责数据表：

| 表 | 必须完成的内容 |
| --- | --- |
| `attendance_rule` | 上班时间、下班时间、迟到阈值、早退阈值、旷工阈值 |
| `attendance_group` | 部门和考勤规则绑定 |
| `attendance_record` | 每人每天一条打卡记录，上班打卡、下班打卡、状态判断 |
| `attendance_correction_apply` | 补卡申请数据预留，可以先做基础提交和查询 |
| `attendance_monthly_report` | 月度统计结果，供报表和大屏使用 |

必须完成的业务规则：

| 规则 | 具体要求 |
| --- | --- |
| 上班打卡 | 当天没有记录则新建，有记录但已经上班打卡则提示不能重复打卡 |
| 下班打卡 | 必须在当天记录上更新下班时间，没有上班打卡时给出明确错误 |
| 每日唯一 | `user_id + work_date` 只能有一条记录 |
| 迟到判断 | 当前打卡时间晚于规则上班时间加迟到阈值，记为迟到 |
| 早退判断 | 下班时间早于规则下班时间减早退阈值，记为早退 |
| 缺勤判断 | 基础版可以在查询或月报生成时按缺少打卡记录判断 |
| 当前登录人 | 优先从 `X-Login-User-Id` 取用户 ID |
| 考勤参数 | 当前可以先在代码中写死默认值，不接 Nacos 动态配置；如果已经完成规则表 CRUD，再从 `attendance_rule` 表读取 |
| 防重复提交 | 当前先依赖数据库唯一约束和业务判断，Redis 锁留到第二阶段 |

必须完成的接口：

| 功能 | 方法与路径 | 说明 |
| --- | --- | --- |
| 上班打卡 | `POST /api/attendance/check-in` | 写入 `check_in_time`、IP、备注、状态 |
| 下班打卡 | `POST /api/attendance/check-out` | 写入 `check_out_time`、工作分钟、早退状态 |
| 今日记录 | `GET /api/attendance/today` | 返回当前登录人今天打卡状态 |
| 个人记录 | `GET /api/attendance/records` | 支持日期范围、分页 |
| 记录详情 | `GET /api/attendance/records/{id}` | 查询单条记录 |
| 考勤规则列表 | `GET /api/attendance/rules` | 管理端查看规则 |
| 考勤规则保存 | `POST /api/attendance/rules` | 新增规则 |
| 考勤规则修改 | `PUT /api/attendance/rules/{id}` | 修改规则 |
| 考勤组列表 | `GET /api/attendance/groups` | 查看部门绑定的考勤组 |
| 考勤组保存 | `POST /api/attendance/groups` | 绑定部门和规则 |
| 月度统计 | `GET /api/attendance/statistics/monthly` | 返回出勤、迟到、早退、缺勤天数 |
| 生成月报 | `POST /api/attendance/reports/monthly/generate` | 按月份生成统计数据 |

后端 2 交付标准：

- 员工登录后可以完成上班、下班打卡。
- 同一员工同一天不能重复上班打卡。
- 能查询本人今天和历史考勤记录。
- 能根据规则判断正常、迟到、早退。
- 能产出月度统计数据，给前端大屏或报表使用。

### 4.4 后端 3：审批、公告、报表

负责目录：

```text
backend/flow-service/
backend/notice-service/
backend/report-service/
docs/sql/init.sql 中 flow、notice、report 相关表和初始化数据
```

负责数据表：

| 模块 | 表 | 必须完成的内容 |
| --- | --- | --- |
| 审批 | `flow_apply` | 请假、加班申请主表，保存申请人、审批人、状态、原因、时间 |
| 审批 | `flow_approve_record` | 提交、同意、驳回、取消的审批记录 |
| 审批 | `flow_cc` | 抄送预留，基础版可以只建表不做复杂功能 |
| 审批 | `flow_attachment` | 附件预留，基础版可以只建表不做上传 |
| 公告 | `notice` | 公告标题、内容、发布人、状态、发布时间 |
| 公告 | `notice_scope` | 公告可见范围，基础版优先支持全员和部门 |
| 公告 | `notice_read` | 用户已读记录 |
| 报表 | `report_dashboard_snapshot` | 首页统计快照，基础版也可以实时查询后返回 |

审批服务必须完成的业务规则：

| 规则 | 具体要求 |
| --- | --- |
| 请假申请 | 员工提交开始时间、结束时间、请假类型、原因 |
| 加班申请 | 员工提交开始时间、结束时间、加班原因 |
| 审批人 | 当前阶段直接查数据库表 `sys_user.manager_id`，不调用 `user-service` |
| 一级审批 | 直属领导只能同意或驳回待审批单据 |
| 待办列表 | 查询 `approver_id = 当前用户` 且状态为 `PENDING` 的申请 |
| 已办列表 | 查询当前用户已经审批过的记录 |
| 我的申请 | 查询 `apply_user_id = 当前用户` 的申请 |
| 状态流转 | `PENDING -> APPROVED`、`PENDING -> REJECTED`、`PENDING -> CANCELED` |

审批服务必须完成的接口：

| 功能 | 方法与路径 | 说明 |
| --- | --- | --- |
| 提交请假 | `POST /api/flow/applications/leave` | 创建请假申请 |
| 提交加班 | `POST /api/flow/applications/overtime` | 创建加班申请 |
| 我的申请 | `GET /api/flow/applications/my` | 当前登录人提交过的单据 |
| 申请详情 | `GET /api/flow/applications/{id}` | 申请详情和审批记录 |
| 取消申请 | `PUT /api/flow/applications/{id}/cancel` | 只能取消自己的待审批申请 |
| 待办列表 | `GET /api/flow/tasks/todo` | 直属领导待审批 |
| 已办列表 | `GET /api/flow/tasks/done` | 当前用户审批过的记录 |
| 同意 | `PUT /api/flow/tasks/{id}/approve` | 审批通过 |
| 驳回 | `PUT /api/flow/tasks/{id}/reject` | 审批驳回，必须填写意见 |

公告服务必须完成的接口：

| 功能 | 方法与路径 | 说明 |
| --- | --- | --- |
| 发布公告 | `POST /api/notice/notices` | 管理员发布公告 |
| 修改公告 | `PUT /api/notice/notices/{id}` | 修改未发布或已发布公告 |
| 删除公告 | `DELETE /api/notice/notices/{id}` | 逻辑删除 |
| 管理端列表 | `GET /api/notice/notices/admin` | 管理员查看全部公告 |
| 员工公告列表 | `GET /api/notice/notices` | 当前登录人可见公告 |
| 公告详情 | `GET /api/notice/notices/{id}` | 返回公告内容和已读状态 |
| 标记已读 | `PUT /api/notice/notices/{id}/read` | 写入 `notice_read` |
| 未读数量 | `GET /api/notice/notices/unread-count` | 首页角标使用 |

报表服务必须完成的接口：

| 功能 | 方法与路径 | 说明 |
| --- | --- | --- |
| 首页汇总 | `GET /api/report/dashboard/summary` | 用户数、今日打卡数、待办数、公告未读数 |
| 考勤统计 | `GET /api/report/dashboard/attendance` | 月度迟到、早退、缺勤趋势 |
| 审批统计 | `GET /api/report/dashboard/flow` | 请假、加班、审批状态数量 |
| 公告统计 | `GET /api/report/dashboard/notice` | 公告阅读率 |

后端 3 交付标准：

- 员工可以提交请假、加班申请。
- 直属领导可以在待办中看到申请并同意或驳回。
- 员工可以看到自己的申请状态变化。
- 管理员可以发布公告，员工可以查看并标记已读。
- 首页统计接口能返回前端大屏需要的数据。

### 4.5 三个后端的协作接口约定

| 约定 | 说明 |
| --- | --- |
| 前端入口 | 前端统一请求 `http://localhost:9000`，不要直接写业务服务端口 |
| 当前用户 | 所有业务服务都从请求头 `X-Login-User-Id` 获取当前用户 ID |
| 返回格式 | 成功统一 `ApiResponse.ok(data)`，失败统一 `ApiResponse.fail(message)` 或抛 `BusinessException` |
| 分页参数 | 统一使用 `pageNum`、`pageSize` |
| 时间格式 | 请求和响应统一使用 `yyyy-MM-dd HH:mm:ss`，日期使用 `yyyy-MM-dd` |
| 逻辑删除 | 有 `is_deleted` 的表都更新为 `1`，不要物理删除 |
| 状态值 | 审批状态用 `PENDING`、`APPROVED`、`REJECTED`、`CANCELED` |
| 权限控制 | 当前阶段先保证登录鉴权，接口级权限后补 |
| 服务调用 | 当前阶段不要写 Feign、RestTemplate、WebClient 调其他服务；需要用户、部门、直属领导等数据时统一查数据库表 |
| Nacos 配置 | 当前阶段不要把业务参数放进 Nacos，Nacos 只作为服务注册中心使用；业务参数可以先写死在代码里 |

### 4.6 推荐开发顺序

| 顺序 | 后端 1 | 后端 2 | 后端 3 |
| --- | --- | --- | --- |
| 第 1 步 | 确认 `oa-common`、网关、用户服务能启动 | 确认考勤服务能启动并连库 | 确认审批、公告、报表服务能启动并连库 |
| 第 2 步 | 完成登录、Token、当前用户接口 | 完成上班打卡、下班打卡 | 完成请假、加班提交 |
| 第 3 步 | 完成员工、部门、岗位 CRUD | 完成今日记录、个人记录查询 | 完成待办、已办、同意、驳回 |
| 第 4 步 | 完成角色、菜单、权限分配 | 完成考勤规则、考勤组 | 完成公告发布、列表、已读未读 |
| 第 5 步 | 完成登录日志、操作日志 | 完成月度统计和月报生成 | 完成首页统计接口 |
| 第 6 步 | 配合前端联调登录和系统管理页面 | 配合前端联调考勤页面 | 配合前端联调审批、公告、大屏 |
| 第 7 步 | 再补接口级权限控制 | 再补 Redis 防重复锁、Nacos 动态参数 | 再补附件、抄送、更多统计 |

每个后端提交代码前至少完成：

- 自己负责的服务可以单独启动。
- 通过网关访问自己的健康检查接口返回成功。
- 自己新增的接口至少用 Postman、Apifox 或 VS Code REST Client 测过一次。
- 新增或修改表结构后同步更新 `docs/sql/init.sql` 和 `docs/design/database-design.md`。
- 接口路径、请求参数、响应示例同步给前端。

## 五、公共模块能力

`oa-common` 已提供以下公共能力：

- `ApiResponse<T>`：统一接口返回
- `ResultCode`：统一状态码
- `BusinessException`：业务异常
- `GlobalExceptionHandler`：全局异常处理
- `PageResult<T>`：分页返回结构
- `JwtUtil`：JWT 生成、解析、校验
- `LoginUser`：当前登录用户模型
- `LoginUserHolder`：当前登录用户上下文预留
- `CommonConstants`：公共常量
- `SecurityConstants`：JWT Claim 常量
- `BaseEntity`：基础实体字段

统一返回示例：

```java
return ApiResponse.ok(data);
return ApiResponse.fail("错误信息");
```

分页返回示例：

```java
return ApiResponse.ok(PageResult.of(total, pageNum, pageSize, records));
```

业务异常示例：

```java
throw new BusinessException("员工不存在");
throw new BusinessException(ResultCode.FORBIDDEN);
```

JWT 工具示例：

```java
String token = JwtUtil.generateToken(subject, claims, secret, expireSeconds);
Claims claims = JwtUtil.parseClaims(token, secret);
```

## 六、网关规则

前端统一访问网关：

```text
http://localhost:9000
```

不要让前端直接访问 `9101`、`9102` 等具体业务服务端口，除非是在单独调试某个服务。

网关已提供：

- Nacos 服务注册
- 统一路由转发
- 跨域处理
- JWT Token 校验过滤器
- 登录接口白名单
- 健康检查接口白名单
- 向下游服务透传当前登录用户请求头

路由约定：

```text
/api/user/**        -> user-service
/api/attendance/**  -> attendance-service
/api/flow/**        -> flow-service
/api/notice/**      -> notice-service
/api/report/**      -> report-service
```

下游服务可以读取这些请求头：

```text
X-Login-User-Id
X-Login-Username
```

## 七、服务开发约定

- Controller 路径必须带服务前缀，例如 `/api/user`、`/api/attendance`。
- 接口返回值统一使用 `ApiResponse<T>`。
- 分页接口统一返回 `PageResult<T>`。
- 业务错误统一抛 `BusinessException`。
- 不要在业务服务里重复写统一异常处理，公共模块已经提供。
- 不要把其他服务的业务代码写进自己的模块。
- 数据库表结构统一维护在 `docs/sql/init.sql`。
- 跨模块数据第一阶段可以直接查公共数据库表，当前不要做 Feign 服务间调用。
- 提交前至少运行 `mvn compile` 或自己模块的启动命令。

## 八、推荐包结构

每个业务服务建议使用：

```text
controller/
service/
service/impl/
mapper/
entity/
dto/
vo/
config/
```

示例：

```text
user-service/src/main/java/com/officeflow/user/
  controller/
  service/
  service/impl/
  mapper/
  entity/
  dto/
  vo/
  config/
```

## 九、Nacos 与 Redis 说明

当前 Nacos 已用于服务注册，启动后可以在 Nacos 控制台看到：

```text
oa-gateway
user-service
attendance-service
flow-service
notice-service
report-service
```

Nacos 配置中心依赖和基础配置已接入，各服务启动时会监听自己的配置：

```text
服务名
服务名.properties
```

注意：当前阶段先不要做 Nacos 动态业务参数。考勤时间、迟到阈值、早退阈值、打卡锁过期时间可以先在代码里写死默认值，把打卡、查询、审批、公告这些基础功能先全部实现。等基础接口全部完成后，再在 `attendance-service` 中补充业务配置读取，例如：

```yaml
attendance:
  check-lock-seconds: 10
  late-threshold-minutes: 10
  early-leave-threshold-minutes: 10
```

Redis 容器和依赖已准备好，当前阶段可以先不接。等打卡接口和查询接口稳定后，再优先用于考勤打卡防重复锁：

```text
attendance:check-lock:{userId}:{date}
```

`check-lock-seconds` 后续再从 Nacos 动态配置中读取。

## 十、启动方式

在 `OfficeFlow` 根目录启动中间件：

```powershell
docker compose up -d
```

进入后端目录：

```powershell
cd backend
```

首次启动前先安装全部后端模块到本地 Maven 仓库，保证各服务能解析到本地公共模块 `oa-common`：

```powershell
mvn clean install -DskipTests
```

启动网关：

```powershell
mvn -pl oa-gateway spring-boot:run
```

启动业务服务：

```powershell
mvn -pl user-service spring-boot:run
mvn -pl attendance-service spring-boot:run
mvn -pl flow-service spring-boot:run
mvn -pl notice-service spring-boot:run
mvn -pl report-service spring-boot:run
```

前端统一访问：

```text
http://localhost:9000
```

## 十一、健康检查

通过网关检查：

```text
http://localhost:9000/api/user/health
http://localhost:9000/api/attendance/health
http://localhost:9000/api/flow/health
http://localhost:9000/api/notice/health
http://localhost:9000/api/report/health
```

Nacos 控制台：

```text
http://127.0.0.1:8848/nacos
```

默认账号密码：

```text
nacos / nacos
```

MySQL 连接信息：

```text
Host: 127.0.0.1
Port: 3307
User: root
Password: root
Database: officeflow
```

## 十二、后续加分项

基础接口全部完成并完成前后端联调后，再考虑下面内容：

| 加分项 | 建议负责人 | 说明 |
| --- | --- | --- |
| Nacos 动态业务参数 | 后端 2 | 把 `attendance_rule` 中的打卡锁过期时间、迟到阈值等同步到 Nacos，使用 `@RefreshScope` 动态刷新 |
| Redis 防重复打卡锁 | 后端 2 | 避免短时间重复点击打卡按钮造成并发写入 |
| 接口级权限控制 | 后端 1 | 网关根据角色接口权限判断是否允许访问 |
| 审批附件 | 后端 3 | 请假、加班申请上传附件 |
| 审批抄送 | 后端 3 | 审批完成后向指定人员展示抄送记录 |
| Excel 报表导出 | 后端 2 或后端 3 | 导出月度考勤报表或审批统计 |
