<template>
  <div class="notice-admin">
    <!-- 搜索栏 -->
    <div class="admin-toolbar">
      <div class="admin-search-card">
        <div class="admin-search-main">
          <div class="admin-search-input-wrap">
            <el-icon class="admin-search-icon"><Search /></el-icon>
            <input
              v-model="query.keyword"
              class="admin-search-input"
              placeholder="搜索公告标题..."
              @keyup.enter="handleSearch"
            />
          </div>
          <el-select v-model="query.status" placeholder="全部状态" clearable>
            <el-option v-for="o in NOTICE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-date-picker v-model="query.dateRange" type="daterange" range-separator="—" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
        <el-button class="create-btn" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          创建公告
        </el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" class="admin-table" stripe>
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="80">
        <template #default="{ row }"><el-tag size="small">{{ getNoticeTypeLabel(row.noticeType) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="优先级" width="80">
        <template #default="{ row }">
          <el-tag :type="getNoticePriorityTag(row.priority)" size="small">{{ getNoticePriorityLabel(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="getNoticeStatusTag(row.status)" size="small">{{ getNoticeStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="定时发布" width="150">
        <template #default="{ row }">{{ row.scheduledTime || '—' }}</template>
      </el-table-column>
      <el-table-column prop="publisherName" label="发布人" width="100" />
      <el-table-column prop="publishTime" label="发布时间" width="160" />
      <el-table-column label="阅读率" width="80">
        <template #default="{ row }">{{ formatReadRate(row.readRate) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)" v-if="row.status === 'DRAFT'">编辑</el-button>
          <el-button link type="warning" size="small" @click="handleOffline(row)" v-if="row.status === 'PUBLISHED'">下线</el-button>
          <el-button link type="primary" size="small" @click="openReadDetail(row)">阅读详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNum"
      @current-change="handlePageChange"
    />

    <div v-if="!loading && list.length === 0" class="admin-empty">
      <el-empty description="暂无公告数据" />
    </div>

    <!-- 新建 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '创建公告'" width="720px" class="admin-form-dialog" destroy-on-close @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px" class="admin-form">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" maxlength="128" show-word-limit placeholder="请输入标题（最多128字）" />
        </el-form-item>
        <el-form-item label="公告类型" prop="noticeType">
          <el-radio-group v-model="form.noticeType">
            <el-radio-button v-for="o in NOTICE_TYPE_OPTIONS" :key="o.value" :label="o.value">{{ o.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio-button v-for="o in NOTICE_PRIORITY_OPTIONS" :key="o.value" :label="o.value">{{ o.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="定时发布">
          <el-date-picker
            v-model="form.scheduledTime"
            type="datetime"
            placeholder="留空则保存为草稿，需手动发布"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:260px"
          />
          <span class="form-tip">设置未来时间，系统将在指定时间自动发布；留空则需手动点击发布</span>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="选择过期时间（可选）"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:260px"
          />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入公告内容（支持 HTML 富文本）" />
          <div class="form-hint">提示：内容支持 HTML 标签，如 &lt;b&gt;加粗&lt;/b&gt;、&lt;p&gt;段落&lt;/p&gt; 等</div>
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            :http-request="customUpload"
            :file-list="fileList"
            :on-remove="handleRemove"
            list-type="text"
            multiple
          >
            <el-button size="small"><el-icon><Upload /></el-icon> 上传附件</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="可见范围">
          <div class="scope-editor">
            <div v-for="(s, i) in form.scopes" :key="i" class="scope-row">
              <el-select v-model="s.scopeType" style="width:140px" @change="onScopeTypeChange(s)">
                <el-option v-for="o in NOTICE_SCOPE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <template v-if="s.scopeType === 'ALL'">
                <span class="scope-all-hint">所有员工可见</span>
              </template>
              <template v-else-if="s.scopeType === 'DEPT'">
                <el-select v-model="s.scopeId" filterable placeholder="请搜索并选择部门" style="width:280px" @change="(val) => onScopeValueSelect(s, 'DEPT', val)">
                  <el-option v-for="d in deptFlatOptions" :key="d.value" :label="d.label" :value="d.value" />
                </el-select>
              </template>
              <template v-else-if="s.scopeType === 'USER'">
                <el-select v-model="s.scopeId" filterable placeholder="请搜索并选择人员" style="width:280px" @change="(val) => onScopeValueSelect(s, 'USER', val)">
                  <el-option v-for="u in userOptions" :key="u.value" :label="u.label" :value="u.value" />
                </el-select>
              </template>
              <template v-else-if="s.scopeType === 'ROLE'">
                <el-select v-model="s.scopeId" filterable placeholder="请搜索并选择角色" style="width:280px" @change="(val) => onScopeValueSelect(s, 'ROLE', val)">
                  <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
                </el-select>
              </template>
              <el-button size="small" type="danger" text @click="removeScope(i)">删除</el-button>
            </div>
            <el-button link type="primary" size="small" @click="addScope">+ 添加范围</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存为草稿</el-button>
        <el-button type="success" :loading="submitting" @click="submitAndPublish">保存并发布</el-button>
      </template>
    </el-dialog>

    <!-- 阅读详情弹窗 -->
    <el-dialog v-model="statsVisible" title="阅读详情" width="620px" destroy-on-close>
      <template v-if="stats">
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-card-icon" style="background:#eff6ff;color:#3b82f6;">
              <el-icon size="22"><User /></el-icon>
            </div>
            <div class="stat-card-body">
              <span class="stat-card-value">{{ stats.totalUsers || 0 }}</span>
              <span class="stat-card-label">应读人数</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-card-icon" style="background:#f0fdf4;color:#22c55e;">
              <el-icon size="22"><Check /></el-icon>
            </div>
            <div class="stat-card-body">
              <span class="stat-card-value">{{ stats.readUsers || 0 }}</span>
              <span class="stat-card-label">已读人数</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-card-icon" style="background:#fef3c7;color:#f59e0b;">
              <el-icon size="22"><Clock /></el-icon>
            </div>
            <div class="stat-card-body">
              <span class="stat-card-value">{{ stats.unreadUsers || 0 }}</span>
              <span class="stat-card-label">未读人数</span>
            </div>
          </div>
          <div class="stat-card stat-card-accent">
            <div class="stat-card-icon" style="background:#fef2f2;color:#ef4444;">
              <el-icon size="22"><DataAnalysis /></el-icon>
            </div>
            <div class="stat-card-body">
              <span class="stat-card-value highlight">{{ formatReadRate(stats.readRate) }}</span>
              <span class="stat-card-label">阅读率</span>
            </div>
          </div>
        </div>
        <div class="stats-section-title">
          <span>部门阅读明细</span>
        </div>
        <el-table :data="stats.deptStats || []" class="stats-table" stripe>
          <el-table-column prop="deptName" label="部门" min-width="130" />
          <el-table-column prop="totalUsers" label="应读" width="80" align="center" />
          <el-table-column prop="readUsers" label="已读" width="80" align="center" />
          <el-table-column prop="unreadUsers" label="未读" width="80" align="center" />
          <el-table-column label="阅读率" width="110" align="center">
            <template #default="{ row }">
              <el-progress :percentage="parseReadRate(row.readRate)" :stroke-width="8" :show-text="true" :color="getReadRateColor(row.readRate)" />
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminNoticeListApi,
  searchAdminNoticeApi,
  createNoticeApi,
  updateNoticeApi,
  publishNoticeApi,
  offlineNoticeApi,
  deleteNoticeApi,
  getNoticeDetailApi,
  getNoticeReadDetailApi,
  uploadNoticeAttachmentApi,
  deleteNoticeAttachmentApi
} from '@/api/notice'
import { getDeptTreeApi, getUserOptionsApi, getRoleListApi } from '@/api/user'
import {
  NOTICE_TYPE_OPTIONS,
  NOTICE_PRIORITY_OPTIONS,
  NOTICE_STATUS_OPTIONS,
  NOTICE_SCOPE_TYPE_OPTIONS,
  getNoticeTypeLabel,
  getNoticePriorityLabel,
  getNoticeStatusLabel,
  getNoticePriorityTag,
  getNoticeStatusTag
} from '@/constants'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', dateRange: null, pageNum: 1, pageSize: 10 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  title: '',
  noticeType: 'COMPANY',
  priority: 'NORMAL',
  content: '',
  scheduledTime: '',
  expireTime: '',
  scopes: [{ scopeType: 'ALL', scopeId: null, scopeName: '全员' }],
  attachmentIds: []
})
const fileList = ref([])
const deptFlatOptions = ref([])
const userOptions = ref([])
const roleOptions = ref([])
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }]
}

const statsVisible = ref(false)
const stats = ref(null)

const loadList = async () => {
  loading.value = true
  try {
    const hasKeyword = !!query.keyword
    if (hasKeyword) {
      // ES 搜索：keyword + status + 分页
      const params = {
        keyword: query.keyword,
        status: query.status || undefined,
        pageNum: query.pageNum,
        pageSize: query.pageSize
      }
      const res = await searchAdminNoticeApi(params)
      if (res.code === 200 && res.data) {
        list.value = res.data.records || []
        total.value = res.data.total || 0
      }
    } else {
      // MySQL 列表：支持日期范围等扩展过滤
      const params = { ...query, onlyPublished: false }
      if (query.dateRange && query.dateRange.length === 2) {
        params.startDate = query.dateRange[0] + ' 00:00:00'
        params.endDate = query.dateRange[1] + ' 23:59:59'
      }
      delete params.dateRange
      const res = await getAdminNoticeListApi(params)
      if (res.code === 200 && res.data) {
        list.value = res.data.records || []
        total.value = res.data.total || 0
      }
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.pageNum = 1
  loadList()
}
const resetQuery = () => {
  Object.assign(query, { keyword: '', status: '', dateRange: null, pageNum: 1 })
  loadList()
}
const handlePageChange = (p) => {
  query.pageNum = p
  loadList()
}

const openCreate = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row) => {
  isEdit.value = true
  resetForm()
  const res = await getNoticeDetailApi(row.id)
  if (res.code === 200 && res.data) {
    const d = res.data
    form.id = d.id
    form.title = d.title
    form.noticeType = d.noticeType
    form.priority = d.priority
    form.content = d.content
    form.scheduledTime = d.scheduledTime || ''
    form.expireTime = d.expireTime || ''
    form.scopes =
      d.scopes && d.scopes.length
        ? d.scopes.map((s) => ({ scopeType: s.scopeType, scopeId: s.scopeId || null, scopeName: s.scopeName || '' }))
        : [{ scopeType: 'ALL', scopeId: null, scopeName: '全员' }]
    form.attachmentIds = (d.attachmentList || []).map((a) => a.id)
    fileList.value = (d.attachmentList || []).map((a) => ({ name: a.fileName, url: a.fileUrl, id: a.id }))
  }
  dialogVisible.value = true
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.noticeType = 'COMPANY'
  form.priority = 'NORMAL'
  form.content = ''
  form.scheduledTime = ''
  form.expireTime = ''
  form.scopes = [{ scopeType: 'ALL', scopeId: null, scopeName: '全员' }]
  form.attachmentIds = []
  fileList.value = []
  formRef.value?.clearValidate?.()
}

const addScope = () => form.scopes.push({ scopeType: 'DEPT', scopeId: null, scopeName: '' })
const removeScope = (i) => form.scopes.splice(i, 1)
const onScopeTypeChange = (s) => {
  s.scopeId = null
  s.scopeName = ''
  if (s.scopeType === 'ALL') {
    s.scopeName = '全员'
  }
}

const flattenDeptTree = (tree, prefix = '') => {
  const result = []
  for (const node of tree) {
    const label = prefix + (node.deptName || node.label || '')
    result.push({ value: node.id, label })
    if (node.children && node.children.length > 0) {
      result.push(...flattenDeptTree(node.children, prefix + '　'))
    }
  }
  return result
}

const onScopeValueSelect = (scope, type, val) => {
  if (!val) { scope.scopeName = ''; return }
  if (type === 'DEPT') {
    const found = deptFlatOptions.value.find(d => d.value === val)
    scope.scopeName = found ? found.label.replace(/　/g, '').trim() : ''
  } else if (type === 'USER') {
    const found = userOptions.value.find(u => u.value === val)
    scope.scopeName = found ? found.label : ''
  } else if (type === 'ROLE') {
    const found = roleOptions.value.find(r => r.value === val)
    scope.scopeName = found ? found.label : ''
  }
}

const loadScopeOptions = async () => {
  try {
    const [deptRes, userRes, roleRes] = await Promise.all([
      getDeptTreeApi(), getUserOptionsApi(), getRoleListApi()
    ])
    deptFlatOptions.value = flattenDeptTree(deptRes.data || [])
    userOptions.value = (userRes.data || []).map(u => ({ value: u.id, label: u.realName || u.username }))
    roleOptions.value = (roleRes.data || []).map(r => ({ value: r.id, label: r.name || r.roleName }))
  } catch (e) {
    // 加载作用域选项失败时静默处理，下拉框为空
  }
}

const customUpload = async ({ file, onSuccess, onError }) => {
  try {
    const res = await uploadNoticeAttachmentApi(file)
    if (res.code === 200 && res.data) {
      file.id = res.data.id
      form.attachmentIds.push(res.data.id)
      onSuccess(res.data)
    } else {
      onError()
    }
  } catch (e) {
    onError(e)
  }
}

const handleRemove = (file) => {
  if (file.id) {
    deleteNoticeAttachmentApi(file.id).catch(() => {})
    const idx = form.attachmentIds.indexOf(file.id)
    if (idx > -1) form.attachmentIds.splice(idx, 1)
  }
}

const buildPayload = () => ({
  id: form.id,
  title: form.title,
  content: form.content,
  noticeType: form.noticeType,
  priority: form.priority,
  scheduledTime: form.scheduledTime || null,
  expireTime: form.expireTime || null,
  scopes: form.scopes,
  attachmentIds: form.attachmentIds
})

const submitForm = () => submit(false)
const submitAndPublish = () => submit(true)

const submit = async (publish) => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      let id = form.id
      if (isEdit.value) {
        await updateNoticeApi(id, buildPayload())
      } else {
        const res = await createNoticeApi(buildPayload())
        id = res.data
      }
      if (publish && id) {
        await publishNoticeApi(id)
      }
      ElMessage.success(publish ? '已发布' : isEdit.value ? '已保存' : '已保存草稿')
      dialogVisible.value = false
      loadList()
    } catch (e) {
      /* 错误已由拦截器提示 */
    } finally {
      submitting.value = false
    }
  })
}

const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm('确认发布该公告？', '提示', { type: 'warning' })
    await publishNoticeApi(row.id)
    ElMessage.success('已发布')
    loadList()
  } catch (e) {}
}
const handleOffline = async (row) => {
  try {
    await ElMessageBox.confirm('确认下线该公告？', '提示', { type: 'warning' })
    await offlineNoticeApi(row.id)
    ElMessage.success('已下线')
    loadList()
  } catch (e) {}
}
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该公告？此操作不可恢复', '提示', { type: 'warning' })
    await deleteNoticeApi(row.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {}
}

const openReadDetail = async (row) => {
  const res = await getNoticeReadDetailApi(row.id)
  if (res.code === 200 && res.data) {
    stats.value = res.data
    statsVisible.value = true
  }
}

const parseReadRate = (rate) => {
  if (rate == null) return 0
  return parseFloat(String(rate).replace('%', '')) || 0
}

const formatReadRate = (rate) => {
  return parseReadRate(rate) + '%'
}

const getReadRateColor = (rate) => {
  const val = parseReadRate(rate)
  if (val >= 80) return '#22c55e'
  if (val >= 50) return '#f59e0b'
  return '#ef4444'
}

onMounted(() => { loadScopeOptions(); loadList() })
</script>

<style scoped>
/* ── 搜索栏 ── */
.admin-toolbar {
  margin-bottom: 20px;
}
.admin-search-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.admin-search-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  flex-wrap: wrap;
  min-width: 0;
}
.admin-search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 200px;
}
.admin-search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 16px;
  z-index: 1;
  pointer-events: none;
}
.admin-search-input {
  width: 100%;
  height: 42px;
  padding: 0 16px 0 40px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  color: #1e293b;
  background: #f8fafc;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}
.admin-search-input::placeholder {
  color: #94a3b8;
}
.admin-search-input:focus {
  border-color: var(--el-color-primary);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(64,158,255,0.06);
}
.admin-search-main .el-select {
  width: 130px;
  flex-shrink: 0;
}
.admin-search-main .el-date-picker {
  width: 230px;
  flex-shrink: 0;
}
.create-btn {
  flex-shrink: 0;
}

/* ── 表格 ── */
.admin-table {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

/* ── 分页 ── */
.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* ── 空状态 ── */
.admin-empty {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

/* ── 表单 ── */
.admin-form-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  padding: 20px 24px 16px;
}
.admin-form-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}
.admin-form-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid #f1f5f9;
  padding: 16px 24px;
}

.scope-editor {
  width: 100%;
}
.scope-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
.form-hint {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

/* ── 阅读统计 ── */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: #f8fafc;
  border-radius: 14px;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
}
.stat-card-accent {
  background: #fef2f2;
}
.stat-card-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-card-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-card-value {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}
.stat-card-value.highlight {
  color: #ef4444;
}
.stat-card-label {
  font-size: 12px;
  color: #94a3b8;
}

.stats-section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}
.stats-section-title::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}
.stats-table {
  border-radius: 10px;
  overflow: hidden;
}
</style>
