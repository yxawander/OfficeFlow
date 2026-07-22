# 审批服务 API 接口文档

## 基础信息
- 服务名称：`flow-service`
- 基础路径：`/api/flow`
- 统一返回：`ApiResponse<T>`
- 分页返回：`PageResult<T>`

---

## 1. 健康检查

### GET /api/flow/health
健康检查接口

**请求参数**：无

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Flow Service is healthy"
}
```

---

## 2. 员工端接口

### 2.1 提交申请

### POST /api/flow/applies
提交审批申请（请假/加班/补卡）

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前登录用户 ID |
| X-Login-Dept-Id | 否 | 当前用户部门 ID |

**请求体**：
```json
{
  "applyType": "LEAVE",
  "title": "年假申请",
  "reason": "个人原因休假",
  "startTime": "2026-07-22 09:00:00",
  "endTime": "2026-07-24 18:00:00",
  "durationHours": 24.00,
  "approverId": 2,
  "ccUserIds": [3, 4]
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| applyType | String | 是 | 申请类型：LEAVE / OVERTIME / CORRECTION |
| title | String | 是 | 申请标题，最长128字 |
| reason | String | 是 | 申请原因，最长500字 |
| startTime | String | 是 | 开始时间，格式 yyyy-MM-dd HH:mm:ss |
| endTime | String | 是 | 结束时间，格式 yyyy-MM-dd HH:mm:ss |
| durationHours | BigDecimal | 是 | 时长（小时数） |
| approverId | Long | 是 | 审批人用户 ID（直属领导） |
| ccUserIds | Long[] | 否 | 抄送人用户 ID 列表 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "applyNo": "FL20260721000001",
    "applyType": "LEAVE",
    "title": "年假申请",
    "reason": "个人原因休假",
    "durationHours": 24.00,
    "status": "PENDING",
    "currentNode": "DIRECT_MANAGER",
    "applicantId": 4,
    "applicantName": "普通员工",
    "applicantDeptName": "研发部",
    "approverId": 2,
    "approverName": "研发主管",
    "startTime": "2026-07-22 09:00:00",
    "endTime": "2026-07-24 18:00:00",
    "approvedAt": null,
    "createdAt": "2026-07-21 14:30:00",
    "approveRecords": [
      {
        "id": 1,
        "approverId": 4,
        "approverName": "普通员工",
        "action": "SUBMIT",
        "comment": null,
        "approvedAt": "2026-07-21 14:30:00"
      }
    ]
  }
}
```

---

### 2.2 我的申请列表

### GET /api/flow/applies/my
获取当前用户的申请列表（分页）

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前登录用户 ID |

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |
| applyType | String | 否 | 按类型筛选：LEAVE / OVERTIME / CORRECTION |
| status | String | 否 | 按状态筛选：PENDING / APPROVED / REJECTED / CANCELED |
| startDate | String | 否 | 开始日期筛选，yyyy-MM-dd |
| endDate | String | 否 | 结束日期筛选，yyyy-MM-dd |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 15,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "applyNo": "FL20260721000001",
        "applyType": "LEAVE",
        "title": "年假申请",
        "reason": "个人原因休假",
        "durationHours": 24.00,
        "status": "PENDING",
        "approverName": "研发主管",
        "startTime": "2026-07-22 09:00:00",
        "endTime": "2026-07-24 18:00:00",
        "createdAt": "2026-07-21 14:30:00"
      }
    ]
  }
}
```

---

### 2.3 申请详情

### GET /api/flow/applies/{id}
查看申请的完整详情（含审批记录时间线）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 申请单 ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "applyNo": "FL20260721000001",
    "applyType": "LEAVE",
    "title": "年假申请",
    "reason": "个人原因休假",
    "durationHours": 24.00,
    "status": "APPROVED",
    "currentNode": "DIRECT_MANAGER",
    "applicantId": 4,
    "applicantName": "普通员工",
    "applicantDeptName": "研发部",
    "approverId": 2,
    "approverName": "研发主管",
    "startTime": "2026-07-22 09:00:00",
    "endTime": "2026-07-24 18:00:00",
    "approvedAt": "2026-07-21 15:00:00",
    "createdAt": "2026-07-21 14:30:00",
    "approveRecords": [
      {
        "id": 1,
        "approverId": 4,
        "approverName": "普通员工",
        "action": "SUBMIT",
        "comment": null,
        "approvedAt": "2026-07-21 14:30:00"
      },
      {
        "id": 2,
        "approverId": 2,
        "approverName": "研发主管",
        "action": "APPROVE",
        "comment": "同意",
        "approvedAt": "2026-07-21 15:00:00"
      }
    ]
  }
}
```

---

### 2.4 撤销申请

### PUT /api/flow/applies/{id}/cancel
撤销待审批状态的申请（仅申请人可操作）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 申请单 ID |

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前登录用户 ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误情况**：
| 错误信息 | 说明 |
|---|---|
| 审批申请不存在 | 申请单 ID 无效或已删除 |
| 仅申请人可撤销自己的申请 | 当前用户不是该申请的申请人 |
| 仅待审批状态可撤销 | 申请状态不是 PENDING |

---

## 3. 审批端接口

> **权限要求**：`/api/flow/admin/**` 路径需要 ADMIN 或 MANAGER 角色。
> 网关通过 `X-Login-Roles` 请求头传递角色信息，后端 PermissionInterceptor 校验。

### 3.1 待审批列表

### GET /api/flow/admin/applies/pending
获取当前审批人的待审批列表（分页）

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前审批人用户 ID |

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |
| applyType | String | 否 | 按类型筛选：LEAVE / OVERTIME / CORRECTION |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "applyNo": "FL20260721000001",
        "applyType": "LEAVE",
        "title": "年假申请",
        "applicantName": "普通员工",
        "applicantDeptName": "研发部",
        "startTime": "2026-07-22 09:00:00",
        "endTime": "2026-07-24 18:00:00",
        "createdAt": "2026-07-21 14:30:00"
      }
    ]
  }
}
```

---

### 3.2 已审批列表

### GET /api/flow/admin/applies/processed
获取当前审批人已处理过的申请列表（分页）

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前审批人用户 ID |

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |
| applyType | String | 否 | 按类型筛选：LEAVE / OVERTIME / CORRECTION |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 12,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 2,
        "applyNo": "FL20260720000001",
        "applyType": "OVERTIME",
        "title": "周末加班申请",
        "status": "APPROVED",
        "applicantName": "普通员工",
        "myAction": "APPROVE",
        "approvedAt": "2026-07-20 16:00:00",
        "createdAt": "2026-07-20 10:00:00"
      }
    ]
  }
}
```

---

### 3.3 审批通过

### POST /api/flow/admin/applies/{id}/approve
审批通过申请（仅审批人可操作）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 申请单 ID |

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前审批人用户 ID |

**请求体**：
```json
{
  "comment": "同意"
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| comment | String | 否 | 审批意见 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误情况**：
| 错误信息 | 说明 |
|---|---|
| 审批申请不存在 | 申请单 ID 无效或已删除 |
| 您不是该申请的审批人 | 当前用户不是该申请的指定审批人 |
| 该申请已被处理 | 申请状态不是 PENDING |

---

### 3.4 审批驳回

### POST /api/flow/admin/applies/{id}/reject
驳回申请（仅审批人可操作，驳回意见必填）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 申请单 ID |

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前审批人用户 ID |

**请求体**：
```json
{
  "comment": "申请理由不充分，请补充详细说明"
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| comment | String | 是 | 驳回意见，不能为空 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误情况**：
| 错误信息 | 说明 |
|---|---|
| 审批申请不存在 | 申请单 ID 无效或已删除 |
| 您不是该申请的审批人 | 当前用户不是该申请的指定审批人 |
| 该申请已被处理 | 申请状态不是 PENDING |

---

## 4. 枚举值参考

### 4.1 申请类型（applyType）

| 值 | 说明 |
|---|---|
| LEAVE | 请假 |
| OVERTIME | 加班 |
| CORRECTION | 补卡 |

### 4.2 申请状态（status）

| 值 | 说明 | 终态 |
|---|---|---|
| PENDING | 待审批 | 否 |
| APPROVED | 已通过 | 是 |
| REJECTED | 已驳回 | 是 |
| CANCELED | 已撤销 | 是 |

### 4.3 审批动作（action）

| 值 | 说明 |
|---|---|
| SUBMIT | 提交申请 |
| APPROVE | 同意 |
| REJECT | 驳回 |
| CANCEL | 撤销 |

### 4.4 审批节点（currentNode）

| 值 | 说明 |
|---|---|
| DIRECT_MANAGER | 直属领导审批 |

---

## 5. 状态机

```
createApply() → PENDING ─┬─ approveApply() → APPROVED
                         ├─ rejectApply()  → REJECTED
                         └─ cancelApply()  → CANCELED
```

- 仅 **PENDING** 状态可执行通过、驳回、撤销操作
- 通过 / 驳回后状态变为终态，不再可操作
- 撤销仅限申请人本人操作

---

## 6. 申请单号生成规则

格式：`FL{yyyyMMdd}{6位序号}`

- 前缀 `FL`（Flow）
- 日期 8 位（年月日）
- 序号 6 位（每日从 000001 开始自增，通过 Redis INCR 实现）
- 示例：`FL20260721000001`

---

## 7. 状态码说明

| Code | Message | 说明 |
|---|---|---|
| 200 | success | 操作成功 |
| 400 | 参数错误 | 请求参数验证失败（@Valid 校验） |
| 403 | 无权限 | 无权访问管理端接口 |
| 500 | 审批申请不存在 | 申请单 ID 无效或已删除 |
| 500 | 仅申请人可撤销自己的申请 | 撤销操作权限不足 |
| 500 | 仅待审批状态可撤销 | 状态不允许撤销 |
| 500 | 您不是该申请的审批人 | 审批操作权限不足 |
| 500 | 该申请已被处理 | 申请已不是待审批状态 |
| 500 | 用户未登录 | 请求头缺少 X-Login-User-Id |

---

## 8. 请求头汇总

所有需要身份认证的接口通过网关传递以下请求头：

| 请求头 | 必填 | 说明 |
|---|---|---|
| Authorization | 是 | JWT Token（Bearer 格式） |
| X-Login-User-Id | 是 | 当前登录用户 ID（网关从 Token 解析注入） |
| X-Login-Username | 否 | 当前登录用户名 |
| X-Login-Roles | 否 | 用户角色编码列表（管理端接口需包含 ADMIN 或 MANAGER） |
| X-Login-Dept-Id | 否 | 当前用户部门 ID |
