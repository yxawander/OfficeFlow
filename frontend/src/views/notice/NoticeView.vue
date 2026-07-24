<template>
  <div class="notice-page">
    <el-tabs v-model="activeTab" class="notice-tabs">
      <!-- 用户侧：公告列表 -->
      <el-tab-pane label="公告列表" name="list">
        <div class="toolbar">
          <div class="search-bar">
            <div class="search-input-wrapper">
              <el-icon class="search-prefix-icon"><Search /></el-icon>
              <input
                v-model="query.keyword"
                class="search-input-native"
                placeholder="搜索公告标题或正文内容..."
                @keyup.enter="loadList"
              />
              <el-button v-if="query.keyword" class="search-clear-btn" link @click="query.keyword='';loadList()">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
            <div class="search-filters">
              <el-select v-model="query.noticeType" placeholder="全部类型" clearable>
                <el-option v-for="o in NOTICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-select v-model="query.priority" placeholder="全部优先级" clearable>
                <el-option v-for="o in NOTICE_PRIORITY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-select v-model="query.readStatus" placeholder="阅读状态" clearable>
                <el-option :value="0" label="未读" />
                <el-option :value="1" label="已读" />
              </el-select>
              <div class="filter-spacer"></div>
              <div class="unread-badge">
                <el-icon><Bell /></el-icon>
                <span>未读 <strong>{{ unreadCount.total }}</strong></span>
              </div>
              <el-button type="primary" @click="loadList">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
              <el-button @click="resetQuery">重置</el-button>
              <el-button v-if="selectedIds.length > 0" type="primary" plain @click="batchRead">
                标记已读 ({{ selectedIds.length }})
              </el-button>
            </div>
          </div>
        </div>

        <div v-loading="loading" class="notice-cards-grid">
          <div
            v-for="row in list"
            :key="row.id"
            class="notice-card"
            :class="{ 'is-selected': selectedIds.includes(row.id), 'is-unread': row.readStatus !== 1 }"
            @click="openDetail(row)"
          >
            <div class="card-accent" :class="'priority-' + (row.priority || 'NORMAL').toLowerCase()">
              <div class="card-accent-icon">
                <span class="card-accent-type">{{ getNoticeTypeLabel(row.noticeType) }}</span>
                <div class="card-select" @click.stop="toggleSelect(row)">
                  <span v-if="selectedIds.includes(row.id)" class="select-check">✓</span>
                </div>
              </div>
            </div>
            <div class="card-body">
              <h3 class="card-title">{{ row.title }}</h3>
              <p class="card-excerpt">{{ row.summary || stripHtml(row.content) || '暂无内容' }}</p>
              <div class="card-tags">
                <el-tag size="small">{{ getNoticeTypeLabel(row.noticeType) }}</el-tag>
                <el-tag :type="getNoticePriorityTag(row.priority)" size="small">{{ getNoticePriorityLabel(row.priority) }}</el-tag>
                <el-tag v-if="row.readStatus !== 1" type="danger" size="small" effect="dark">未读</el-tag>
              </div>
            </div>
            <div class="card-footer">
              <span class="card-publisher">
                <el-icon><User /></el-icon>
                {{ row.publisherName }}
              </span>
              <span class="card-time">{{ row.publishTime }}</span>
            </div>
          </div>
        </div>

        <div v-if="!loading && list.length === 0" class="empty-state">
          <el-empty description="暂无公告" />
        </div>

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
      </el-tab-pane>

      <!-- 管理侧：仅 ADMIN 可见 -->
      <el-tab-pane v-if="isAdmin" label="公告管理" name="admin">
        <NoticeAdminView />
      </el-tab-pane>
    </el-tabs>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentDetail.title" width="720px" class="detail-dialog" destroy-on-close>
      <div v-if="currentDetail.id" class="notice-detail">
        <div class="detail-meta">
          <div class="meta-row">
            <div class="meta-item">
              <span class="meta-label">公告类型</span>
              <el-tag size="default">{{ getNoticeTypeLabel(currentDetail.noticeType) }}</el-tag>
            </div>
            <div class="meta-item">
              <span class="meta-label">优先级</span>
              <el-tag :type="getNoticePriorityTag(currentDetail.priority)" size="default" effect="dark">{{ getNoticePriorityLabel(currentDetail.priority) }}</el-tag>
            </div>
          </div>
          <div class="meta-row">
            <div class="meta-item">
              <span class="meta-label">发布人</span>
              <span class="meta-value">
                <el-icon><User /></el-icon>
                {{ currentDetail.publisherName }}
              </span>
            </div>
            <div class="meta-item">
              <span class="meta-label">发布时间</span>
              <span class="meta-value">
                <el-icon><Clock /></el-icon>
                {{ currentDetail.publishTime }}
              </span>
            </div>
          </div>
          <div class="meta-row">
            <div class="meta-item meta-full">
              <span class="meta-label">有效期</span>
              <span class="meta-value" :class="{ 'text-muted': !currentDetail.expireTime }">
                {{ currentDetail.expireTime || '长期有效' }}
              </span>
            </div>
          </div>
        </div>

        <div class="detail-divider">
          <span>正文内容</span>
        </div>
        <div class="notice-content" v-html="currentDetail.content"></div>

        <div v-if="(currentDetail.attachmentList || []).length" class="notice-attachments">
          <div class="detail-divider">
            <span>附件列表</span>
          </div>
          <div
            v-for="a in currentDetail.attachmentList"
            :key="a.id"
            class="attachment-item"
            @click="window.open(a.fileUrl, '_blank')"
          >
            <div class="attachment-icon">
              <el-icon size="24"><Document /></el-icon>
            </div>
            <div class="attachment-info">
              <span class="attachment-name">{{ a.fileName }}</span>
              <span class="attachment-size">{{ formatSize(a.fileSize) }}</span>
            </div>
            <el-icon class="attachment-arrow"><Download /></el-icon>
          </div>
        </div>
      </div>
    </el-dialog>
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

const toggleSelect = (row) => {
  const idx = selectedIds.value.indexOf(row.id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(row.id)
  }
}

const stripHtml = (html) => {
  if (!html) return ''
  return html.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
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
/* ── 搜索工具栏 ── */
.toolbar {
  margin-bottom: 20px;
  width: 100%;
}
.search-bar {
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  width: 100%;
  box-sizing: border-box;
}
.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  margin-bottom: 14px;
  width: 100%;
}
.search-prefix-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 18px;
  z-index: 1;
  pointer-events: none;
}
.search-input-native {
  width: 100%;
  height: 48px;
  padding: 0 44px 0 44px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 15px;
  color: #1e293b;
  background: #f8fafc;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}
.search-input-native::placeholder {
  color: #94a3b8;
}
.search-input-native:focus {
  border-color: var(--el-color-primary);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.08);
}
.search-clear-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 16px;
}

/* ── 筛选行：自适应布局 ── */
.search-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.search-filters .el-select {
  width: 130px;
  flex-shrink: 0;
}
.filter-spacer {
  flex: 1;
  min-width: 0;
}

/* ── 未读徽章 ── */
.unread-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  flex-shrink: 0;
}
.unread-badge .el-icon {
  color: var(--el-color-danger);
  font-size: 16px;
}
.unread-badge strong {
  color: var(--el-color-danger);
  font-size: 16px;
  font-weight: 700;
}

/* ── 卡片网格 ── */
.notice-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.notice-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 2px solid transparent;
  display: flex;
  flex-direction: column;
}
.notice-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.12);
}
.notice-card.is-selected {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.15);
}
.notice-card.is-unread {
  background: #fefefe;
}

/* ── 卡片顶部色条 ── */
.card-accent {
  position: relative;
  height: 72px;
  padding: 12px 16px;
  display: flex;
  align-items: flex-end;
}
.card-accent.priority-urgent {
  background: linear-gradient(135deg, #f56c6c 0%, #e63946 100%);
}
.card-accent.priority-important {
  background: linear-gradient(135deg, #e6a23c 0%, #d97706 100%);
}
.card-accent.priority-normal {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
}

.card-accent-icon {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-accent-type {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.2);
  padding: 3px 12px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

.card-select {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
.card-select:hover {
  background: rgba(255, 255, 255, 0.5);
}
.select-check {
  line-height: 1;
}

/* ── 卡片正文 ── */
.card-body {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.card-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-excerpt {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: auto;
}

/* ── 卡片底部 ── */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid #f1f5f9;
  font-size: 12px;
  color: #94a3b8;
}
.card-publisher {
  display: flex;
  align-items: center;
  gap: 4px;
}
.card-time {
  white-space: nowrap;
}

/* ── 空状态 ── */
.empty-state {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

/* ── 分页居中 ── */
.pager {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

/* ── 详情抽屉 ── */
.drawer-title-bar {
  display: flex;
  align-items: center;
}
.drawer-title-text {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.detail-meta {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px 20px;
}
.meta-row {
  display: flex;
  gap: 24px;
  margin-bottom: 14px;
}
.meta-row:last-child {
  margin-bottom: 0;
}
.meta-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.meta-item.meta-full {
  flex: none;
  width: 100%;
}
.meta-label {
  font-size: 12px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.meta-value {
  font-size: 14px;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 6px;
}
.meta-value.text-muted {
  color: #94a3b8;
}

.detail-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 24px 0 16px;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}
.detail-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.notice-content {
  line-height: 1.9;
  word-break: break-word;
  font-size: 15px;
  color: #334155;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
}

.notice-attachments {
  margin-top: 0;
}
.attachment-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}
.attachment-item:hover {
  background: #f1f5f9;
}
.attachment-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e2e8f0;
  border-radius: 8px;
  color: #64748b;
  flex-shrink: 0;
}
.attachment-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.attachment-name {
  font-size: 14px;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.attachment-size {
  font-size: 12px;
  color: #94a3b8;
}
.attachment-arrow {
  color: #94a3b8;
  flex-shrink: 0;
}
</style>
