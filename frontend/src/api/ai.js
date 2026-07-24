import request from './request'

/**
 * RAG 智能问答
 * @param {string} question
 * @returns {Promise<{answer: string, sources: string[], costMs: number}>}
 */
export function ragQuery(question) {
  return request.get('/ai/rag/query', {
    params: { question },
    timeout: 60000
  })
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
