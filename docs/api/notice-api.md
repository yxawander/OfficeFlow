# 公告服务 API 接口文档

## 基础信息
- 服务名称：`notice-service`
- 基础路径：`/api/notice`
- 统一返回：`ApiResponse<T>`
- 分页返回：`PageResult<T>`

**路径规范**：
- 用户端：`/api/notice/notices/...`
- 管理端：`/api/notice/admin/notices/...`（需 ADMIN 或 MANAGER 角色）
- 附件：`/api/notice/admin/attachments/...`（需 ADMIN 或 MANAGER 角色）

---

## 1. 健康检查

### GET /api/notice/health
健康检查接口

**请求参数**：无

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Notice Service is healthy"
}
```

---

## 2. 用户端接口

所有路径前缀：`/api/notice`

### 2.1 公告列表（分页）

### GET /notices
获取当前用户可见的公告列表（按可见范围过滤）

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |
| keyword | String | 否 | 搜索关键词（标题/内容模糊查询） |
| noticeType | String | 否 | 公告类型：COMPANY/DEPT/SYSTEM |
| priority | String | 否 | 优先级：NORMAL/IMPORTANT/URGENT |
| readStatus | Integer | 否 | 阅读状态：0未读，1已读 |
| onlyPublished | Boolean | 否 | 仅已发布，默认true |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "title": "OfficeFlow 项目启动通知",
        "summary": "请各成员按照分工完成对应模块开发...",
        "noticeType": "COMPANY",
        "priority": "IMPORTANT",
        "publisherId": 1,
        "publisherName": "系统管理员",
        "publishTime": "2026-07-21 09:00:00",
        "expireTime": "2026-12-31 23:59:59",
        "readStatus": 0,
        "readAt": null
      }
    ]
  }
}
```

---

### 2.2 公告详情

### GET /notices/{id}
获取公告详情（自动标记已读，记录阅读事件）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 公告ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "OfficeFlow 项目启动通知",
    "content": "请各成员按照分工完成对应模块开发，并保持每日提交。",
    "noticeType": "COMPANY",
    "priority": "IMPORTANT",
    "publisherId": 1,
    "publisherName": "系统管理员",
    "publishTime": "2026-07-21 09:00:00",
    "expireTime": "2026-12-31 23:59:59",
    "viewCount": 128,
    "readCount": 85,
    "readStatus": 1,
    "readAt": "2026-07-21 10:30:00",
    "createdAt": "2026-07-21 08:00:00",
    "attachments": [
      {
        "id": 1,
        "noticeId": 1,
        "fileName": "会议纪要.pdf",
        "fileUrl": "https://oss.example.com/notice/2026-07/xxx.pdf",
        "fileSize": 102400,
        "fileType": "application/pdf"
      }
    ]
  }
}
```

---

### 2.3 公告预览（不标记已读）

### GET /notices/{id}/preview
预览公告详情（不更新阅读状态，用于管理员预览草稿等场景）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 公告ID |

**响应示例**：同详情接口，但不会触发阅读记录

---

### 2.4 标记已读/未读

### POST /notices/{id}/read-status
标记单条公告的阅读状态

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 公告ID |

**请求体**：
```json
{
  "readStatus": 0
}
```
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| readStatus | Integer | 是 | 0未读，1已读 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

---

### 2.5 批量标记已读

### POST /notices/batch-read
批量标记多条公告为已读

**请求体**：
```json
{
  "noticeIds": [1, 2, 3]
}
```
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| noticeIds | List\<Long\> | 是 | 公告ID列表 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": 3
}
```
返回成功处理的数量。

---

### 2.6 未读统计

### GET /notices/unread-count
获取当前用户未读公告统计

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| noticeType | String | 否 | 按类型筛选 |
| priority | String | 否 | 按优先级筛选 |

> 注：传入 noticeType 或 priority 筛选时，`byType` 和 `byPriority` 返回空对象，仅 `total` 有效。

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "byType": {
      "COMPANY": 3,
      "DEPT": 2,
      "SYSTEM": 0
    },
    "byPriority": {
      "URGENT": 1,
      "IMPORTANT": 2,
      "NORMAL": 2
    }
  }
}
```

---

## 3. 管理端接口

所有路径前缀：`/api/notice/admin`，需 ADMIN 或 MANAGER 角色（由 PermissionInterceptor 校验）。

### 3.1 创建公告（草稿）

### POST /admin/notices
创建公告，保存为草稿状态。支持设置定时发布时间和绑定已上传附件。

**请求体**：
```json
{
  "title": "年度总结会议通知",
  "content": "请各部门准备年度总结材料...",
  "noticeType": "COMPANY",
  "priority": "IMPORTANT",
  "scheduledTime": "2026-08-01 09:00:00",
  "expireTime": "2026-12-31 23:59:59",
  "scopes": [
    { "scopeType": "ALL" },
    { "scopeType": "DEPT", "scopeId": 2 }
  ],
  "attachmentIds": [1, 2]
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | String | 是 | 公告标题，最长128字 |
| content | String | 是 | 公告内容，富文本HTML |
| noticeType | String | 是 | 类型：COMPANY/DEPT/SYSTEM |
| priority | String | 是 | 优先级：NORMAL/IMPORTANT/URGENT |
| scheduledTime | LocalDateTime | 否 | 定时发布时间，为null则手动发布 |
| expireTime | LocalDateTime | 否 | 过期时间 |
| scopes | List\<NoticeScopeDTO\> | 是 | 可见范围数组 |
| attachmentIds | List\<Long\> | 否 | 已上传附件的ID列表（两阶段上传） |

**NoticeScopeDTO 结构**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| scopeType | String | 是 | ALL / DEPT / USER / ROLE |
| scopeId | Long | 否 | ALL时不传，DEPT传部门ID，USER传用户ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": 10
}
```
返回新创建的公告ID。

---

### 3.2 更新公告

### PUT /admin/notices/{id}
更新公告（仅草稿或已下线状态可编辑，已发布状态不可编辑）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 公告ID |

**请求体**：字段同创建公告（`NoticeUpdateDTO` 继承 `NoticeCreateDTO`，额外包含 `id` 字段）

```json
{
  "id": "10",
  "title": "年度总结会议通知（更新版）",
  "content": "请各部门准备年度总结材料，本周五提交...",
  "noticeType": "COMPANY",
  "priority": "URGENT",
  "scopes": [
    { "scopeType": "ALL" }
  ],
  "attachmentIds": [1, 3]
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | String | 是 | 公告ID |
| title | String | 是 | 公告标题 |
| content | String | 是 | 公告内容 |
| noticeType | String | 是 | 类型 |
| priority | String | 是 | 优先级 |
| scheduledTime | LocalDateTime | 否 | 定时发布时间 |
| expireTime | LocalDateTime | 否 | 过期时间 |
| scopes | List\<NoticeScopeDTO\> | 是 | 可见范围（全量替换） |
| attachmentIds | List\<Long\> | 否 | 附件ID列表（全量替换，传[]清空） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

---

### 3.3 发布公告

### POST /admin/notices/{id}/publish
发布公告（仅草稿或已下线状态可发布）。发布时设置 `publishTime = NOW()`。

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 公告ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": 10
}
```
返回公告ID。

---

### 3.4 下线公告

### POST /admin/notices/{id}/offline
下线公告（仅已发布状态可下线）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 公告ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": 10
}
```
返回公告ID。

---

### 3.5 删除公告

### DELETE /admin/notices/{id}
删除公告（仅草稿或已下线状态可删除）。同时删除可见范围记录和附件（OSS + DB）。

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 公告ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

---

### 3.6 管理员公告列表

### GET /admin/notices
获取所有公告（管理员视角，不含可见范围过滤）

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |
| keyword | String | 否 | 搜索关键词 |
| status | String | 否 | 状态：DRAFT/PUBLISHED/OFFLINE |
| noticeType | String | 否 | 类型筛选 |
| priority | String | 否 | 优先级筛选 |
| publisherId | Long | 否 | 发布人ID |
| startDate | String | 否 | 发布开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 发布结束日期（yyyy-MM-dd） |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 20,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "title": "OfficeFlow 项目启动通知",
        "noticeType": "COMPANY",
        "priority": "IMPORTANT",
        "publisherId": 1,
        "publisherName": "系统管理员",
        "publishTime": "2026-07-21 09:00:00",
        "status": "PUBLISHED",
        "readCount": 85,
        "viewCount": 128,
        "readRate": 66.41,
        "createdAt": "2026-07-21 08:00:00"
      }
    ]
  }
}
```

---

### 3.7 公告阅读详情

### GET /admin/notices/{id}/read-details
获取某条公告的阅读详情（按部门统计已读/未读人数）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 公告ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "noticeId": 1,
    "totalUsers": 128,
    "readUsers": 85,
    "unreadUsers": 43,
    "readRate": 66.41,
    "deptStats": [
      {
        "deptId": 2,
        "deptName": "研发部",
        "totalUsers": 50,
        "readUsers": 42,
        "unreadUsers": 8,
        "readRate": 84.00
      },
      {
        "deptId": 3,
        "deptName": "市场部",
        "totalUsers": 30,
        "readUsers": 18,
        "unreadUsers": 12,
        "readRate": 60.00
      }
    ]
  }
}
```

---

## 4. 附件管理接口

所有路径前缀：`/api/notice/admin`，需 ADMIN 或 MANAGER 角色。

附件采用两阶段上传模式：先上传获取附件ID，再在创建/更新公告时通过 `attachmentIds` 绑定。

### 4.1 上传附件

### POST /admin/attachments/upload
上传文件到 OSS，返回附件信息（此时 `noticeId` 为 null）

**请求方式**：`multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | MultipartFile | 是 | 上传的文件 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "noticeId": null,
    "fileName": "会议纪要.pdf",
    "fileUrl": "https://oss.example.com/notice/2026-07/abc123.pdf",
    "fileSize": 102400,
    "fileType": "application/pdf"
  }
}
```

---

### 4.2 获取附件URL

### GET /admin/attachments/{id}
获取附件的访问URL

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 附件ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": "https://oss.example.com/notice/2026-07/abc123.pdf"
}
```

---

### 4.3 删除附件

### DELETE /admin/attachments/{id}
删除附件（同时删除 OSS 文件和数据库记录）

**路径参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 附件ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

---

## 5. 状态码说明

| Code | Message | 说明 |
|---|---|---|
| 200 | success | 操作成功 |
| 400 | 参数错误 | 请求参数验证失败 |
| 403 | 无权限 | 无权访问或角色不足 |
| 404 | 公告不存在 | 公告ID不存在 |

---

## 6. 请求头说明

当前用户信息由网关从 JWT 解析后通过请求头透传：

```
Authorization: Bearer {jwt_token}
X-Login-User-Id: {user_id}
X-Login-Username: {username}
X-Login-Roles: [ADMIN, MANAGER]
```

`X-Login-Roles` 用于 PermissionInterceptor 校验管理端权限（解析 `ADMIN` 或 `MANAGER` 角色）。

---

## 7. 业务说明

### 7.1 公告状态流转

```
创建公告 → DRAFT ─┬─ 手动发布 ──→ PUBLISHED ──→ OFFLINE
                  ├─ 定时发布 ──→ PUBLISHED     (自动下线或手动)
                  └─ 删除
                       OFFLINE ──→ PUBLISHED (重新发布)
                       OFFLINE ──→ 删除
```

- **DRAFT**（草稿）：可编辑、可删除、可发布
- **PUBLISHED**（已发布）：不可编辑、不可删除，只能下线
- **OFFLINE**（已下线）：可编辑、可删除、可重新发布

### 7.2 定时发布

创建或更新公告时设置 `scheduledTime` 字段，公告保持 DRAFT 状态。服务端定时任务每分钟扫描一次，将 `scheduledTime <= NOW()` 的 DRAFT 公告自动发布。

- `scheduledTime` 为 `null`：手动发布，需调用发布接口
- `scheduledTime` 为非 null：到达指定时间自动发布

### 7.3 可见范围

通过 `notice_scope` 表控制公告可见性：

| scopeType | scopeId | 说明 |
|---|---|---|
| ALL | null | 所有人可见 |
| DEPT | 部门ID | 指定部门员工可见 |
| USER | 用户ID | 指定用户可见 |
| ROLE | 角色ID | 指定角色用户可见 |

用户列表查询时根据 `userId`、`deptId` 与 scope 表做交集过滤。

### 7.4 阅读追踪

- 首次访问详情页自动标记已读（插入 `notice_read` 记录，`readStatus=1`）
- 浏览次数每次访问详情页 +1（`viewCount`）
- 已读人数 = 去重的已标记用户数（`readCount`）
- 支持手动标记未读（`readStatus=0`），此时 `readAt` 置为 null

### 7.5 附件两阶段上传

```
阶段一：上传附件
  POST /admin/attachments/upload → 返回 AttachmentVO { id, fileName, fileUrl, ... }
  （此时 noticeId = null，附件尚未绑定到任何公告）

阶段二：绑定附件
  POST /admin/notices 创建公告时传入 attachmentIds: [1, 2]
  PUT  /admin/notices/{id} 更新公告时传入新的 attachmentIds（全量替换）
  （服务端将对应附件记录的 notice_id 更新为公告ID）

删除公告时，关联的附件文件会从 OSS 和数据库中同步清除。
```
