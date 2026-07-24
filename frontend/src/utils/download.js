import request from '@/api/request'

/**
 * 下载需要登录鉴权的文件。
 *
 * @param {string} url 后端下载接口地址
 * @param {string} fileName 保存时使用的文件名
 */
export async function downloadFile(url, fileName = 'download') {
  const data = await request.get(url, {
    responseType: 'blob',
    timeout: 60_000
  })

  const blob = data instanceof Blob ? data : new Blob([data])
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = objectUrl
  link.download = fileName || 'download'
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  link.remove()

  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0)
}
