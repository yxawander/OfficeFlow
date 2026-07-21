# OfficeFlow 企业智慧 OA 管理系统

OfficeFlow 是 JavaEE 企业级开发课程考核项目，采用前后端分离和 Spring Boot 微服务架构实现企业 OA 办公管理系统。

本仓库是完整项目目录，不依赖 `OfficeFlow` 外部的脚本或文件。小组成员拉取仓库后，只需要按本文档安装基础环境、启动中间件、启动后端服务和前端服务即可。

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
  docker-compose.yml
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
- Vite
- Element Plus
- Vue Router
- Pinia
- Axios
- ECharts

## 三、环境要求

每位成员本机需要安装：

| 环境 | 建议版本 | 用途 |
| --- | --- | --- |
| JDK | 21 或以上 | 运行 Spring Boot 后端 |
| Maven | 3.9 或以上 | 构建后端多模块工程 |
| Node.js | 20 或以上 | 运行前端项目 |
| pnpm | 10 或以上 | 前端包管理 |
| Docker Desktop | 推荐安装 | 一键启动 MySQL、Redis、Nacos |
| Git | 任意新版 | 代码同步 |

检查命令：

```powershell
java -version
mvn -version
node -v
pnpm -v
docker -v
git -v
```

## 四、启动中间件

项目内已经提供 `docker-compose.yml`，推荐所有成员统一使用 Docker 启动中间件。

在 `OfficeFlow` 根目录执行：

```powershell
docker compose up -d
```

启动后会创建：

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| MySQL | `localhost:3306` | 用户名 `root`，密码 `root`，数据库 `officeflow` |
| Redis | `localhost:6379` | 后端缓存、分布式锁预留 |
| Nacos | `http://localhost:8848/nacos` | 注册中心、配置中心 |

Nacos 默认账号密码通常是：

```text
nacos / nacos
```

查看容器状态：

```powershell
docker compose ps
```

停止中间件：

```powershell
docker compose down
```

如果需要同时删除本地数据卷，重新初始化数据库：

```powershell
docker compose down -v
docker compose up -d
```

数据库初始化脚本位于：

```text
docs/sql/init.sql
```

完整数据库设计说明位于：

```text
docs/design/database-design.md
```

后端公共框架说明位于：

```text
docs/design/backend-framework-guide.md
```

后端 1 交付说明位于：

```text
docs/design/backend-1-handoff.md
```

## 五、后端模块说明

| 模块 | 说明 | 端口 | 负责人 |
| --- | --- | --- | --- |
| `oa-common` | 公共返回结果、异常、JWT 工具等公共代码 | 无端口 | 后端 1 |
| `oa-gateway` | API 网关、跨域、路由、Token 校验 | `9000` | 后端 1 |
| `user-service` | 用户、部门、岗位、角色、菜单、权限 | `9101` | 后端 2 |
| `attendance-service` | 上下班打卡、考勤记录查询 | `9102` | 后端 3 |
| `flow-service` | 请假申请、加班申请、一级审批 | `9103` | 后端 3 |
| `notice-service` | 公告发布、公告已读 / 未读 | `9104` | 后端 3 |
| `report-service` | 数据大屏统计、考勤报表导出 | `9105` | 后端 3 / 文档负责人协助 |

## 六、后端启动方式

进入后端目录：

```powershell
cd backend
```

第一次拉取代码后，先检查 Maven 工程：

```powershell
mvn validate
```

首次启动前先安装全部后端模块到本地 Maven 仓库，保证各服务能解析到本地公共模块 `oa-common`：

```powershell
mvn clean install -DskipTests
```

日常开发时快速编译全部模块：

```powershell
mvn compile
```

如果需要打包：

```powershell
mvn clean package -DskipTests
```

### 启动顺序

先启动中间件：

```powershell
docker compose up -d
```

然后打开多个 VS Code 终端，分别启动后端服务。

终端 1：启动网关

```powershell
cd backend
mvn -pl oa-gateway spring-boot:run
```

终端 2：启动用户权限服务

```powershell
cd backend
mvn -pl user-service spring-boot:run
```

终端 3：启动考勤服务

```powershell
cd backend
mvn -pl attendance-service spring-boot:run
```

终端 4：启动审批服务

```powershell
cd backend
mvn -pl flow-service spring-boot:run
```

终端 5：启动公告服务

```powershell
cd backend
mvn -pl notice-service spring-boot:run
```

终端 6：启动统计服务

```powershell
cd backend
mvn -pl report-service spring-boot:run
```

如果只开发某一个模块，可以只启动：

- `oa-gateway`
- 自己负责的业务服务
- 该业务依赖的中间件

例如只开发用户权限模块，启动 `oa-gateway` 和 `user-service` 即可。

## 七、后端验证

所有前端请求统一访问网关：

```text
http://localhost:9000
```

健康检查接口：

| 服务 | 通过网关访问 |
| --- | --- |
| 用户服务 | `http://localhost:9000/api/user/health` |
| 考勤服务 | `http://localhost:9000/api/attendance/health` |
| 审批服务 | `http://localhost:9000/api/flow/health` |
| 公告服务 | `http://localhost:9000/api/notice/health` |
| 统计服务 | `http://localhost:9000/api/report/health` |

也可以直接访问服务端口：

| 服务 | 直接访问 |
| --- | --- |
| 用户服务 | `http://localhost:9101/api/user/health` |
| 考勤服务 | `http://localhost:9102/api/attendance/health` |
| 审批服务 | `http://localhost:9103/api/flow/health` |
| 公告服务 | `http://localhost:9104/api/notice/health` |
| 统计服务 | `http://localhost:9105/api/report/health` |

## 八、前端启动方式

进入前端目录：

```powershell
cd frontend
```

安装依赖：

```powershell
pnpm install
```

启动前端开发服务：

```powershell
pnpm dev
```

默认访问：

```text
http://127.0.0.1:5173/
```

构建前端：

```powershell
pnpm build
```

前端开发服务已经配置代理，页面里访问 `/api/**` 会转发到：

```text
http://localhost:9000
```

## 九、完整启动流程

新成员第一次拉取代码后，按这个顺序执行：

```powershell
git clone <仓库地址>
cd OfficeFlow
docker compose up -d
cd backend
mvn clean install -DskipTests
```

后端安装通过后，分别在多个终端启动：

```powershell
cd backend
mvn -pl oa-gateway spring-boot:run
```

```powershell
cd backend
mvn -pl user-service spring-boot:run
```

```powershell
cd backend
mvn -pl attendance-service spring-boot:run
```

```powershell
cd backend
mvn -pl flow-service spring-boot:run
```

```powershell
cd backend
mvn -pl notice-service spring-boot:run
```

```powershell
cd backend
mvn -pl report-service spring-boot:run
```

再启动前端：

```powershell
cd frontend
pnpm install
pnpm dev
```

浏览器访问：

```text
http://127.0.0.1:5173/
```

## 十、Git 协作方式

本项目周期短，建议全组先统一在 `main` 分支开发，减少分支合并成本。

每天开始开发前先拉取最新代码：

```powershell
git pull
```

提交代码：

```powershell
git add .
git commit -m "feat: 完成某某模块"
git push
```

注意：

- 提交前先运行自己模块的编译或构建命令。
- 不要提交 `node_modules/`、`dist/`、`target/`，这些已经被 `.gitignore` 排除。
- 每个人尽量只修改自己负责的目录。
- 如果 VS Code 提示有冲突，先不要乱点覆盖，确认冲突文件是谁负责的。

## 十一、分工目录

后端：

```text
backend/
  oa-common/              后端 1
  oa-gateway/             后端 1
  user-service/           后端 2
  attendance-service/     后端 3
  flow-service/           后端 3
  notice-service/         后端 3
  report-service/         后端 3 / 文档负责人协助
```

前端：

```text
frontend/src/
  layout/                 前端 1
  router/                 前端 1
  stores/                 前端 1
  views/login/            前端 1
  views/system/           前端 1
  views/attendance/       前端 2
  views/flow/             前端 2
  views/notice/           前端 2
  views/dashboard/        前端 2
```

文档：

```text
docs/
  sql/                    数据库脚本
  api/                    接口文档
  design/                 设计文档
  ppt/                    答辩 PPT、演示录屏、截图
```

## 十二、开发约定

- 后端统一返回 `ApiResponse<T>`。
- 后端业务异常统一使用 `BusinessException`。
- 所有前端请求统一走网关，不直接访问具体服务端口。
- 网关负责跨域处理和 Token 校验。
- 每个服务必须注册到 Nacos。
- 数据库表结构统一维护在 `docs/sql/init.sql`。
- 答辩前必须提前启动项目，准备好测试账号和演示数据。
