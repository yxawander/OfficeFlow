<template>
  <div class="chat-container">
    <div class="chat-panel">
      <!-- 顶部 -->
      <div class="chat-head">
        <div>
          <h2>AI 智能问答</h2>
          <p class="panel-subtitle">基于 RAG 知识库的企业智能助手</p>
        </div>
        <el-tag :type="ragReady ? 'success' : 'info'" effect="plain" size="small">
          知识库 {{ docCount }} 条
        </el-tag>
      </div>

      <!-- 消息区 -->
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
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="msg-row"
          :class="msg.role"
        >
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
            <div class="typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
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
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ChatLineSquare, Service, User, Promotion } from '@element-plus/icons-vue'
import { ragQuery, getRagStatus } from '@/api/ai'

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const docCount = ref(0)
const ragReady = ref(false)
const messagesRef = ref(null)

const quickQuestions = [
  '员工迟到怎么处理？',
  '公司有哪些福利？',
  '请假制度是怎样的？',
  '加班调休规则是什么？'
]

function sendQuick(q) {
  inputText.value = q
  send()
}

async function send() {
  const question = inputText.value.trim()
  if (!question || loading.value) return

  messages.value.push({ role: 'user', content: question, time: new Date() })
  inputText.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const res = await ragQuery(question)
    messages.value.push({
      role: 'ai',
      content: res.answer,
      time: new Date(),
      costMs: res.costMs
    })
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

onMounted(loadStatus)
</script>

<style scoped>
.chat-container {
  height: calc(100vh - 112px);
}

.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid #e6eaf0;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
}

/* ---- 顶部 ---- */
.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e6eaf0;
  flex-shrink: 0;
}

.chat-head h2 {
  margin: 0;
  font-size: 18px;
}

/* ---- 消息区 ---- */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f9fafb;
}

.chat-messages::-webkit-scrollbar {
  width: 5px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #d0d5dc;
  border-radius: 3px;
}

/* ---- 欢迎 ---- */
.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
  text-align: center;
}

.chat-welcome h3 {
  margin: 8px 0 0;
  font-size: 18px;
  font-weight: 700;
}

.chat-welcome p {
  margin: 0 0 16px;
  color: #7b8794;
  font-size: 14px;
}

.quick-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

/* ---- 消息行 ---- */
.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: flex-start;
}

.msg-row.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-row.ai .msg-avatar {
  background: #e8f0fe;
}

.msg-row.user .msg-avatar {
  background: #2563eb;
}

.msg-bubble {
  max-width: 65%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.msg-bubble.ai {
  background: #ffffff;
  border: 1px solid #e6eaf0;
  border-top-left-radius: 2px;
  color: #172033;
}

.msg-bubble.user {
  background: #2563eb;
  color: #ffffff;
  border-top-right-radius: 2px;
}

.msg-meta {
  margin-top: 4px;
  font-size: 11px;
  display: flex;
  gap: 8px;
}

.msg-row.ai .msg-meta {
  color: #a0aab4;
}

.msg-row.user .msg-meta {
  color: rgba(255, 255, 255, 0.55);
}

.msg-cost {
  opacity: 0.7;
}

/* ---- 打字动画 ---- */
.typing {
  display: flex;
  gap: 4px;
  padding: 2px 0;
}

.typing span {
  width: 7px;
  height: 7px;
  background: #a0aab4;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing span:nth-child(1) { animation-delay: 0s; }
.typing span:nth-child(2) { animation-delay: 0.16s; }
.typing span:nth-child(3) { animation-delay: 0.32s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.5); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}

/* ---- 输入区 ---- */
.chat-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 14px 20px;
  border-top: 1px solid #e6eaf0;
  flex-shrink: 0;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 10px;
  padding: 8px 14px;
  font-size: 14px;
  line-height: 1.6;
}

.chat-input :deep(.el-button) {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
}
</style>
