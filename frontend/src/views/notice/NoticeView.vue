<template>
  <div class="notice-page">
    <el-tabs v-model="activeTab" class="notice-tabs">
      <!-- 用户侧：公告列表 -->
      <el-tab-pane label="公告列表" name="list">
        <div class="toolbar">
          <el-form :inline="true" :model="query" class="search-form">
            <el-form-item label="关键词">
              <el-input v-model="query.keyword" placeholder="标题/内容" clearable style="width:200px" @keyup.enter="loadList" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="query.noticeType" placeholder="全部" clearable style="width:130px">
                <el-option v-for="o in NOTICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="优先级">
              <el-select v-model="query.priority" placeholder="全部" clearable style="width:120px">
                <el-option v-for="o in NOTICE_PRIORITY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="阅读状态">
              <el-select v-model="query.readStatus" placeholder="全部" clearable style="width:120px">
                <el-option :value="0" label="未读" />
                <el-option :value="1" label="已读" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadList">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
          <div class="unread-badge">
            未读公告：
            <el-tag type="danger">{{ unreadCount.total }}</el-tag>
            <el-button link type="primary" :disabled="selectedIds.length === 0" @click="batchRead">标记选中为已读</el-button>
          </div>
        </div>

        <el-table :data="list" v-loading="loading" border stripe @selection-change="onSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column label="标题" min-width="220">
            <template #default="{ row }">
              <a class="notice-title" @click="openDetail(row)">{{ row.title }}</a>
            </template>
          </el-table-column>
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
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.readStatus === 1 ? 'success' : 'info'" size="small">
                {{ row.readStatus === 1 ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openDetail(row)">查看</el-button>
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
      </el-tab-pane>

      <!-- 管理侧：仅 ADMIN 可见 -->
      <el-tab-pane v-if="isAdmin" label="公告管理" name="admin">
        <NoticeAdminView />
      </el-tab-pane>
    </el-tabs>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="currentDetail.title" size="580px">
      <div v-if="currentDetail.id" class="notice-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="类型">{{ getNoticeTypeLabel(currentDetail.noticeType) }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="getNoticePriorityTag(currentDetail.priority)" size="small">{{ getNoticePriorityLabel(currentDetail.priority) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发布人">{{ currentDetail.publisherName }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ currentDetail.publishTime }}</el-descriptions-item>
          <el-descriptions-item label="有效期至" :span="2">{{ currentDetail.expireTime || '长期有效' }}</el-descriptions-item>
        </el-descriptions>

        <div class="notice-content" v-html="currentDetail.content"></div>

        <div class="notice-attachments" v-if="(currentDetail.attachmentList || []).length">
          <h4>附件</h4>
          <ul>
            <li v-for="a in currentDetail.attachmentList" :key="a.id">
              <a :href="a.fileUrl" target="_blank" rel="noopener">{{ a.fileName }}</a>
              <span class="file-size">{{ formatSize(a.fileSize) }}</span>
            </li>
          </ul>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import NoticeAdminView from './NoticeAdminView.vue'
import {
  getNoticeListApi,
  searchNoticeApi,
  getNoticeDetailApi,
  markNoticeReadApi,
  batchReadNoticeApi,
  getUnreadCountApi
} from '@/api/notice'
import {
  NOTICE_TYPE_OPTIONS,
  NOTICE_PRIORITY_OPTIONS,
  getNoticeTypeLabel,
  getNoticePriorityLabel,
  getNoticePriorityTag
} from '@/constants'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.hasRole('ADMIN'))

const activeTab = ref('list')
const loading = ref(false)
const list = ref([])
const total = ref(0)
const unreadCount = ref({ total: 0 })
const selectedIds = ref([])

const query = reactive({
  keyword: '',
  noticeType: '',
  priority: '',
  readStatus: '',
  pageNum: 1,
  pageSize: 10
})

const detailVisible = ref(false)
const currentDetail = ref({})

const loadList = async () => {
  loading.value = true
  try {
    const hasKeyword = !!query.keyword
    const api = hasKeyword ? searchNoticeApi : getNoticeListApi
    const params = { ...query }
    if (hasKeyword) {
      delete params.readStatus // 搜索接口不支持 readStatus 过滤
    }
    const res = await api(params)
    if (res.code === 200 && res.data) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

const loadUnreadCount = async () => {
  try {
    const res = await getUnreadCountApi()
    if (res.code === 200 && res.data) unreadCount.value = res.data
  } catch (e) {
    /* 忽略 */
  }
}

const openDetail = async (row) => {
  const res = await getNoticeDetailApi(row.id)
  if (res.code === 200 && res.data) {
    currentDetail.value = res.data
    detailVisible.value = true
    if (row.readStatus !== 1) {
      markNoticeReadApi(row.id, 1).catch(() => {})
      row.readStatus = 1
      loadUnreadCount()
    }
  }
}

const onSelectionChange = (rows) => {
  selectedIds.value = rows.map((r) => r.id)
}

const batchRead = async () => {
  try {
    await batchReadNoticeApi(selectedIds.value)
    ElMessage.success('已标记为已读')
    loadList()
    loadUnreadCount()
  } catch (e) {
    /* 忽略 */
  }
}

const resetQuery = () => {
  Object.assign(query, { keyword: '', noticeType: '', priority: '', readStatus: '', pageNum: 1, pageSize: 10 })
  loadList()
}

const handlePageChange = (p) => {
  query.pageNum = p
  loadList()
}

const formatSize = (bytes) => {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

onMounted(() => {
  loadList()
  loadUnreadCount()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.unread-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 4px;
}
.notice-title {
  color: var(--el-color-primary);
  cursor: pointer;
}
.notice-title:hover {
  text-decoration: underline;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.notice-content {
  margin-top: 16px;
  line-height: 1.8;
  word-break: break-word;
}
.notice-attachments {
  margin-top: 16px;
}
.notice-attachments ul {
  padding-left: 18px;
}
.notice-attachments .file-size {
  color: #999;
  margin-left: 8px;
  font-size: 12px;
}
</style>
