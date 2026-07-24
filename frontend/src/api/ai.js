import request from './request'

/**
 * RAG 智能问答（支持多轮对话）
 * @param {string} question
 * @param {string} [conversationId]
 */
export function ragQuery(question, conversationId) {
  const params = { question }
  if (conversationId) params.conversationId = conversationId
  return request.get('/ai/rag/query', { params, timeout: 60000 })
}

/**
 * 查询知识库状态
 */
export function getRagStatus() {
  return request.get('/ai/rag/status')
}

/**
 * 健康检查
 */
export function getAiHealth() {
  return request.get('/ai/health')
}

/**
 * 获取对话列表
 */
export function getConversations() {
  return request.get('/ai/rag/conversations')
}

/**
 * 获取对话消息
 */
export function getConversationMessages(id) {
  return request.get(`/ai/rag/conversations/${id}/messages`)
}

/**
 * 创建新对话
 */
export function createConversation(title) {
  return request.post('/ai/rag/conversations', { title })
}

/**
 * 删除对话
 */
export function deleteConversation(id) {
  return request.delete(`/ai/rag/conversations/${id}`)
}

/**
 * 上传文件到知识库（仅 ADMIN）
 * @param {File} file
 */
export function uploadKnowledgeFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/ai/rag/upload-file', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/**
 * 获取知识库文档列表（仅 ADMIN）
 */
export function getDocuments() {
  return request.get('/ai/rag/documents')
}

/**
 * 删除知识库中的指定文档（仅 ADMIN）
 */
export function deleteDocument(source) {
  return request.delete(`/ai/rag/documents/${encodeURIComponent(source)}`)
}
