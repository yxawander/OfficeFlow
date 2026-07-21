# OfficeFlow 数据库设计说明

数据库名：`officeflow`

设计原则：

- 当前课程项目先使用一个 MySQL 库，按微服务模块划分表归属。
- 不设置物理外键，避免微服务拆分、初始化顺序和测试数据清理带来额外成本。
- 所有核心业务表保留 `created_at`、`updated_at`，需要软删除的表统一使用 `is_deleted`。
- 状态字段优先使用清晰字符串，例如 `PENDING`、`APPROVED`、`REJECTED`，方便前后端联调和答辩讲解。

## 表归属

| 模块 | 表 |
| --- | --- |
| `user-service` | `sys_user`、`sys_dept`、`sys_post`、`sys_role`、`sys_menu`、`sys_api_permission`、`sys_user_role`、`sys_role_menu`、`sys_role_api_permission`、`sys_config`、`sys_login_log`、`sys_operation_log` |
| `attendance-service` | `attendance_rule`、`attendance_group`、`attendance_record`、`attendance_correction_apply`、`attendance_monthly_report` |
| `flow-service` | `flow_apply`、`flow_approve_record`、`flow_cc`、`flow_attachment` |
| `notice-service` | `notice`、`notice_scope`、`notice_read` |
| `report-service` | `report_dashboard_snapshot`，同时读取用户、考勤、审批、公告数据做统计 |

## 核心业务关系

用户权限：

- `sys_user.dept_id` 关联部门。
- `sys_user.post_id` 关联岗位。
- `sys_user.manager_id` 表示直属领导，用于一级审批。
- `sys_user_role` 绑定用户和角色。
- `sys_role_menu` 控制前端菜单和按钮权限。
- `sys_role_api_permission` 预留接口权限控制。

考勤：

- `attendance_rule` 保存上下班时间、迟到阈值、分布式锁过期时间等参数。
- `attendance_group` 可按部门绑定考勤规则。
- `attendance_record` 每个员工每天一条记录，通过 `user_id + work_date` 唯一约束防止重复。
- `attendance_monthly_report` 用于月度报表、Excel 导出和数据大屏。

审批：

- `flow_apply` 是审批主表，覆盖请假、加班、补卡三类申请。
- `approver_id` 保存直属领导，满足课程要求的一级审批。
- `flow_approve_record` 保存提交、同意、驳回、取消等流转记录。
- `flow_cc` 和 `flow_attachment` 是拓展工作流预留。

公告：

- `notice` 保存公告主信息。
- `notice_scope` 控制可见范围，支持全员、部门、用户、角色。
- `notice_read` 记录用户已读状态，用于已读 / 未读和阅读率统计。

## 初始化账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `123456` | 系统管理员 |
| `manager` | `123456` | 部门主管 |
| `hr` | `123456` | 普通员工 |
| `employee` | `123456` | 普通员工 |

开发阶段密码暂时使用明文，后续用户模块开发时建议改为 BCrypt。

