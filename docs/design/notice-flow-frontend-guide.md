# OfficeFlow 前端工程开发指南

本文档供前端/AI 辅助工具开发 Vue 3 前端项目时使用。包含公告服务（notice-service）和审批服务（flow-service）的完整数据结构、API 端点、约束规范和开发提示词。

---

## 一、前端工程约束

### 1.1 技术栈

| 项 | 选型 |
|---|---|
| 框架 | Vue 3 (Composition API, `<script setup>`) |
| 构建工具 | Vite |
| UI 组件库 | Element Plus |
| HTTP 客户端 | Axios |
| 路由 | Vue Router 4 |
| 状态管理 | Pinia |
| CSS 方案 | 任意（推荐 SCSS 或 UnoCSS） |

### 1.2 端口与代理

| 项 | 值 |
|---|---|
| 前端开发端口 | `5173` |
| 后端网关地址 | `http://localhost:9000` |
| 代理规则 | `/api/**` → `http://localhost:9000` |

Vite 配置参考：

```ts
// vite.config.ts
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true
      }
    }
  }
})
```

### 1.3 请求头规范

所有需要身份认证的请求必须携带以下请求头（网关 JWT 解析后透传）：

| 请求头 | 说明 | 示例值 |
|---|---|---|
| `Authorization` | JWT Token | `Bearer eyJhbG...` |
| `X-Login-User-Id` | 当前登录用户 ID | `100` |
| `X-Login-Username` | 当前登录用户名 | `zhangsan` |
| `X-Login-Roles` | 当前用户角色编码列表 | `ADMIN,MANAGER` |
| `X-Login-Dept-Id` | 当前用户部门 ID | `2` |

**重要**：前端在 Axios 拦截器中统一添加 `Authorization` 头即可，`X-Login-*` 头由网关从 JWT 解析后自动注入，前端不需要手动设置。

### 1.4 统一响应格式

所有接口返回统一格式：

```ts
// ApiResponse
interface ApiResponse<T> {
  code: number    // 状态码：200 成功，400 参数错误，403 无权限，500 失败
  message: string // 提示信息
  data: T         // 业务数据
}

// PageResult
interface PageResult<T> {
  total: number    // 总条数
  pageNum: number  // 当前页码
  pageSize: number // 每页条数
  records: T[]     // 数据列表
}
```

### 1.5 时间格式

| 场景 | 格式 |
|---|---|
| 请求体 / 响应体中的时间字段 | `yyyy-MM-dd HH:mm:ss` |
| 查询参数中的日期筛选 | `yyyy-MM-dd` |
| 审批时长 | `BigDecimal`（小时数，如 `"16"`） |

### 1.6 开发约束

1. **永远不要**在前端把请求发给 `9101`、`9103`、`9104` 等具体服务端口，统一走 `9000` 网关
2. Axios 封装类统一处理 `ApiResponse`，在拦截器中判断 `code === 200`，非 200 直接 `message.error()` 提示
3. 分页查询统一使用 `pageNum`（从 1 开始）和 `pageSize` 参数
4. 表单校验规则与后端 `@Valid` 注解保持一致
5. 状态值使用大写字符串常量（`PENDING`、`APPROVED` 等），建议在前端定义枚举
6. 富文本内容使用 `v-html` 渲染，注意 XSS 防护

---

## 二、公告服务（notice-service）

### 2.1 枚举常量

```ts
// 公告状态
const NoticeStatus = {
  DRAFT: 'DRAFT',
  PUBLISHED: 'PUBLISHED',
  OFFLINE: 'OFFLINE'
} as const;

// 公告类型
const NoticeType = {
  COMPANY: 'COMPANY',
  DEPT: 'DEPT',
  SYSTEM: 'SYSTEM'
} as const;

// 优先级
const NoticePriority = {
  NORMAL: 'NORMAL',
  IMPORTANT: 'IMPORTANT',
  URGENT: 'URGENT'
} as const;

// 阅读状态
const ReadStatus = {
  UNREAD: 0,
  READ: 1
} as const;

// 可见范围类型
const ScopeType = {
  ALL: 'ALL',
  DEPT: 'DEPT',
  USER: 'USER',
  ROLE: 'ROLE'
} as const;
```

### 2.2 API 端点

#### 用户端（基础路径：`/api/notice`）

| Method | Path | 功能 |
|---|---|---|
| GET | `/health` | 健康检查 |
| GET | `/notices` | 公告列表（分页） |
| GET | `/notices/{id}` | 公告详情（自动标记已读） |
| GET | `/notices/{id}/preview` | 公告预览（不标记已读） |
| POST | `/notices/{id}/read-status` | 标记已读/未读 |
| POST | `/notices/batch-read` | 批量标记已读 |
| GET | `/notices/unread-count` | 未读统计 |

#### 管理端（基础路径：`/api/notice/admin`，需 ADMIN 或 MANAGER 角色）

| Method | Path | 功能 |
|---|---|---|
| POST | `/notices` | 创建公告（保存为草稿） |
| PUT | `/notices/{id}` | 更新公告 |
| POST | `/notices/{id}/publish` | 发布公告 |
| POST | `/notices/{id}/offline` | 下线公告 |
| DELETE | `/notices/{id}` | 删除公告 |
| GET | `/notices` | 管理端公告列表（分页，全量查询） |
| GET | `/notices/{id}/read-details` | 阅读详情（按部门统计） |
| POST | `/attachments/upload` | 上传附件（multipart/form-data） |
| DELETE | `/attachments/{id}` | 删除附件（OSS + DB） |
| GET | `/attachments/{id}` | 获取附件URL |

### 2.3 数据结构

#### NoticeListVO — 公告列表项（用户端）

```ts
interface NoticeListVO {
  id: number
  title: string
  summary: string                    // 内容摘要（截取前若干字）
  noticeType: string                 // COMPANY | DEPT | SYSTEM
  priority: string                   // NORMAL | IMPORTANT | URGENT
  publisherId: number
  publisherName: string
  publishTime: string                // yyyy-MM-dd HH:mm:ss
  expireTime: string | null          // yyyy-MM-dd HH:mm:ss
  readStatus: number                 // 0 未读，1 已读
  readAt: string | null              // yyyy-MM-dd HH:mm:ss
}
```

#### NoticeDetailVO — 公告详情

```ts
interface NoticeDetailVO {
  id: number
  title: string
  content: string                    // 富文本 HTML
  noticeType: string
  priority: string
  publisherId: number
  publisherName: string
  publishTime: string | null
  expireTime: string | null
  viewCount: number                  // 浏览次数
  readCount: number                  // 已读人数
  readStatus: number                 // 当前用户阅读状态：0=未读，1=已读
  readAt: string | null              // 当前用户阅读时间
  createdAt: string
  attachments: AttachmentVO[]        // 附件列表
}
```

#### AdminNoticeListVO — 公告列表项（管理端）

```ts
interface AdminNoticeListVO {
  id: number
  title: string
  noticeType: string
  priority: string
  publisherId: number
  publisherName: string
  publishTime: string | null
  status: string                     // DRAFT | PUBLISHED | OFFLINE
  readCount: number                  // 已读人数
  viewCount: number                  // 浏览次数
  readRate: number                   // 阅读率（百分比，如 66.41）
  createdAt: string
}
```

#### NoticeReadDetailVO — 阅读详情

```ts
interface NoticeReadDetailVO {
  noticeId: number
  totalUsers: number
  readUsers: number
  unreadUsers: number
  readRate: number
  deptStats: DeptStatVO[]
}

interface DeptStatVO {
  deptId: number
  deptName: string
  totalUsers: number
  readUsers: number
  unreadUsers: number
  readRate: number
}
```

#### UnreadCountVO — 未读统计

```ts
interface UnreadCountVO {
  total: number
  byType: Record<string, number>     // { "COMPANY": 3, "DEPT": 2, "SYSTEM": 0 }
  byPriority: Record<string, number> // { "URGENT": 1, "IMPORTANT": 2, "NORMAL": 2 }
}
```

#### AttachmentVO — 附件信息

```ts
interface AttachmentVO {
  id: number
  noticeId: number | null            // 关联的公告 ID（上传时为空，绑定后回填）
  fileName: string                   // 原始文件名
  fileUrl: string                    // OSS 文件访问 URL，可直接下载
  fileSize: number                   // 文件大小（字节）
  fileType: string                   // MIME 类型，如 application/pdf
}
```

#### NoticeCreateDTO — 创建公告请求体

```ts
interface NoticeCreateDTO {
  title: string                      // 必填，最长 128
  content: string                    // 必填，富文本 HTML
  noticeType: string                 // 必填，COMPANY | DEPT | SYSTEM
  priority: string                   // 必填，NORMAL | IMPORTANT | URGENT
  scheduledTime?: string             // 定时发布时间（yyyy-MM-dd HH:mm:ss），null 为手动发布
  expireTime?: string                // 过期时间（yyyy-MM-dd HH:mm:ss）
  scopes: NoticeScopeDTO[]           // 必填
  attachmentIds?: number[]           // 已上传附件 ID 列表（先上传再绑定）
}

interface NoticeScopeDTO {
  scopeType: string                  // ALL | DEPT | USER | ROLE
  scopeId?: number                   // ALL 时不传，其他类型传对应 ID
}
```

#### NoticeUpdateDTO — 更新公告请求体

```ts
// 继承 NoticeCreateDTO 所有字段，额外包含 id：
interface NoticeUpdateDTO extends NoticeCreateDTO {
  id: string                         // 必填，公告 ID
}
// 更新时 scopes 和 attachmentIds 均为全量替换（传 [] 清空）
```

#### NoticeReadStatusDTO — 标记已读请求体

```ts
interface NoticeReadStatusDTO {
  readStatus: number                 // 必填，0 未读，1 已读
}
```

#### BatchReadDTO — 批量已读请求体

```ts
interface BatchReadDTO {
  noticeIds: number[]                // 必填
}
```

#### 公告列表查询参数（NoticeQueryDTO）

```ts
interface NoticeQueryParams {
  pageNum?: number                   // 默认 1
  pageSize?: number                  // 默认 10
  keyword?: string                   // 搜索关键词（标题+内容模糊查询）
  noticeType?: string                // COMPANY | DEPT | SYSTEM
  priority?: string                  // NORMAL | IMPORTANT | URGENT
  readStatus?: number                // 0 未读，1 已读
  onlyPublished?: boolean            // 仅已发布，默认 true
  status?: string                    // DRAFT | PUBLISHED | OFFLINE（管理端）
  publisherId?: number               // 发布人 ID（管理端）
  startDate?: string                 // yyyy-MM-dd
  endDate?: string                   // yyyy-MM-dd
}
```

---

## 三、审批服务（flow-service）

### 3.1 枚举常量

```ts
// 申请状态
const ApplyStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  CANCELED: 'CANCELED'
} as const;

// 申请类型
const ApplyType = {
  LEAVE: 'LEAVE',
  OVERTIME: 'OVERTIME',
  CORRECTION: 'CORRECTION'
} as const;

// 审批动作
const ApproveAction = {
  SUBMIT: 'SUBMIT',
  APPROVE: 'APPROVE',
  REJECT: 'REJECT',
  CANCEL: 'CANCEL',
  AUTO_REJECT: 'AUTO_REJECT'
} as const;

// 审批动作标签映射
const ActionLabels: Record<string, string> = {
  SUBMIT: '提交申请',
  APPROVE: '审批通过',
  REJECT: '审批驳回',
  CANCEL: '撤销申请',
  AUTO_REJECT: '超时自动驳回'
};

// 状态标签映射
const StatusLabels: Record<string, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELED: '已撤销'
};

const StatusColors: Record<string, string> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CANCELED: 'info'
};

// 类型标签映射
const TypeLabels: Record<string, string> = {
  LEAVE: '请假',
  OVERTIME: '加班',
  CORRECTION: '补卡'
};
```

### 3.2 API 端点

#### 员工端（基础路径：`/api/flow`，所有登录用户可访问）

| Method | Path | 功能 |
|---|---|---|
| GET | `/health` | 健康检查 |
| POST | `/applies` | 提交申请（支持携带附件） |
| GET | `/applies/my` | 我的申请列表（分页） |
| GET | `/applies/{id}` | 申请详情（含审批记录 + 附件列表） |
| GET | `/applies/pending` | 我的待审批列表（分页） |
| GET | `/applies/processed` | 我的已审批列表（分页） |
| PUT | `/applies/{id}` | 编辑申请（仅 PENDING，支持更新附件） |
| PUT | `/applies/{id}/cancel` | 撤销申请（仅 PENDING） |
| DELETE | `/applies/{id}` | 删除申请（软删除） |
| POST | `/attachments/upload` | 上传附件 |
| DELETE | `/attachments/{id}` | 删除附件（仅上传人可操作） |

#### 管理端（基础路径：`/api/flow/admin`，需 ADMIN 或 MANAGER 角色）

| Method | Path | 功能 |
|---|---|---|
| POST | `/applies/{id}/approve` | 审批通过 |
| POST | `/applies/{id}/reject` | 审批驳回（意见必填） |
| GET | `/applies/approved` | 所有已审批通过申请（分页，跨人员查看） |

**权限说明**：`/api/flow/admin/**` 路径由 PermissionInterceptor 校验角色，需 ADMIN 或 MANAGER。其他端点所有登录用户可访问，权限在业务层按 userId 隔离。

### 3.3 数据结构

#### FlowApplyListVO — 我的申请列表项

```ts
interface FlowApplyListVO {
  id: number
  applyNo: string                    // 申请单号，如 FL20260721000001
  applyType: string                  // LEAVE | OVERTIME | CORRECTION
  title: string
  reason: string
  durationHours: number              // 时长（小时）
  status: string                     // PENDING | APPROVED | REJECTED | CANCELED
  approverName: string               // 审批人姓名
  startTime: string                  // yyyy-MM-dd HH:mm:ss
  endTime: string                    // yyyy-MM-dd HH:mm:ss
  createdAt: string                  // yyyy-MM-dd HH:mm:ss
}
```

#### FlowApplyDetailVO — 申请详情

```ts
interface FlowApplyDetailVO {
  id: number
  applyNo: string
  applyType: string
  title: string
  reason: string
  durationHours: number
  status: string
  currentNode: string                // 当前节点，如 DIRECT_MANAGER
  applicantId: number
  applicantName: string
  applicantDeptName: string
  approverId: number
  approverName: string
  startTime: string
  endTime: string
  approvedAt: string | null          // 最终审批时间
  createdAt: string
  approveRecords: FlowApproveRecordVO[]  // 审批记录时间线
  attachments: AttachmentVO[]        // 附件列表
}
```

#### FlowApproveRecordVO — 审批记录

```ts
interface FlowApproveRecordVO {
  id: number
  approverId: number
  approverName: string
  action: string                     // SUBMIT | APPROVE | REJECT | CANCEL | AUTO_REJECT
  comment: string | null             // 审批意见（超时自动驳回时显示超时说明）
  approvedAt: string                 // yyyy-MM-dd HH:mm:ss
}
```

#### AttachmentVO — 附件信息

```ts
interface AttachmentVO {
  id: number
  flowApplyId: number | null         // 关联的审批单 ID（上传时为空）
  fileName: string                   // 原始文件名
  fileUrl: string                    // OSS 文件访问 URL，可直接下载
  fileSize: number                   // 文件大小（字节）
  fileType: string                   // MIME 类型，如 application/pdf
}
```

#### FlowPendingVO — 待审批列表项

```ts
interface FlowPendingVO {
  id: number
  applyNo: string
  applyType: string
  title: string
  applicantName: string              // 申请人姓名
  applicantDeptName: string          // 申请人部门
  startTime: string
  endTime: string
  createdAt: string
}
```

#### FlowProcessedVO — 已审批列表项

```ts
interface FlowProcessedVO {
  id: number
  applyNo: string
  applyType: string
  title: string
  status: string                     // 申请最终状态
  applicantName: string
  myAction: string                   // 我的审批动作：APPROVE | REJECT
  approvedAt: string                 // 我审批的时间
  createdAt: string
}
```

#### FlowApprovedVO — 所有已审批列表项

```ts
interface FlowApprovedVO {
  id: number
  applyNo: string
  applyType: string
  title: string
  reason: string
  durationHours: number
  applicantId: number
  applicantName: string
  applicantDeptName: string
  approverId: number
  approverName: string
  startTime: string
  endTime: string
  approvedAt: string
  createdAt: string
}
```

#### FlowApplyCreateDTO — 提交申请请求体

```ts
interface FlowApplyCreateDTO {
  applyType: string                  // 必填，LEAVE | OVERTIME | CORRECTION
  title: string                      // 必填
  reason: string                     // 必填
  startTime: string                  // 必填，yyyy-MM-dd HH:mm:ss。LEAVE/OVERTIME 不可早于当前
  endTime: string                    // 必填。LEAVE/OVERTIME 不可早于当前
  durationHours: number              // 必填，时长（小时）
  ccUserIds?: number[]               // 抄送人 ID 列表，可选
  attachmentIds?: number[]           // 附件 ID 列表（先上传附件再提交），可选
}
```

> 审批人由服务端根据 `sys_user.manager_id` 自动确定，前端无需传递 `approverId`。如需自定义审批人，联系后端添加该字段。

#### FlowApplyUpdateDTO — 编辑申请请求体

```ts
interface FlowApplyUpdateDTO {
  title: string                      // 必填
  reason: string                     // 必填
  startTime: string                  // 必填，yyyy-MM-dd HH:mm:ss
  endTime: string                    // 必填
  durationHours: number              // 必填，时长（小时）
  attachmentIds?: number[]           // 新的附件 ID 列表（传入后替换全部附件，传 [] 清空）
}
```

#### FlowApproveDTO — 审批通过请求体

```ts
interface FlowApproveDTO {
  comment?: string                   // 审批意见，可选
}
```

#### FlowRejectDTO — 审批驳回请求体

```ts
interface FlowRejectDTO {
  comment: string                    // 必填，驳回意见
}
```

#### 申请列表查询参数（FlowApplyQueryDTO）

```ts
interface FlowApplyQueryParams {
  pageNum?: number                   // 默认 1
  pageSize?: number                  // 默认 10
  applyType?: string                 // LEAVE | OVERTIME | CORRECTION
  status?: string                    // PENDING | APPROVED | REJECTED | CANCELED
  startDate?: string                 // yyyy-MM-dd
  endDate?: string                   // yyyy-MM-dd
}
```

---

## 四、审批状态机

```
PENDING ─┬─ approveApply()  ──→ APPROVED
         ├─ rejectApply()   ──→ REJECTED
         ├─ cancelApply()   ──→ CANCELED
         └─ 超时自动驳回     ──→ REJECTED (approverId=0，系统操作)
```

- **PENDING**（待审批）：初始状态，可以撤销、编辑或审批
- **APPROVED**（已通过）：终态，不可再操作
- **REJECTED**（已驳回）：终态（含人工驳回和超时自动驳回）
- **CANCELED**（已撤销）：终态，不可再操作

只有状态为 `PENDING` 的申请才可以撤销、编辑、通过或驳回。

审批记录的 `action` 字段值为 `SUBMIT`（提交）、`APPROVE`（通过）、`REJECT`（驳回）、`CANCEL`（撤销）、`AUTO_REJECT`（超时自动驳回），按 `approvedAt` 时间排序组成时间线。

### 附件上传与绑定流程

```
1. 用户上传附件
   POST /api/flow/attachments/upload (multipart/form-data)
   → 返回 AttachmentVO { id, fileName, fileUrl, ... }

2. 提交/编辑申请时传入附件 ID
   POST /api/flow/applies  body: { ..., attachmentIds: [1, 2] }
   PUT  /api/flow/applies/{id}  body: { ..., attachmentIds: [1, 3] }

3. 查看详情时附件随审批单一起返回
   GET /api/flow/applies/{id} → FlowApplyDetailVO.attachments
```

前端需实现两阶段上传：
- 先在表单中调用上传接口，拿到附件 ID 列表并展示缩略图
- 提交表单时将 `attachmentIds` 一起发送
- 编辑时传入新的 `attachmentIds` 会替换全部附件，传 `[]` 清空

---

## 五、前端页面路由建议

```ts
// 建议路由结构
const routes = [
  // ===== 公告 =====
  {
    path: '/notice',
    name: 'Notice',
    component: () => import('@/views/notice/NoticeListView.vue'),
    meta: { title: '公告通知', icon: 'Bell' }
  },
  {
    path: '/notice/:id',
    name: 'NoticeDetail',
    component: () => import('@/views/notice/NoticeDetailView.vue'),
    meta: { title: '公告详情', hidden: true }
  },
  {
    path: '/notice/admin',
    name: 'NoticeAdmin',
    component: () => import('@/views/notice/NoticeAdminView.vue'),
    meta: { title: '公告管理', icon: 'Bell', roles: ['ADMIN', 'MANAGER'] }
  },
  {
    path: '/notice/admin/create',
    name: 'NoticeCreate',
    component: () => import('@/views/notice/NoticeFormView.vue'),
    meta: { title: '创建公告', hidden: true, roles: ['ADMIN', 'MANAGER'] }
  },
  {
    path: '/notice/admin/:id/edit',
    name: 'NoticeEdit',
    component: () => import('@/views/notice/NoticeFormView.vue'),
    meta: { title: '编辑公告', hidden: true, roles: ['ADMIN', 'MANAGER'] }
  },
  {
    path: '/notice/admin/:id/read-detail',
    name: 'NoticeReadDetail',
    component: () => import('@/views/notice/NoticeReadDetailView.vue'),
    meta: { title: '阅读详情', hidden: true, roles: ['ADMIN', 'MANAGER'] }
  },

  // ===== 审批（员工端）=====
  {
    path: '/flow',
    name: 'Flow',
    component: () => import('@/views/flow/FlowView.vue'),
    redirect: '/flow/my',
    children: [
      { path: 'my', name: 'FlowMy', component: () => import('@/views/flow/FlowMyView.vue'), meta: { title: '我的申请' } },
      { path: 'create', name: 'FlowCreate', component: () => import('@/views/flow/FlowCreateView.vue'), meta: { title: '提交申请' } },
      { path: ':id', name: 'FlowDetail', component: () => import('@/views/flow/FlowDetailView.vue'), meta: { title: '申请详情', hidden: true } },
      { path: ':id/edit', name: 'FlowEdit', component: () => import('@/views/flow/FlowCreateView.vue'), meta: { title: '编辑申请', hidden: true } },
      { path: 'pending', name: 'FlowPending', component: () => import('@/views/flow/FlowPendingView.vue'), meta: { title: '待我审批' } },
      { path: 'processed', name: 'FlowProcessed', component: () => import('@/views/flow/FlowProcessedView.vue'), meta: { title: '我已审批' } }
    ],
    meta: { title: '审批中心', icon: 'Tickets' }
  },

  // ===== 审批（管理端）=====
  {
    path: '/flow/admin',
    name: 'FlowAdmin',
    component: () => import('@/views/flow/FlowAdminView.vue'),
    meta: { title: '审批管理', icon: 'Tickets', roles: ['ADMIN', 'MANAGER'] },
    children: [
      { path: 'approved', name: 'FlowApproved', component: () => import('@/views/flow/FlowApprovedView.vue'), meta: { title: '所有已审批' } }
    ]
  }
];
```

**路由要点：**
- 待审批 `/flow/pending` 和已审批 `/flow/processed` 放在员工端，所有登录用户可访问（按自己 ID 过滤）
- 管理端 `/flow/admin` 仅 ADMIN/MANAGER 可见，目前只放"所有已审批"列表
- 审批操作（通过/驳回）按钮放在待审批列表和申请详情页中，不单独占路由
- 编辑申请路由 `:id/edit` 复用创建申请的组件，传入已有数据做回填

---

## 六、Axios 封装示例

```ts
// utils/request.ts
import axios from 'axios';
import { ElMessage } from 'element-plus';

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

// 请求拦截器 — 附加 Token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器 — 统一处理
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data;
    if (code === 200) {
      return data;  // 直接返回 data，调用方无需 .data
    }
    ElMessage.error(message || '请求失败');
    return Promise.reject(new Error(message));
  },
  (error) => {
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录');
      // 跳转登录页
    } else {
      ElMessage.error('网络异常');
    }
    return Promise.reject(error);
  }
);

export default request;
```

---

## 七、API 调用示例

```ts
// api/notice.ts
import request from '@/utils/request';

// 公告列表（用户端）
export const getNoticeList = (params: NoticeQueryParams) =>
  request.get('/notice/notices', { params });

// 公告详情
export const getNoticeDetail = (id: number) =>
  request.get(`/notice/notices/${id}`);

// 公告预览（不标记已读）
export const previewNotice = (id: number) =>
  request.get(`/notice/notices/${id}/preview`);

// 标记已读/未读
export const markRead = (id: number, readStatus: number) =>
  request.post(`/notice/notices/${id}/read-status`, { readStatus });

// 批量已读
export const batchRead = (noticeIds: number[]) =>
  request.post('/notice/notices/batch-read', { noticeIds });

// 未读统计
export const getUnreadCount = (params?: { noticeType?: string; priority?: string }) =>
  request.get('/notice/notices/unread-count', { params });

// --- 管理端 ---

// 创建公告
export const createNotice = (data: NoticeCreateDTO) =>
  request.post('/notice/admin/notices', data);

// 更新公告
export const updateNotice = (id: number, data: NoticeUpdateDTO) =>
  request.put(`/notice/admin/notices/${id}`, data);

// 发布公告
export const publishNotice = (id: number) =>
  request.post(`/notice/admin/notices/${id}/publish`);

// 下线公告
export const offlineNotice = (id: number) =>
  request.post(`/notice/admin/notices/${id}/offline`);

// 删除公告
export const deleteNotice = (id: number) =>
  request.delete(`/notice/admin/notices/${id}`);

// 管理端公告列表
export const getAdminNoticeList = (params: NoticeQueryParams) =>
  request.get('/notice/admin/notices', { params });

// 阅读详情
export const getNoticeReadDetail = (id: number) =>
  request.get(`/notice/admin/notices/${id}/read-details`);

// 上传附件
export const uploadNoticeAttachment = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/notice/admin/attachments/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

// 删除附件
export const deleteNoticeAttachment = (id: number) =>
  request.delete(`/notice/admin/attachments/${id}`);

// 获取附件URL
export const getNoticeAttachmentUrl = (id: number) =>
  request.get(`/notice/admin/attachments/${id}`);

// --- flow ---

// api/flow.ts
import request from '@/utils/request';

// 提交申请
export const createApply = (data: FlowApplyCreateDTO) =>
  request.post('/flow/applies', data);

// 我的申请列表
export const getMyApplies = (params: FlowApplyQueryParams) =>
  request.get('/flow/applies/my', { params });

// 申请详情
export const getApplyDetail = (id: number) =>
  request.get(`/flow/applies/${id}`);

// 编辑申请
export const updateApply = (id: number, data: FlowApplyUpdateDTO) =>
  request.put(`/flow/applies/${id}`, data);

// 撤销申请
export const cancelApply = (id: number) =>
  request.put(`/flow/applies/${id}/cancel`);

// 删除申请
export const deleteApply = (id: number) =>
  request.delete(`/flow/applies/${id}`);

// 我的待审批列表
export const getPendingApplies = (params: FlowApplyQueryParams) =>
  request.get('/flow/applies/pending', { params });

// 我的已审批列表
export const getProcessedApplies = (params: FlowApplyQueryParams) =>
  request.get('/flow/applies/processed', { params });

// 上传附件
export const uploadAttachment = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/flow/attachments/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

// 删除附件
export const deleteAttachment = (id: number) =>
  request.delete(`/flow/attachments/${id}`);

// --- 管理端 ---

// 所有已审批申请
export const getAllApprovedApplies = (params: FlowApplyQueryParams) =>
  request.get('/flow/admin/applies/approved', { params });

// 审批通过
export const approveApply = (id: number, data: FlowApproveDTO) =>
  request.post(`/flow/admin/applies/${id}/approve`, data);

// 审批驳回
export const rejectApply = (id: number, data: FlowRejectDTO) =>
  request.post(`/flow/admin/applies/${id}/reject`, data);
```

---

## 八、AI 开发提示词模板

以下提示词可直接用于 AI 辅助工具生成前端页面代码：

### 公告列表页

```
请为 OfficeFlow OA 系统创建公告列表页面（NoticeListView.vue），使用 Vue 3 Composition API + Element Plus。

功能要求：
1. 分页列表展示：标题、类型标签、优先级标签、发布人、发布时间、阅读状态
2. 搜索筛选：关键词搜索框、类型下拉（全部/公司/部门/系统）、优先级下拉
3. 未读公告用醒目样式标注（加粗标题 + 红色圆点角标）
4. 点击行进入详情页
5. 页面顶部显示未读统计角标

数据结构使用：
- 响应：ApiResponse<PageResult<NoticeListVO>>，调用 GET /api/notice/notices
- NoticeListVO: id, title, summary, noticeType, priority, publisherId, publisherName, publishTime, expireTime, readStatus, readAt

请给出完整 .vue 文件。
```

### 公告详情页

```
请为 OfficeFlow OA 系统创建公告详情页（NoticeDetailView.vue），使用 Vue 3 Composition API + Element Plus。

功能要求：
1. 显示公告标题、发布人、发布时间、类型标签、优先级标签
2. 富文本内容渲染（v-html），注意 XSS 防护
3. 附件列表展示：文件名 + 文件大小 + 下载链接（直接使用 fileUrl），无附件时隐藏此区域
4. 底部显示阅读统计（浏览次数、已读人数）
5. 进入页面自动调用详情接口（触发标记已读）
6. 返回按钮

数据结构使用：
- 响应：ApiResponse<NoticeDetailVO>，调用 GET /api/notice/notices/{id}
- NoticeDetailVO: id, title, content, noticeType, priority, publisherId, publisherName, publishTime, expireTime, viewCount, readCount, readStatus, readAt, createdAt, attachments[]
- AttachmentVO: id, fileName, fileUrl, fileSize, fileType

请给出完整 .vue 文件。
```

### 公告管理列表页（管理员）

```
请为 OfficeFlow OA 系统创建公告管理列表页（NoticeAdminView.vue），使用 Vue 3 Composition API + Element Plus。
此页面仅 ADMIN 或 MANAGER 角色可见（路由 meta.roles 控制）。

功能要求：
1. 分页列表：标题、类型标签、优先级标签、状态标签、发布人、发布时间、阅读率（进度条）、已读/浏览数
2. 状态用不同颜色标签：DRAFT=info, PUBLISHED=success, OFFLINE=warning
3. 筛选：状态（全部/草稿/已发布/已下线）、类型、优先级、发布时间范围、关键词搜索
4. 每行操作按钮：
   - 草稿/已下线：编辑、发布、删除（需确认）
   - 已发布：查看阅读详情、下线（需确认）
5. 顶部"创建公告"按钮跳转创建页
6. 点击行进入预览页（调用 preview 接口，不标记已读）

数据结构使用：
- 列表响应：ApiResponse<PageResult<AdminNoticeListVO>>，调用 GET /api/notice/admin/notices
- AdminNoticeListVO: id, title, noticeType, priority, publisherId, publisherName, publishTime, status, readCount, viewCount, readRate, createdAt
- 发布：POST /api/notice/admin/notices/{id}/publish
- 下线：POST /api/notice/admin/notices/{id}/offline
- 删除：DELETE /api/notice/admin/notices/{id}

请给出完整 .vue 文件。
```

### 公告创建/编辑表单页（管理员）

```
请为 OfficeFlow OA 系统创建公告创建/编辑表单页（NoticeFormView.vue），使用 Vue 3 Composition API + Element Plus。
组件需兼容创建和编辑两种模式（通过路由参数判断：有 :id 为编辑，无则为创建）。

功能要求：
1. 表单字段：
   - 标题（el-input，必填，最长128）
   - 类型（el-select，必填：公司公告/部门公告/系统公告）
   - 优先级（el-radio-group，必填：普通/重要/紧急，用颜色区分）
   - 内容（富文本编辑器，必填。推荐使用 @wangeditor/editor 或类似的 Vue 3 富文本组件）
   - 定时发布开关（el-switch）+ 日期时间选择器（el-date-picker，精确到分钟）
     提示：开启后公告将在指定时间自动发布，不需要手动点"发布"
   - 过期时间（el-date-picker，可选）
   - 可见范围（动态列表，每行：scopeType 下拉 + scopeId 选择器）：
     - 至少保留一个"ALL"范围
     - 支持添加 DEPT（选部门）、USER（选用户）、ROLE（选角色）
     - 调用户服务接口获取部门和用户列表
2. 附件上传区域：
   - 上传按钮，调用 POST /api/notice/admin/attachments/upload
   - 已上传附件列表：文件名、大小、删除按钮（DELETE /api/notice/admin/attachments/{id}）
   - 编辑模式加载已有附件列表
3. 创建模式：提交成功跳转到管理列表
4. 编辑模式：进入时调 GET /api/notice/notices/{id}/preview 获取已有数据回填，提交成功返回管理列表
5. 底部按钮：保存草稿（创建/更新接口），预览按钮（新窗口打开预览）

数据结构使用：
- 创建：POST /api/notice/admin/notices，请求体 NoticeCreateDTO
- 更新：PUT /api/notice/admin/notices/{id}，请求体 NoticeUpdateDTO（extends NoticeCreateDTO + id）
- NoticeCreateDTO: title, content, noticeType, priority, scheduledTime?, expireTime?, scopes[], attachmentIds[]
- NoticeScopeDTO: scopeType (ALL|DEPT|USER|ROLE), scopeId?
- 附件上传：POST /api/notice/admin/attachments/upload (multipart/form-data, field: file) → AttachmentVO
- 附件删除：DELETE /api/notice/admin/attachments/{id}

编辑时 scopes 和 attachmentIds 都是全量替换（传 [] 清空）。

请给出完整 .vue 文件。
```

### 阅读详情统计页

```
请为 OfficeFlow OA 系统创建公告阅读详情统计页（NoticeReadDetailView.vue），使用 Vue 3 Composition API + Element Plus。

功能要求：
1. 顶部：公告标题、总览统计卡片（3个卡片：总人数、已读人数、未读人数，各配图标和颜色）
2. 整体阅读率进度条（el-progress，带百分比显示）
3. 部门阅读统计表格：
   - 列：部门名称、总人数、已读人数、未读人数、阅读率（进度条 + 百分比）
   - 按阅读率降序排列
   - 阅读率 < 50% 的行用警告色高亮
4. 刷新按钮

数据结构使用：
- 响应：ApiResponse<NoticeReadDetailVO>，调用 GET /api/notice/admin/notices/{id}/read-details
- NoticeReadDetailVO: noticeId, totalUsers, readUsers, unreadUsers, readRate, deptStats[]
- DeptStatVO: deptId, deptName, totalUsers, readUsers, unreadUsers, readRate

请给出完整 .vue 文件。
```

### 我的申请列表页

```
请为 OfficeFlow OA 系统创建"我的申请"列表页（FlowMyView.vue），使用 Vue 3 Composition API + Element Plus。

功能要求：
1. 分页列表展示：申请单号、标题、类型、时长、审批人、状态标签、提交时间
2. 类型筛选（全部/请假/加班/补卡）和状态筛选（全部/待审批/已通过/已驳回/已撤销）
3. 状态用不同颜色标签：PENDING=warning, APPROVED=success, REJECTED=danger, CANCELED=info
4. 点击行进入详情页
5. 顶部"提交申请"按钮跳转创建页
6. 每条记录右侧下拉菜单：查看详情 / 编辑（仅PENDING）/ 撤销（仅PENDING） / 删除

数据结构使用：
- 响应：ApiResponse<PageResult<FlowApplyListVO>>，调用 GET /api/flow/applies/my
- FlowApplyListVO: id, applyNo, applyType, title, reason, durationHours, status, approverName, startTime, endTime, createdAt
- 编辑：PUT /api/flow/applies/{id}，请求体 FlowApplyUpdateDTO
- 撤销：PUT /api/flow/applies/{id}/cancel
- 删除：DELETE /api/flow/applies/{id}

请给出完整 .vue 文件。
```

### 提交申请页（兼容编辑）

```
请为 OfficeFlow OA 系统创建提交/编辑申请页（FlowCreateView.vue），使用 Vue 3 Composition API + Element Plus。
组件需兼容创建和编辑两种模式（通过路由参数判断：有 :id 为编辑，无则为创建）。

功能要求：
1. 表单：申请类型（下拉：请假/加班/补卡，编辑时不可改）、标题、原因（多行文本）、时长（数字输入）、开始/结束时间（日期时间选择器）、抄送人选择（多选，调用户接口）
2. 附件上传区域：
   - 上传按钮，调用 POST /api/flow/attachments/upload
   - 已上传附件列表：显示文件名、大小、删除按钮（调 DELETE /api/flow/attachments/{id}）
   - 编辑模式下加载已有附件列表
3. 前端校验：标题必填(最长128)、原因必填(最长500)、时长必填且>0、开始结束必填且结束>开始、请假/加班时间不可早于当前
4. 创建模式：提交成功跳转到我的申请列表
5. 编辑模式：进入时调 GET /api/flow/applies/{id} 获取已有数据回填，提交成功返回详情页
6. 补卡类型时开始/结束时间可早于当前（补历史卡）

数据结构使用：
- 创建：POST /api/flow/applies，请求体 FlowApplyCreateDTO = { applyType, title, reason, startTime, endTime, durationHours, ccUserIds?, attachmentIds? }
- 编辑：PUT /api/flow/applies/{id}，请求体 FlowApplyUpdateDTO = { title, reason, startTime, endTime, durationHours, attachmentIds? }
- 附件上传：POST /api/flow/attachments/upload (multipart/form-data, field: file)
- 附件删除：DELETE /api/flow/attachments/{id}

审批人由服务端自动确定（直属领导），前端无需选择。

请给出完整 .vue 文件。
```

### 申请详情页

```
请为 OfficeFlow OA 系统创建申请详情页（FlowDetailView.vue），使用 Vue 3 Composition API + Element Plus。

功能要求：
1. 顶部显示申请单号、状态标签、当前节点
2. 信息卡片：申请类型、标题、原因、时长、开始/结束时间、申请人、部门、审批人
3. 审批记录时间线（el-timeline）：每条显示审批人、动作、意见、时间，AUTO_REJECT 用警告色标注
4. 附件列表：文件名 + 下载链接（直接使用 fileUrl），允许点击下载
5. 操作栏（仅 PENDING 状态 + 当前用户是审批人 + 有 ADMIN/MANAGER 角色时显示）：通过按钮 + 驳回按钮
6. 通过：弹出确认框 + 可选意见输入
7. 驳回：弹出对话框，意见必填
8. 底部返回按钮

数据结构使用：
- 响应：ApiResponse<FlowApplyDetailVO>，调用 GET /api/flow/applies/{id}
- FlowApplyDetailVO: id, applyNo, ..., approveRecords[], attachments[]
- 通过：POST /api/flow/admin/applies/{id}/approve
- 驳回：POST /api/flow/admin/applies/{id}/reject

请给出完整 .vue 文件。
```

### 待我审批页

```
请为 OfficeFlow OA 系统创建"待我审批"列表页（FlowPendingView.vue），使用 Vue 3 Composition API + Element Plus。

功能要求：
1. 分页列表展示：申请单号、类型标签、标题、申请人、部门、开始/结束时间、提交时间
2. 类型筛选（全部/请假/加班/补卡）
3. 每行提供"通过"和"驳回"操作按钮
4. 通过：弹出确认框（el-message-box confirm）+ 可选意见（el-input textarea），调用 POST /api/flow/admin/applies/{id}/approve
5. 驳回：弹出对话框（el-dialog），意见必填（el-input textarea + 校验），调用 POST /api/flow/admin/applies/{id}/reject
6. 点击行跳转详情页
7. 操作后刷新列表

数据结构使用：
- 列表响应：ApiResponse<PageResult<FlowPendingVO>>，调用 GET /api/flow/applies/pending
- FlowPendingVO: id, applyNo, applyType, title, applicantName, applicantDeptName, startTime, endTime, createdAt
- 通过：POST /api/flow/admin/applies/{id}/approve，请求体 { comment?: string }
- 驳回：POST /api/flow/admin/applies/{id}/reject，请求体 { comment: string }（必填）

注意：此页面在员工端路由下（/flow/pending），所有登录用户可访问。通过/驳回按钮需要 ADMIN 或 MANAGER 角色才显示（后端也会拦截）。

请给出完整 .vue 文件。
```

### 我已审批页

```
请为 OfficeFlow OA 系统创建"我已审批"列表页（FlowProcessedView.vue），使用 Vue 3 Composition API + Element Plus。

功能要求：
1. 分页列表展示：申请单号、类型标签、标题、申请人、最终状态、我的审批动作（APPROVE/REJECT）、审批时间
2. 类型筛选（全部/请假/加班/补卡）
3. 状态用不同颜色标签：APPROVED=success, REJECTED=danger
4. 我的审批动作用标签区分：APPROVE 绿色"已通过"，REJECT 红色"已驳回"
5. 点击行跳转详情页

数据结构使用：
- 响应：ApiResponse<PageResult<FlowProcessedVO>>，调用 GET /api/flow/applies/processed
- FlowProcessedVO: id, applyNo, applyType, title, status, applicantName, myAction, approvedAt, createdAt

请给出完整 .vue 文件。
```

### 所有已审批页（管理端）

```
请为 OfficeFlow OA 系统创建"所有已审批"列表页（FlowApprovedView.vue），使用 Vue 3 Composition API + Element Plus。
此页面仅 ADMIN 或 MANAGER 角色可见（路由 meta.roles 控制）。

功能要求：
1. 分页列表展示：申请单号、类型标签、标题、原因摘要、时长、申请人、部门、审批人、审批时间
2. 类型筛选（全部/请假/加班/补卡）和审批日期范围筛选
3. 点击行跳转详情页
4. 可导出 Excel（可选，后续实现）

数据结构使用：
- 响应：ApiResponse<PageResult<FlowApprovedVO>>，调用 GET /api/flow/admin/applies/approved
- FlowApprovedVO: id, applyNo, applyType, title, reason, durationHours, applicantName, applicantDeptName, approverName, startTime, endTime, approvedAt, createdAt

请给出完整 .vue 文件。
```

---

## 九、测试账号

| 账号 | 密码 | 角色 | 直属领导 |
|---|---|---|---|
| `admin` | `123456` | 系统管理员 | 无 |
| `manager` | `123456` | 部门主管 | admin |
| `employee` | `123456` | 普通员工 | manager |
| `hr` | `123456` | 人事专员 | admin |

测试建议：
- 用 `employee` 登录提交请假/加班申请，审批人选 `manager`（ID=2）
- 用 `manager` 登录查看待办并审批
- 用 `admin` 登录发布公告
- 用 `employee` 登录查看公告并标记已读
