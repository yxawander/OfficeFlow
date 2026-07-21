# 后端基础框架说明

本文档面向后端 2、后端 3，说明后续业务开发必须遵守的基础规范。

## 后端 1 已完成内容

- Maven 多模块父工程：`backend/pom.xml`
- 公共模块：`backend/oa-common`
- 网关模块：`backend/oa-gateway`
- 业务服务空壳：`user-service`、`attendance-service`、`flow-service`、`notice-service`、`report-service`
- Docker 中间件编排：`docker-compose.yml`
- 数据库初始化脚本：`docs/sql/init.sql`
- 数据库设计说明：`docs/design/database-design.md`

## 公共模块

统一返回：

```java
return ApiResponse.ok(data);
return ApiResponse.fail("错误信息");
```

分页返回：

```java
return ApiResponse.ok(PageResult.of(total, pageNum, pageSize, records));
```

业务异常：

```java
throw new BusinessException("员工不存在");
throw new BusinessException(ResultCode.FORBIDDEN);
```

JWT 工具：

```java
String token = JwtUtil.generateToken(subject, claims, secret, expireSeconds);
Claims claims = JwtUtil.parseClaims(token, secret);
```

## 网关规则

前端统一访问：

```text
http://localhost:9000
```

网关负责：

- 跨域处理
- 服务路由
- JWT Token 校验
- 健康检查白名单
- 登录接口白名单
- 向下游服务透传当前用户请求头

下游服务可以读取这些请求头：

```text
X-Login-User-Id
X-Login-Username
```

## 服务开发约定

- Controller 路径必须带服务前缀，例如 `/api/user`、`/api/attendance`。
- 返回值统一使用 `ApiResponse<T>`。
- 列表接口统一返回 `PageResult<T>`。
- 不要在业务服务里重复写统一异常处理，公共模块已经提供。
- 不要把其他服务的业务代码写进自己模块。
- 数据库表结构统一维护在 `docs/sql/init.sql`。

## 推荐包结构

每个业务服务建议使用：

```text
controller/
service/
service/impl/
mapper/
entity/
dto/
vo/
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
```

## 启动验证

启动中间件：

```powershell
docker compose up -d
```

编译后端：

```powershell
cd backend
mvn clean install -DskipTests
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
```

