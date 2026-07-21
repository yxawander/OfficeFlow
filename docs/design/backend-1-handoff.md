# 后端 1 交付说明

负责人：后端 1，架构与网关负责人

当前状态：基础框架已完成，后端 2、后端 3、前端同学可以基于 `main` 分支并行开发。

## 已完成内容

- Maven 多模块父工程：`backend/pom.xml`
- 公共模块：`backend/oa-common`
- 网关模块：`backend/oa-gateway`
- 用户权限服务空壳：`backend/user-service`
- 考勤服务空壳：`backend/attendance-service`
- 审批服务空壳：`backend/flow-service`
- 公告服务空壳：`backend/notice-service`
- 报表统计服务空壳：`backend/report-service`
- Docker 中间件编排：`docker-compose.yml`
- 数据库初始化脚本：`docs/sql/init.sql`
- 数据库设计说明：`docs/design/database-design.md`
- 后端公共框架说明：`docs/design/backend-framework-guide.md`

## 公共模块能力

`oa-common` 已提供：

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

## 网关能力

`oa-gateway` 已提供：

- Nacos 服务注册
- 统一路由转发
- 跨域配置
- JWT Token 校验过滤器
- 登录接口白名单
- 健康检查接口白名单
- 向下游服务透传用户请求头

下游服务可读取：

```text
X-Login-User-Id
X-Login-Username
```

## 启动方式

在 `OfficeFlow` 根目录启动中间件：

```powershell
docker compose up -d
```

编译后端：

```powershell
cd backend
mvn compile
```

启动网关：

```powershell
mvn -pl oa-gateway spring-boot:run
```

启动自己的服务，例如用户服务：

```powershell
mvn -pl user-service spring-boot:run
```

健康检查：

```text
http://localhost:9000/api/user/health
http://localhost:9000/api/attendance/health
http://localhost:9000/api/flow/health
http://localhost:9000/api/notice/health
http://localhost:9000/api/report/health
```

## 后续分工入口

后端 2 从这里开始：

```text
backend/user-service
```

负责：

- 登录接口
- 员工 CRUD
- 部门 CRUD
- 岗位 CRUD
- 角色 CRUD
- 菜单权限
- 用户角色分配
- 权限菜单返回

后端 3 从这里开始：

```text
backend/attendance-service
backend/flow-service
backend/notice-service
backend/report-service
```

负责：

- 上下班打卡
- 个人考勤记录
- 请假 / 加班申请
- 一级审批
- 待办 / 已办列表
- 公告发布
- 公告已读 / 未读
- 数据大屏统计接口
- 月度考勤报表导出

前端同学统一访问：

```text
http://localhost:9000
```

不要直接访问具体服务端口，除非是在单独调试接口。

## 开发约定

- 全组先统一在 `main` 分支开发。
- 每天开发前先 `git pull`。
- 后端接口统一返回 `ApiResponse<T>`。
- 分页接口统一返回 `PageResult<T>`。
- 业务错误统一抛 `BusinessException`。
- 数据库表结构统一维护在 `docs/sql/init.sql`。
- 不要把其他服务的业务代码写进自己的模块。
- 提交前至少运行 `mvn compile` 或自己模块的启动命令。

