# 接口文档

接口统一通过网关访问：

```text
http://localhost:9000
```

## 健康检查接口

| 服务 | 方法 | 路径 |
| --- | --- | --- |
| 用户服务 | GET | `/api/user/health` |
| 考勤服务 | GET | `/api/attendance/health` |
| 审批服务 | GET | `/api/flow/health` |
| 公告服务 | GET | `/api/notice/health` |
| 统计服务 | GET | `/api/report/health` |

