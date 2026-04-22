export const uploadAction = '/v1/common/file/upload'

export function getUploadHeaders() {
  const token = localStorage.getItem('token')
  return token ? { Authorization: token } : {}
}

export const UploadType = Object.freeze({
  Excel: 'excel',
  Image: 'image',
  Video: 'video',
  Document: 'document',
  Any: 'any'
})

function getExts(type) {
  if (type === UploadType.Excel) return ['xls', 'xlsx', 'csv']
  if (type === UploadType.Image) return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']
  if (type === UploadType.Video) return ['mp4', 'avi', 'mov', 'wmv', 'mkv', 'webm', 'mpeg', 'mpg', 'flv']
  if (type === UploadType.Document) return ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'txt', 'xls', 'xlsx', 'csv']
  return []
}

export function getAcceptByType(type) {
  if (type === UploadType.Excel) return '.xls,.xlsx,.csv'
  if (type === UploadType.Image) return 'image/*'
  if (type === UploadType.Video) return 'video/*'
  if (type === UploadType.Document) return '.pdf,.doc,.docx,.ppt,.pptx,.txt,.xls,.xlsx,.csv'
  return ''
}

export function validateFile(file, type = UploadType.Any, maxMB = 10) {
  const ext = String(file.name || '').split('.').pop().toLowerCase()
  const exts = getExts(type)
  if (exts.length && !exts.includes(ext)) {
    if (type === UploadType.Excel) return { valid: false, message: '只能上传Excel文件（.xls, .xlsx, .csv）!' }
    if (type === UploadType.Image) return { valid: false, message: '只能上传图片文件（jpg、jpeg、png、gif、bmp、webp、svg）!' }
    if (type === UploadType.Video) return { valid: false, message: '只能上传视频文件（mp4、avi、mov、wmv、mkv、webm、mpeg、mpg、flv）!' }
    if (type === UploadType.Document) return { valid: false, message: '只能上传文档文件（pdf、doc、docx、ppt、pptx、txt、xls、xlsx、csv）!' }
    return { valid: false, message: '文件类型不支持' }
  }
  const ok = file.size / 1024 / 1024 < maxMB
  if (!ok) {
    return { valid: false, message: `上传文件大小不能超过 ${maxMB}MB!` }
  }
  return { valid: true }
}

export function validateExcelFile(file, maxMB = 10) {
  return validateFile(file, UploadType.Excel, maxMB)
}

export function buildUploadProps(opts = {}) {
  return {
    action: uploadAction,
    headers: getUploadHeaders(),
    data: opts.data || {},
    accept: opts.accept || getAcceptByType(opts.type || UploadType.Excel),
    limit: typeof opts.limit === 'number' ? opts.limit : 1,
    autoUpload: typeof opts.autoUpload === 'boolean' ? opts.autoUpload : true
  }
}

export function getButtonTextByType(type) {
  if (type === UploadType.Excel) return '选择Excel文件'
  if (type === UploadType.Image) return '选择图片文件'
  if (type === UploadType.Video) return '选择视频文件'
  if (type === UploadType.Document) return '选择文档文件'
  return '选择文件'
}

export function getTipTextByType(type, { maxMB = 10, limit = 1 } = {}) {
  const limitText = `且只能上传${limit === 1 ? '一个' : limit + '个'}文件，大小不超过${maxMB}MB`
  if (type === UploadType.Excel) return `只能上传Excel文件（.xls, .xlsx, .csv），${limitText}`
  if (type === UploadType.Image) return `只能上传图片文件，${limitText}`
  if (type === UploadType.Video) return `只能上传视频文件，${limitText}`
  if (type === UploadType.Document) return `只能上传文档文件（pdf, doc, docx, ppt, pptx, txt, xls, xlsx, csv），${limitText}`
  return `可上传任意文件类型，${limitText}`
}

export function normalizeUrl(url) {
  return (url || '').replace(/`/g, '').trim()
}

export function getFileIcon(url) {
  const fileUrl = normalizeUrl(url)
  if (!fileUrl) return 'el-icon-document'
  const extension = fileUrl.split('.').pop().toLowerCase()
  const iconMap = {
    xls: 'el-icon-document',
    xlsx: 'el-icon-document',
    csv: 'el-icon-document',
    pdf: 'el-icon-document',
    doc: 'el-icon-document',
    docx: 'el-icon-document',
    ppt: 'el-icon-document',
    pptx: 'el-icon-document',
    jpg: 'el-icon-picture',
    jpeg: 'el-icon-picture',
    png: 'el-icon-picture',
    gif: 'el-icon-picture',
    bmp: 'el-icon-picture',
    txt: 'el-icon-tickets',
    zip: 'el-icon-folder',
    rar: 'el-icon-folder',
    '7z': 'el-icon-folder'
  }
  return iconMap[extension] || 'el-icon-document'
}

export function getFileName(url) {
  const fileUrl = normalizeUrl(url)
  if (!fileUrl) return '未知文件'
  const fileName = fileUrl.split('/').pop()
  return fileName || '未知文件'
}

export function downloadByUrl(url, fileName) {
  const fileUrl = normalizeUrl(url)
  if (!fileUrl) return false
  const link = document.createElement('a')
  link.href = fileUrl
  link.target = '_blank'
  link.download = fileName || getFileName(fileUrl)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  return true
}