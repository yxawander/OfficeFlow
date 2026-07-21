# OfficeFlow 企业智慧 OA 管理系统

OfficeFlow 是 JavaEE 企业级开发课程考核项目，采用前后端分离和 Spring Boot 微服务架构实现企业 OA 办公管理系统。

## 一、项目结构

```text
OfficeFlow/
  backend/
    pom.xml
    oa-common/
    oa-gateway/
    user-service/
    attendance-service/
    flow-service/
    notice-service/
    report-service/
  frontend/
  docs/
    sql/
    api/
    design/
    ppt/
  小组分工表.md
```

## 二、技术栈

后端：

- Java 21+
- Spring Boot 3.2.x
- Spring Cloud 2023.x
- Spring Cloud Alibaba 2023.x
- Nacos 注册中心 / 配置中心
- Spring Cloud Gateway
- MySQL
- MyBatis
- Redis
- JWT
- Maven 多模块工程

前端：

- Vue3
- Element Plus
- Vue Router
- Pinia
- Axios
- ECharts

## 三、后端模块说明

| 模块 | 说明 | 负责人 |
| --- | --- | --- |
| `oa-common` | 公共返回结果、异常、JWT 工具等公共代码 | 后端 1 |
| `oa-gateway` | API 网关、跨域、路由、Token 校验 | 后端 1 |
| `user-service` | 用户、部门、岗位、角色、菜单、权限 | 后端 2 |
| `attendance-service` | 上下班打卡、考勤记录查询 | 后端 3 |
| `flow-service` | 请假申请、加班申请、一级审批 | 后端 3 |
| `notice-service` | 公告发布、公告已读 / 未读 | 后端 3 |
| `report-service` | 数据大屏统计、考勤报表导出 | 后端 3 / 文档负责人协助 |

## 四、环境要求

本机建议环境：

- JDK 21 或以上
- Maven 3.9 或以上
- Node.js 20 或以上
- MySQL 8.x
- Redis
- Nacos 2.x

当前开发环境可使用根目录的脚本启动：

```powershell
..\start-env.cmd
```

停止环境：

```powershell
..\stop-env.cmd
```

## 五、后端启动方式

进入后端目录：

```powershell
cd backend
```

检查 Maven 工程：

```powershell
mvn validate
```

编译全部模块：

```powershell
mvn clean package -DskipTests
```

分别启动服务：

```powershell
mvn -pl oa-gateway spring-boot:run
mvn -pl user-service spring-boot:run
mvn -pl attendance-service spring-boot:run
mvn -pl flow-service spring-boot:run
mvn -pl notice-service spring-boot:run
mvn -pl report-service spring-boot:run
```

默认端口：

| 服务 | 端口 |
| --- | --- |
| `oa-gateway` | `9000` |
| `user-service` | `9101` |
| `attendance-service` | `9102` |
| `flow-service` | `9103` |
| `notice-service` | `9104` |
| `report-service` | `9105` |

## 六、接口访问入口

前端统一访问网关：

```text
http://localhost:9000
```

网关路由前缀：

| 路由 | 服务 |
| --- | --- |
| `/api/user/**` | `user-service` |
| `/api/attendance/**` | `attendance-service` |
| `/api/flow/**` | `flow-service` |
| `/api/notice/**` | `notice-service` |
| `/api/report/**` | `report-service` |

## 七、Git 上传步骤

如果当前目录还没有 Git 仓库，在 `OfficeFlow` 目录执行：

```powershell
git init
git add .
git commit -m "init officeflow project scaffold"
```

绑定远程仓库：

```powershell
git remote add origin <你的Git仓库地址>
git branch -M main
git push -u origin main
```

后续每个人开发建议新建自己的分支：

```powershell
git checkout -b feature/user-service
```

提交代码：

```powershell
git add .
git commit -m "feat: add user service basic api"
git push
```

## 八、开发约定

- 后端统一返回 `ApiResponse<T>`。
- 后端业务异常统一使用 `BusinessException`。
- 所有前端请求统一走网关，不直接访问具体服务端口。
- 每个服务必须注册到 Nacos。
- 每个成员负责自己的模块，减少多人同时修改同一文件。
- 答辩前必须提前启动项目，准备好测试账号和演示数据。

