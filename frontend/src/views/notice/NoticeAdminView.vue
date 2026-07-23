<template>
  <div class="notice-admin">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" class="search-bar">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="标题/内容" clearable style="width:180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.noticeType" placeholder="全部" clearable style="width:130px">
          <el-option v-for="o in NOTICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:130px">
          <el-option v-for="o in NOTICE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级">
        <el-select v-model="query.priority" placeholder="全部" clearable style="width:120px">
          <el-option v-for="o in NOTICE_PRIORITY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="success" icon="Plus" @click="openCreate">新建公告</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="110">
        <template #default="{ row }"><el-tag>{{ getNoticeTypeLabel(row.noticeType) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="优先级" width="100">
        <template #default="{ row }">
          <el-tag :type="getNoticePriorityTag(row.priority)" size="small">{{ getNoticePriorityLabel(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publisherName" label="发布人" width="110" />
      <el-table-column prop="publishTime" label="发布时间" width="170" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getNoticeStatusTag(row.status)" size="small">{{ getNoticeStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="readRate" label="阅读率" width="90" />
      <el-table-column label="操作" width="330" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openReadDetail(row)">阅读统计</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row)" v-if="row.status === 'DRAFT'">编辑</el-button>
          <el-button link type="success" size="small" @click="handlePublish(row)" v-if="row.status === 'DRAFT'">发布</el-button>
          <el-button link type="warning" size="small" @click="handleOffline(row)" v-if="row.status === 'PUBLISHED'">下线</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNum"
      @current-change="handlePageChange"
    />

    <!-- 新建 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新建公告'" width="760px" destroy-on-close @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型" prop="noticeType">
              <el-select v-model="form.noticeType" style="width:100%">
                <el-option v-for="o in NOTICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="form.priority" style="width:100%">
                <el-option v-for="o in NOTICE_PRIORITY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="定时发布">
          <el-date-picker
            v-model="form.scheduledTime"
            type="datetime"
            placeholder="留空则发布时立即生效"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="支持 HTML，可直接粘贴富文本" />
        </el-form-item>
        <el-form-item label="发布范围">
          <div class="scope-editor">
            <el-button size="small" @click="addScope">+ 添加范围</el-button>
            <div v-for="(s, i) in form.scopes" :key="i" class="scope-row">
              <el-select v-model="s.scopeType" style="width:130px" @change="onScopeTypeChange(s)">
                <el-option v-for="o in NOTICE_SCOPE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <template v-if="s.scopeType === 'ALL'">
                <el-input v-model="s.scopeName" placeholder="全员" disabled style="width:180px" />
              </template>
              <template v-else>
                <el-input v-model="s.scopeValue" placeholder="对象ID" style="width:130px" />
                <el-input v-model="s.scopeName" placeholder="对象名称" style="width:180px" />
              </template>
              <el-button size="small" type="danger" @click="removeScope(i)">删除</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            :http-request="customUpload"
            :file-list="fileList"
            :on-remove="handleRemove"
            list-type="text"
            multiple
          >
            <el-button size="small" type="primary">点击上传</el-button>
            <template #tip><div class="el-upload__tip">上传的附件将在保存/发布时关联</div></template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
        <el-button type="success" v-if="!isEdit" :loading="submitting" @click="submitAndPublish">保存并发布</el-button>
      </template>
    </el-dialog>

    <!-- 阅读统计抽屉 -->
    <el-drawer v-model="statsVisible" title="阅读统计" size="480px">
      <template v-if="stats">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="应读人数">{{ stats.totalUsers }}</el-descriptions-item>
          <el-descriptions-item label="已读人数">{{ stats.readUsers }}</el-descriptions-item>
          <el-descriptions-item label="未读人数">{{ stats.unreadUsers }}</el-descriptions-item>
          <el-descriptions-item label="阅读率">{{ stats.readRate }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:16px 0 8px">按部门统计</h4>
        <el-table :data="stats.deptStats || []" border size="small">
          <el-table-column prop="deptName" label="部门" />
          <el-table-column prop="totalUsers" label="应读" width="70" />
          <el-table-column prop="readUsers" label="已读" width="70" />
          <el-table-column prop="unreadUsers" label="未读" width="70" />
          <el-table-column prop="readRate" label="率" width="80" />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminNoticeListApi,
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
const query = reactive({ keyword: '', noticeType: '', status: '', priority: '', pageNum: 1, pageSize: 10 })

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
  scopes: [{ scopeType: 'ALL', scopeValue: '', scopeName: '全员' }],
  attachmentIds: []
})
const fileList = ref([])
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
    // onlyPublished=false：管理员需看到全部（含草稿）以便发布
    const res = await getAdminNoticeListApi({ ...query, onlyPublished: false })
    if (res.code === 200 && res.data) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
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
  Object.assign(query, { keyword: '', noticeType: '', status: '', priority: '', pageNum: 1 })
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
    form.scopes =
      d.scopes && d.scopes.length
        ? d.scopes.map((s) => ({ scopeType: s.scopeType, scopeValue: s.scopeValue || '', scopeName: s.scopeName || '' }))
        : [{ scopeType: 'ALL', scopeValue: '', scopeName: '全员' }]
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
  form.scopes = [{ scopeType: 'ALL', scopeValue: '', scopeName: '全员' }]
  form.attachmentIds = []
  fileList.value = []
  formRef.value?.clearValidate?.()
}

const addScope = () => form.scopes.push({ scopeType: 'DEPT', scopeValue: '', scopeName: '' })
const removeScope = (i) => form.scopes.splice(i, 1)
const onScopeTypeChange = (s) => {
  if (s.scopeType === 'ALL') {
    s.scopeValue = ''
    s.scopeName = '全员'
  } else {
    s.scopeName = ''
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
  title: form.title,
  content: form.content,
  noticeType: form.noticeType,
  priority: form.priority,
  scheduledTime: form.scheduledTime || null,
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

onMounted(loadList)
</script>

<style scoped>
.search-bar {
  margin-bottom: 12px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
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
</style>
