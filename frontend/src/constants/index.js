// ===================== 公告模块常量 =====================
export const NOTICE_TYPE_OPTIONS = [
  { value: 'COMPANY', label: '公司公告' },
  { value: 'DEPT', label: '部门公告' }
]

export const NOTICE_PRIORITY_OPTIONS = [
  { value: 'URGENT', label: '紧急' },
  { value: 'HIGH', label: '重要' },
  { value: 'NORMAL', label: '普通' },
  { value: 'LOW', label: '低' }
]

export const NOTICE_STATUS_OPTIONS = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'OFFLINE', label: '已下线' },
  { value: 'REVIEWING', label: '待审核' },
  { value: 'REJECTED', label: '已驳回' }
]

export const NOTICE_SCOPE_TYPE_OPTIONS = [
  { value: 'ALL', label: '全员' },
  { value: 'DEPT', label: '按部门' },
  { value: 'USER', label: '按人员' }
]

export const READ_STATUS_OPTIONS = [
  { value: 0, label: '未读' },
  { value: 1, label: '已读' }
]

// ===================== 审批模块常量 =====================
export const APPLY_TYPE_OPTIONS = [
  { value: 'LEAVE', label: '请假' },
  { value: 'OVERTIME', label: '加班' },
  { value: 'BUSINESS_TRIP', label: '出差' },
  { value: 'CORRECTION', label: '补卡' },
  { value: 'PURCHASE', label: '采购' }
]

export const APPLY_STATUS_OPTIONS = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING', label: '待审批' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'CANCELLED', label: '已撤销' }
]

// ===================== 标签/文本映射 =====================
const noticeTypeMap = Object.fromEntries(NOTICE_TYPE_OPTIONS.map(o => [o.value, o.label]))
const noticePriorityMap = Object.fromEntries(NOTICE_PRIORITY_OPTIONS.map(o => [o.value, o.label]))
const noticeStatusMap = Object.fromEntries(NOTICE_STATUS_OPTIONS.map(o => [o.value, o.label]))
const applyTypeMap = Object.fromEntries(APPLY_TYPE_OPTIONS.map(o => [o.value, o.label]))
const applyStatusMap = Object.fromEntries(APPLY_STATUS_OPTIONS.map(o => [o.value, o.label]))

export const getNoticeTypeLabel = (v) => noticeTypeMap[v] || v || '-'
export const getNoticePriorityLabel = (v) => noticePriorityMap[v] || v || '-'
export const getNoticeStatusLabel = (v) => noticeStatusMap[v] || v || '-'
export const getApplyTypeLabel = (v) => applyTypeMap[v] || v || '-'
export const getApplyStatusLabel = (v) => applyStatusMap[v] || v || '-'

export const getNoticePriorityTag = (v) =>
  ({ URGENT: 'danger', HIGH: 'warning', NORMAL: '', LOW: 'info' })[v] || ''
export const getNoticeStatusTag = (v) =>
  ({ DRAFT: 'info', PUBLISHED: 'success', OFFLINE: 'info', REVIEWING: 'warning', REJECTED: 'danger' })[v] || ''
export const getApplyStatusTag = (v) =>
  ({ DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' })[v] || ''
