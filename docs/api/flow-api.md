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

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Flow Service is healthy"
}
```

---

## 2. 员工端接口（所有人可访问）

> 权限要求：登录即可，无角色限制。通过 `X-Login-User-Id` 请求头识别当前用户。

### 2.1 提交申请

### POST /api/flow/applies
提交审批申请（请假/加班/补卡），支持携带附件。

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
  "startTime": "2026-07-25 09:00:00",
  "endTime": "2026-07-26 18:00:00",
  "durationHours": 16.00,
  "ccUserIds": [3, 4],
  "attachmentIds": [1, 2]
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| applyType | String | 是 | 申请类型：LEAVE / OVERTIME / CORRECTION |
| title | String | 是 | 申请标题，最长128字 |
| reason | String | 是 | 申请原因，最长500字 |
| startTime | String | 是 | 开始时间，格式 yyyy-MM-dd HH:mm:ss。LEAVE/OVERTIME 不能早于当前时间 |
| endTime | String | 是 | 结束时间，格式 yyyy-MM-dd HH:mm:ss。LEAVE/OVERTIME 不能早于当前时间 |
| durationHours | BigDecimal | 是 | 时长（小时数） |
| ccUserIds | Long[] | 否 | 抄送人用户 ID 列表 |
| attachmentIds | Long[] | 否 | 附件 ID 列表（先调用附件上传接口获得 ID） |

> 审批人由服务端根据 `sys_user.manager_id` 自动确定，无需前端传递。

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "applyNo": "FL20260725000001",
    "applyType": "LEAVE",
    "title": "年假申请",
    "reason": "个人原因休假",
    "durationHours": 16.00,
    "status": "PENDING",
    "currentNode": "DIRECT_MANAGER",
    "applicantId": 4,
    "applicantName": "普通员工",
    "applicantDeptName": "研发部",
    "approverId": 2,
    "approverName": "研发主管",
    "startTime": "2026-07-25 09:00:00",
    "endTime": "2026-07-26 18:00:00",
    "approvedAt": null,
    "createdAt": "2026-07-24 14:30:00",
    "approveRecords": [
      {
        "id": 1,
        "approverId": 4,
        "approverName": "普通员工",
        "action": "SUBMIT",
        "comment": null,
        "approvedAt": "2026-07-24 14:30:00"
      }
    ],
    "attachments": [
      {
        "id": 1,
        "flowApplyId": 1,
        "fileName": "请假申请表.pdf",
        "fileUrl": "https://officeflow.oss-cn-hangzhou.aliyuncs.com/flow/2026-07/a1b2c3.pdf",
        "fileSize": 102400,
        "fileType": "application/pdf"
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
        "applyNo": "FL20260725000001",
        "applyType": "LEAVE",
        "title": "年假申请",
        "reason": "个人原因休假",
        "durationHours": 16.00,
        "status": "PENDING",
        "approverName": "研发主管",
        "startTime": "2026-07-25 09:00:00",
        "endTime": "2026-07-26 18:00:00",
        "createdAt": "2026-07-24 14:30:00"
      }
    ]
  }
}
```

---

### 2.3 申请详情

### GET /api/flow/applies/{id}
查看申请的完整详情（含审批记录时间线和附件）

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
    "applyNo": "FL20260725000001",
    "applyType": "LEAVE",
    "title": "年假申请",
    "reason": "个人原因休假",
    "durationHours": 16.00,
    "status": "APPROVED",
    "currentNode": "DIRECT_MANAGER",
    "applicantId": 4,
    "applicantName": "普通员工",
    "applicantDeptName": "研发部",
    "approverId": 2,
    "approverName": "研发主管",
    "startTime": "2026-07-25 09:00:00",
    "endTime": "2026-07-26 18:00:00",
    "approvedAt": "2026-07-24 15:00:00",
    "createdAt": "2026-07-24 14:30:00",
    "approveRecords": [
      {
        "id": 1,
        "approverId": 4,
        "approverName": "普通员工",
        "action": "SUBMIT",
        "comment": null,
        "approvedAt": "2026-07-24 14:30:00"
      },
      {
        "id": 2,
        "approverId": 2,
        "approverName": "研发主管",
        "action": "APPROVE",
        "comment": "同意",
        "approvedAt": "2026-07-24 15:00:00"
      }
    ],
    "attachments": [
      {
        "id": 1,
        "flowApplyId": 1,
        "fileName": "请假申请表.pdf",
        "fileUrl": "https://officeflow.oss-cn-hangzhou.aliyuncs.com/flow/2026-07/a1b2c3.pdf",
        "fileSize": 102400,
        "fileType": "application/pdf"
      }
    ]
  }
}
```

---

### 2.4 我的待审批列表

### GET /api/flow/applies/pending
获取当前用户作为审批人的待审批列表（分页）

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前审批人用户 ID |
| X-Login-Dept-Id | 否 | 当前用户部门 ID |

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
        "applyNo": "FL20260725000001",
        "applyType": "LEAVE",
        "title": "年假申请",
        "applicantName": "普通员工",
        "applicantDeptName": "研发部",
        "startTime": "2026-07-25 09:00:00",
        "endTime": "2026-07-26 18:00:00",
        "createdAt": "2026-07-24 14:30:00"
      }
    ]
  }
}
```

---

### 2.5 我的已审批列表

### GET /api/flow/applies/processed
获取当前用户已处理过的申请列表（分页，含通过和驳回）

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前审批人用户 ID |
| X-Login-Dept-Id | 否 | 当前用户部门 ID |

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

### 2.6 编辑申请

### PUT /api/flow/applies/{id}
编辑申请（仅申请人可操作，仅 PENDING 状态）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 申请单 ID |

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前登录用户 ID |

**请求体**：
```json
{
  "title": "修改后的标题",
  "reason": "修改后的原因",
  "startTime": "2026-07-26 09:00:00",
  "endTime": "2026-07-27 18:00:00",
  "durationHours": 16.00,
  "attachmentIds": [1, 3]
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | String | 是 | 申请标题 |
| reason | String | 是 | 申请原因 |
| startTime | String | 是 | 开始时间 |
| endTime | String | 是 | 结束时间 |
| durationHours | BigDecimal | 是 | 时长（小时数） |
| attachmentIds | Long[] | 否 | 新的附件 ID 列表（传入后替换全部附件，传空数组清空附件） |

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
| 仅申请人可编辑自己的申请 | 当前用户不是该申请的申请人 |
| 仅待审批状态可编辑 | 申请状态不是 PENDING |

---

### 2.7 撤销申请

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

### 2.8 删除申请

### DELETE /api/flow/applies/{id}
删除申请（仅申请人可操作，软删除）

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
| 仅申请人可删除自己的申请 | 当前用户不是该申请的申请人 |

---

## 3. 附件接口

### 3.1 上传附件

### POST /api/flow/attachments/upload
上传附件到 OSS，返回附件信息。附件上传后须在提交/编辑申请时通过 `attachmentIds` 绑定。

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 上传人用户 ID |

**请求体**：`multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | MultipartFile | 是 | 上传文件 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fileName": "请假申请表.pdf",
    "fileUrl": "https://officeflow.oss-cn-hangzhou.aliyuncs.com/flow/2026-07/a1b2c3d4e5f6.pdf",
    "fileSize": 102400,
    "fileType": "application/pdf"
  }
}
```

---

### 3.2 删除附件

### DELETE /api/flow/attachments/{id}
删除附件（仅上传人可操作，同时删除 OSS 文件和数据库记录）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 附件 ID |

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
| 附件不存在 | 附件 ID 无效 |
| 只能删除自己上传的附件 | 当前用户不是该附件的上传人 |

---

## 4. 管理端接口（ADMIN/MANAGER 角色）

> 权限要求：`/api/flow/admin/**` 路径需要 ADMIN 或 MANAGER 角色，由 PermissionInterceptor 统一校验。
> 网关通过 `X-Login-Roles` 请求头传递角色信息。

### 4.1 审批通过

### POST /api/flow/admin/applies/{id}/approve
审批通过申请

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 申请单 ID |

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前审批人用户 ID |
| X-Login-Roles | 是 | 角色列表，需包含 ADMIN 或 MANAGER |

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

### 4.2 审批驳回

### POST /api/flow/admin/applies/{id}/reject
驳回申请（驳回意见必填）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 申请单 ID |

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前审批人用户 ID |
| X-Login-Roles | 是 | 角色列表，需包含 ADMIN 或 MANAGER |

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

### 4.3 所有已审批申请

### GET /api/flow/admin/applies/approved
获取所有已审批通过的申请列表（分页，跨人员查看）

**请求头**：
| 参数 | 必填 | 说明 |
|---|---|---|
| X-Login-User-Id | 是 | 当前登录用户 ID |
| X-Login-Dept-Id | 否 | 部门 ID（按申请人部门筛选） |
| X-Login-Roles | 是 | 角色列表，需包含 ADMIN 或 MANAGER |

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |
| applyType | String | 否 | 按类型筛选：LEAVE / OVERTIME / CORRECTION |
| startDate | String | 否 | 审批开始日期筛选，yyyy-MM-dd |
| endDate | String | 否 | 审批结束日期筛选，yyyy-MM-dd |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 25,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "applyNo": "FL20260725000001",
        "applyType": "LEAVE",
        "title": "年假申请",
        "reason": "个人原因休假",
        "durationHours": 16.00,
        "applicantName": "普通员工",
        "applicantDeptName": "研发部",
        "approverName": "研发主管",
        "startTime": "2026-07-25 09:00:00",
        "endTime": "2026-07-26 18:00:00",
        "approvedAt": "2026-07-24 15:00:00",
        "createdAt": "2026-07-24 14:30:00"
      }
    ]
  }
}
```

---

## 5. 枚举值参考

### 5.1 申请类型（applyType）

| 值 | 说明 |
|---|---|
| LEAVE | 请假 |
| OVERTIME | 加班 |
| CORRECTION | 补卡 |

### 5.2 申请状态（status）

| 值 | 说明 | 终态 |
|---|---|---|
| PENDING | 待审批 | 否 |
| APPROVED | 已通过 | 是 |
| REJECTED | 已驳回 | 是 |
| CANCELED | 已撤销 | 是 |

### 5.3 审批动作（action）

| 值 | 说明 |
|---|---|
| SUBMIT | 提交申请 |
| APPROVE | 同意 |
| REJECT | 驳回 |
| CANCEL | 撤销 |

### 5.4 审批节点（currentNode）

| 值 | 说明 |
|---|---|
| DIRECT_MANAGER | 直属领导审批 |

---

## 6. 状态机

```
createApply() → PENDING ─┬─ approveApply() → APPROVED
                         ├─ rejectApply()  → REJECTED
                         ├─ cancelApply()  → CANCELED
                         └─ 超时自动驳回   → REJECTED (系统操作)
```

- 仅 **PENDING** 状态可执行通过、驳回、撤销操作
- 通过 / 驳回后状态变为终态，不再可操作
- 撤销仅限申请人本人操作
- 超时自动驳回：PENDING 状态超过 48 小时（可配置），系统定时任务自动驳回

---

## 7. 申请单号生成规则

格式：`FL{yyyyMMdd}{6位序号}`

- 前缀 `FL`（Flow）
- 日期 8 位（年月日）
- 序号 6 位（每日从 000001 开始自增，通过 Redis INCR 实现）
- 示例：`FL20260725000001`

---

## 8. 附件管理

### 上传绑定流程

```
1. POST /api/flow/attachments/upload   → 上传文件到 OSS，获得附件 ID
2. POST /api/flow/applies              → 提交申请时传入 attachmentIds
3. 后端自动绑定: flowAttachmentMapper.updateFlowApplyId(attachmentIds, applyId)
```

### OSS 存储路径

```
格式: flow/{yyyy-MM}/{uuid}{扩展名}
示例: flow/2026-07/a1b2c3d4e5f6.pdf
```

### 编辑时附件替换

编辑申请时传入 `attachmentIds`，服务端会自动：
1. 解除旧附件关联（`deleteByApplyId`）
2. 绑定新附件（`updateFlowApplyId`）
3. 传空数组 `[]` 则清空全部附件

### 权限

- 上传：所有登录用户
- 删除：仅限上传人本人

---

## 9. 自动驳回（定时任务）

- 定时扫描 PENDING 状态且 `created_at` 超过配置时限的申请
- 默认时限：48 小时（通过 `flow.auto-reject.timeout-hours` 配置，Nacos 或 application.yml）
- 执行频率：每 5 分钟（cron: `0 */5 * * * ?`）
- 使用 Redis 分布式锁防止多实例重复执行
- 驳回记录 `approverId = 0`（系统），`comment = "审批超时（超过XX小时未处理），系统自动驳回"`

---

## 10. 状态码说明

| Code | Message | 说明 |
|---|---|---|
| 200 | success | 操作成功 |
| 400 | 参数错误 | 请求参数验证失败（@Valid 校验） |
| 403 | 无权限 | 无权访问管理端接口（缺少 ADMIN/MANAGER 角色） |
| 500 | 审批申请不存在 | 申请单 ID 无效或已删除 |
| 500 | 仅申请人可撤销自己的申请 | 撤销操作权限不足 |
| 500 | 仅待审批状态可撤销 | 状态不允许撤销 |
| 500 | 您不是该申请的审批人 | 审批操作权限不足 |
| 500 | 该申请已被处理 | 申请已不是待审批状态 |
| 500 | 用户未登录 | 请求头缺少 X-Login-User-Id |
| 500 | 未找到直属领导，无法提交申请 | 该员工未设置直属领导 |
| 500 | 仅申请人可编辑自己的申请 | 编辑操作权限不足 |
| 500 | 仅待审批状态可编辑 | 状态不允许编辑 |
| 500 | 仅申请人可删除自己的申请 | 删除操作权限不足 |
| 500 | 附件不存在 | 附件 ID 无效 |
| 500 | 只能删除自己上传的附件 | 附件删除权限不足 |
| 500 | OSS未配置，无法上传文件 | 服务端 OSS 配置缺失 |
| 500 | 请假和加班申请的开始时间不能早于当前时间 | 时间校验失败 |
| 500 | 请假和加班申请的结束时间不能早于当前时间 | 时间校验失败 |

---

## 11. 请求头汇总

所有需要身份认证的接口通过网关传递以下请求头：

| 请求头 | 必填 | 说明 |
|---|---|---|
| Authorization | 是 | JWT Token（Bearer 格式） |
| X-Login-User-Id | 是 | 当前登录用户 ID（网关从 Token 解析注入） |
| X-Login-Username | 否 | 当前登录用户名 |
| X-Login-Roles | 否 | 用户角色编码列表（管理端接口需包含 ADMIN 或 MANAGER） |
| X-Login-Dept-Id | 否 | 当前用户部门 ID |

---

## 12. 端点权限总览

| Method | Path | 角色要求 | 说明 |
|---|---|---|---|
| GET | `/api/flow/health` | 无 | 健康检查 |
| POST | `/api/flow/applies` | 登录 | 提交申请 |
| GET | `/api/flow/applies/my` | 登录 | 我的申请列表 |
| GET | `/api/flow/applies/{id}` | 登录 | 申请详情 |
| GET | `/api/flow/applies/pending` | 登录 | 我的待审批列表 |
| GET | `/api/flow/applies/processed` | 登录 | 我的已审批列表 |
| PUT | `/api/flow/applies/{id}` | 登录 | 编辑申请 |
| PUT | `/api/flow/applies/{id}/cancel` | 登录 | 撤销申请 |
| DELETE | `/api/flow/applies/{id}` | 登录 | 删除申请 |
| POST | `/api/flow/attachments/upload` | 登录 | 上传附件 |
| DELETE | `/api/flow/attachments/{id}` | 登录 | 删除附件 |
| POST | `/api/flow/admin/applies/{id}/approve` | ADMIN/MANAGER | 审批通过 |
| POST | `/api/flow/admin/applies/{id}/reject` | ADMIN/MANAGER | 审批驳回 |
| GET | `/api/flow/admin/applies/approved` | ADMIN/MANAGER | 所有已审批申请 |
