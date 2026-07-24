<template>
  <div class="chat-layout">
    <!-- 左侧会话列表 -->
    <aside class="chat-sidebar">
      <div class="sidebar-head">
        <el-button type="primary" class="new-chat-btn" @click="newConversation">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
        <el-button
          v-if="isAdmin"
          class="upload-btn"
          :loading="uploading"
          @click="triggerUpload"
        >
          <el-icon v-if="!uploading"><Upload /></el-icon>
          {{ uploading ? '上传中...' : '上传知识文档' }}
        </el-button>
      </div>
      <div class="conv-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === currentConvId }"
          @click="switchConversation(conv.id)"
        >
          <el-icon class="conv-icon"><ChatDotSquare /></el-icon>
          <span class="conv-title">{{ conv.title }}</span>
          <el-icon class="conv-del" @click.stop="removeConversation(conv.id)"><Delete /></el-icon>
        </div>
        <div v-if="conversations.length === 0" class="conv-empty">暂无对话记录</div>
      </div>

      <!-- 知识库管理（ADMIN） -->
      <div v-if="isAdmin" class="kb-section">
        <div class="kb-toggle" @click="showDocs = !showDocs">
          <span>知识库文档 ({{ documents.length }})</span>
          <span class="kb-arrow">{{ showDocs ? '▼' : '▲' }}</span>
        </div>
        <div v-if="showDocs" class="kb-list">
          <div v-for="doc in documents" :key="doc.source" class="kb-item">
            <div class="kb-info">
              <span class="kb-name" :title="doc.source">{{ doc.source }}</span>
              <span class="kb-chunks">{{ doc.chunks }} 块</span>
            </div>
            <el-icon class="kb-del" @click="removeDocument(doc.source)"><Delete /></el-icon>
          </div>
          <div v-if="documents.length === 0" class="kb-empty">暂无文档，请上传</div>
        </div>
      </div>
    </aside>

    <!-- 右侧聊天区 -->
    <main class="chat-main">
      <div class="chat-head">
        <div>
          <h2>{{ currentConv ? currentConv.title : 'AI 智能问答' }}</h2>
          <p class="chat-sub">基于 RAG 知识库的企业智能助手</p>
        </div>
        <el-tag :type="ragReady ? 'success' : 'info'" effect="plain" size="small">
          知识库 {{ docCount }} 条
        </el-tag>
      </div>

      <div class="chat-messages" ref="messagesRef">
        <!-- 欢迎 -->
        <div v-if="messages.length === 0" class="chat-welcome">
          <el-icon :size="40" color="#2563eb"><ChatLineSquare /></el-icon>
          <h3>OfficeFlow AI 助手</h3>
          <p>基于员工手册和考勤规则的智能问答，试试下面的问题吧</p>
          <div class="quick-btns">
            <el-button v-for="(q, i) in quickQuestions" :key="i" round @click="sendQuick(q)">
              {{ q }}
            </el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.role">
          <div class="msg-avatar">
            <el-icon v-if="msg.role === 'ai'" :size="18" color="#2563eb"><Service /></el-icon>
            <el-icon v-else :size="18" color="#fff"><User /></el-icon>
          </div>
          <div class="msg-bubble" :class="msg.role">
            <div class="msg-text" v-html="formatText(msg.content)"></div>
            <div class="msg-meta">
              {{ formatTime(msg.time) }}
              <span v-if="msg.costMs" class="msg-cost">{{ msg.costMs }}ms</span>
            </div>
          </div>
        </div>

        <!-- 思考中 -->
        <div v-if="loading" class="msg-row ai">
          <div class="msg-avatar">
            <el-icon :size="18" color="#2563eb"><Service /></el-icon>
          </div>
          <div class="msg-bubble ai">
            <div class="typing"><span></span><span></span><span></span></div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputText"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入问题... (Enter 发送, Shift+Enter 换行)"
          resize="none"
          @keydown="onKeydown"
        />
        <el-button
          type="primary"
          :icon="Promotion"
          :disabled="!inputText.trim() || loading"
          circle
          @click="send"
        />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed } from 'vue'
import { ChatLineSquare, Service, User, Promotion, Plus, ChatDotSquare, Delete, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ragQuery, getRagStatus, getConversations, getConversationMessages, createConversation, deleteConversation, uploadKnowledgeFile, getDocuments, deleteDocument } from '@/api/ai'
import { useUserStore } from '@/stores/user'

const conversations = ref([])
const currentConvId = ref(null)
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const docCount = ref(0)
const ragReady = ref(false)
const messagesRef = ref(null)

const userStore = useUserStore()
const isAdmin = computed(() => userStore.hasRole('ADMIN'))
const uploading = ref(false)
const documents = ref([])
const showDocs = ref(false)

const currentConv = computed(() => conversations.value.find(c => c.id === currentConvId.value))

const quickQuestions = [
  '员工迟到怎么处理？',
  '公司有哪些福利？',
  '请假制度是怎样的？',
  '加班调休规则是什么？'
]

/* ── 对话管理 ── */

async function loadConversations() {
  try {
    const res = await getConversations()
    conversations.value = (res.data || res || []).map(c => ({
      id: String(c.id),
      title: c.title,
      updatedAt: c.updated_at || c.updatedAt
    }))
  } catch { /* ignore */ }
}

async function newConversation() {
  try {
    const res = await createConversation()
    const id = String(res.id || res.data?.id)
    conversations.value.unshift({ id, title: '新对话' })
    currentConvId.value = id
    messages.value = []
  } catch { /* ignore */ }
}

async function switchConversation(id) {
  if (currentConvId.value === id) return
  currentConvId.value = id
  messages.value = []
  try {
    const res = await getConversationMessages(id)
    const list = res.data || res || []
    messages.value = list.map(m => ({
      role: m.role === 'assistant' ? 'ai' : m.role,
      content: m.content,
      time: m.created_at || m.createdAt
    }))
    await scrollToBottom()
  } catch { /* ignore */ }
}

async function removeConversation(id) {
  try {
    await deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (currentConvId.value === id) {
      currentConvId.value = null
      messages.value = []
    }
  } catch { /* ignore */ }
}

/* ── 文件上传（ADMIN） ── */

function triggerUpload() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.pdf,.txt,.md'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    uploading.value = true
    try {
      const res = await uploadKnowledgeFile(file)
      const chunks = res.chunks || 0
      ElMessage.success(`${file.name} 上传成功，已分为 ${chunks} 个知识片段`)
      await loadStatus()
      await loadDocuments()
    } catch {
      ElMessage.error('文件上传失败，请检查文件格式')
    } finally {
      uploading.value = false
    }
  }
  input.click()
}

async function loadDocuments() {
  if (!isAdmin.value) return
  try {
    const res = await getDocuments()
    documents.value = (res.data || res || []).map(d => ({
      id: d.id,
      source: d.source,
      chunks: d.chunk_count || d.chunkCount || 0,
      loadedAt: d.loaded_at || d.loadedAt
    }))
  } catch { /* ignore */ }
}

async function removeDocument(source) {
  try {
    await deleteDocument(source)
    ElMessage.success(`已删除: ${source}`)
    await loadDocuments()
    await loadStatus()
  } catch {
    ElMessage.error('删除失败')
  }
}

/* ── 消息发送 ── */

function sendQuick(q) {
  inputText.value = q
  send()
}

async function send() {
  const question = inputText.value.trim()
  if (!question || loading.value) return

  if (!currentConvId.value) {
    await newConversation()
  }

  messages.value.push({ role: 'user', content: question, time: new Date() })
  inputText.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const res = await ragQuery(question, currentConvId.value)
    messages.value.push({
      role: 'ai',
      content: res.answer,
      time: new Date(),
      costMs: res.costMs
    })
    await loadConversations()
  } catch {
    messages.value.push({
      role: 'ai',
      content: '抱歉，查询出错，请稍后再试。',
      time: new Date()
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

/* ── 工具函数 ── */

function formatText(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>')
}

function formatTime(t) {
  return new Date(t).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function scrollToBottom() {
  await nextTick()
  if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
}

async function loadStatus() {
  try {
    const res = await getRagStatus()
    docCount.value = res.totalDocuments || 0
    ragReady.value = docCount.value > 0
  } catch { /* ignore */ }
}

onMounted(async () => {
  await loadStatus()
  await loadConversations()
  await loadDocuments()
})
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: calc(100vh - 112px);
  gap: 0;
  border: 1px solid #e6eaf0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

/* ── 侧边栏 ── */
.chat-sidebar {
  width: 240px;
  border-right: 1px solid #e6eaf0;
  display: flex;
  flex-direction: column;
  background: #f9fafb;
  flex-shrink: 0;
}

.sidebar-head {
  padding: 14px;
  border-bottom: 1px solid #e6eaf0;
}

.new-chat-btn {
  width: 100%;
}

.upload-btn {
  width: 100%;
  margin-top: 8px;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.conv-list::-webkit-scrollbar { width: 4px; }
.conv-list::-webkit-scrollbar-thumb { background: #d0d5dc; border-radius: 2px; }

.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #374151;
  transition: background 0.15s;
  position: relative;
}

.conv-item:hover { background: #eef2ff; }
.conv-item.active { background: #dbeafe; color: #1d4ed8; font-weight: 600; }

.conv-icon { flex-shrink: 0; font-size: 16px; color: #9ca3af; }
.conv-item.active .conv-icon { color: #2563eb; }

.conv-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-del {
  flex-shrink: 0;
  font-size: 14px;
  color: #d1d5db;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s;
}

.conv-item:hover .conv-del { opacity: 1; }
.conv-del:hover { color: #ef4444; }

.conv-empty {
  text-align: center;
  padding: 32px 12px;
  color: #9ca3af;
  font-size: 13px;
}

/* ── 主聊天区 ── */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e6eaf0;
  flex-shrink: 0;
}

.chat-head h2 { margin: 0; font-size: 18px; }
.chat-sub { margin: 2px 0 0; font-size: 12px; color: #9ca3af; }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f9fafb;
}

.chat-messages::-webkit-scrollbar { width: 5px; }
.chat-messages::-webkit-scrollbar-thumb { background: #d0d5dc; border-radius: 3px; }

/* ── 欢迎 ── */
.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
  text-align: center;
}

.chat-welcome h3 { margin: 8px 0 0; font-size: 18px; font-weight: 700; }
.chat-welcome p { margin: 0 0 16px; color: #7b8794; font-size: 14px; }

.quick-btns { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }

/* ── 消息行 ── */
.msg-row { display: flex; gap: 10px; margin-bottom: 16px; align-items: flex-start; }
.msg-row.user { flex-direction: row-reverse; }

.msg-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}

.msg-row.ai .msg-avatar { background: #e8f0fe; }
.msg-row.user .msg-avatar { background: #2563eb; }

.msg-bubble {
  max-width: 65%; padding: 10px 14px; border-radius: 10px;
  font-size: 14px; line-height: 1.7; word-break: break-word;
}

.msg-bubble.ai { background: #fff; border: 1px solid #e6eaf0; border-top-left-radius: 2px; color: #172033; }
.msg-bubble.user { background: #2563eb; color: #fff; border-top-right-radius: 2px; }

.msg-meta { margin-top: 4px; font-size: 11px; display: flex; gap: 8px; }
.msg-row.ai .msg-meta { color: #a0aab4; }
.msg-row.user .msg-meta { color: rgba(255,255,255,0.55); }
.msg-cost { opacity: 0.7; }

/* ── 打字动画 ── */
.typing { display: flex; gap: 4px; padding: 2px 0; }
.typing span {
  width: 7px; height: 7px; background: #a0aab4; border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}
.typing span:nth-child(1) { animation-delay: 0s; }
.typing span:nth-child(2) { animation-delay: 0.16s; }
.typing span:nth-child(3) { animation-delay: 0.32s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.5); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}

/* ── 输入区 ── */
.chat-input {
  display: flex; gap: 10px; align-items: flex-end;
  padding: 14px 20px; border-top: 1px solid #e6eaf0; flex-shrink: 0;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 10px; padding: 8px 14px; font-size: 14px; line-height: 1.6;
}

.chat-input :deep(.el-button) { flex-shrink: 0; width: 38px; height: 38px; }

/* ── 知识库管理 ── */
.kb-section {
  border-top: 1px solid #e6eaf0;
  flex-shrink: 0;
}

.kb-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
  user-select: none;
}

.kb-toggle:hover { color: #2563eb; }
.kb-arrow { font-size: 10px; }

.kb-list {
  max-height: 160px;
  overflow-y: auto;
  padding: 0 6px 6px;
}

.kb-list::-webkit-scrollbar { width: 3px; }
.kb-list::-webkit-scrollbar-thumb { background: #d0d5dc; border-radius: 2px; }

.kb-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.kb-item:hover { background: #f3f4f6; }

.kb-info { display: flex; flex-direction: column; gap: 1px; min-width: 0; flex: 1; }
.kb-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #374151; }
.kb-chunks { font-size: 11px; color: #9ca3af; }

.kb-del { font-size: 13px; color: #d1d5db; cursor: pointer; flex-shrink: 0; }
.kb-del:hover { color: #ef4444; }

.kb-empty { text-align: center; padding: 8px; color: #9ca3af; font-size: 12px; }
</style>
