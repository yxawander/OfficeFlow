# 公告服务 API 接口文档

## 基础信息
- 服务名称：`notice-service`
- 基础路径：`/api/notice`
- 统一返回：`ApiResponse<T>`
- 分页返回：`PageResult<T>`

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

### 2.1 公告列表（分页）

### GET /notices
获取当前用户可见的公告列表

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
        "publishTime": "2026-07-21T09:00:00",
        "expireTime": "2026-12-31T23:59:59",
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
获取公告详情（自动标记已读）

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
    "publishTime": "2026-07-21T09:00:00",
    "expireTime": "2026-12-31T23:59:59",
    "viewCount": 128,
    "readCount": 85,
    "readStatus": 1,
    "readAt": "2026-07-21T10:30:00",
    "createdAt": "2026-07-21T08:00:00"
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

**响应示例**：同详情接口，但 `readStatus` 为实际状态

---

### 2.4 标记已读/未读

### POST /notices/{id}/read-status
标记公告阅读状态

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
批量标记公告为已读

**请求体**：
```json
{
  "noticeIds": [1, 2, 3]
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": 3
}
```
返回成功标记的数量

---

### 2.6 未读统计

### GET /notices/unread-count
获取当前用户未读公告数量

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| noticeType | String | 否 | 按类型筛选 |
| priority | String | 否 | 按优先级筛选 |

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

## 3. 管理员接口

### 3.1 创建公告（草稿）

### POST /notices
创建公告，默认为草稿状态

**请求体**：
```json
{
  "title": "年度总结会议通知",
  "content": "请各部门准备年度总结材料...",
  "noticeType": "COMPANY",
  "priority": "IMPORTANT",
  "expireTime": "2026-12-31T23:59:59",
  "scopes": [
    { "scopeType": "ALL", "scopeId": null },
    { "scopeType": "DEPT", "scopeId": 2 }
  ]
}
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | String | 是 | 公告标题，最多128字 |
| content | String | 是 | 公告内容，富文本 |
| noticeType | String | 是 | 类型：COMPANY/DEPT/SYSTEM |
| priority | String | 是 | 优先级：NORMAL/IMPORTANT/URGENT |
| expireTime | LocalDateTime | 否 | 过期时间 |
| scopes | List | 是 | 可见范围数组 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10,
    "title": "年度总结会议通知",
    "status": "DRAFT"
  }
}
```

---

### 3.2 更新公告

### PUT /notices/{id}
更新公告（仅草稿或已下线状态可编辑）

**请求体**：同创建公告

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

### POST /notices/{id}/publish
发布公告

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
    "id": 10,
    "status": "PUBLISHED",
    "publishTime": "2026-07-21T15:30:00"
  }
}
```

---

### 3.4 下线公告

### POST /notices/{id}/offline
下线公告（仅已发布状态可下线）

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10,
    "status": "OFFLINE"
  }
}
```

---

### 3.5 删除公告

### DELETE /notices/{id}
删除公告（仅草稿或已下线状态可删除）

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
获取所有公告（管理员视角）

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页条数 |
| keyword | String | 否 | 搜索关键词 |
| status | String | 否 | 状态：DRAFT/PUBLISHED/OFFLINE |
| publisherId | Long | 否 | 发布人ID |
| startDate | String | 否 | 发布开始日期 |
| endDate | String | 否 | 发布结束日期 |

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
        "publishTime": "2026-07-21T09:00:00",
        "status": "PUBLISHED",
        "readCount": 85,
        "viewCount": 128,
        "readRate": 66.41,
        "createdAt": "2026-07-21T08:00:00"
      }
    ]
  }
}
```

---

### 3.7 公告阅读详情

### GET /admin/notices/{id}/read-details
获取公告的阅读详情（按部门统计）

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
      }
    ]
  }
}
```

---

## 4. 状态码说明

| Code | Message | 说明 |
|---|---|---|
| 200 | success | 操作成功 |
| 400 | 参数错误 | 请求参数验证失败 |
| 403 | 无权限 | 无权访问或操作 |
| 404 | 公告不存在 | 公告ID不存在 |
| 409 | 状态冲突 | 当前状态不允许该操作 |

---

## 5. 请求头说明

所有需要身份认证的接口需携带：
```
Authorization: Bearer {jwt_token}
```

当前用户信息由网关通过请求头透传：
```
X-Login-User-Id: {user_id}
X-Login-Username: {username}
```