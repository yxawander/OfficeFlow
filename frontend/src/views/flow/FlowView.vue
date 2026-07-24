<template>
  <div class="flow-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">审批中心</h1>
        <p class="page-subtitle">轻松提交请假、加班与补卡申请，高效进行线上团队审批</p>
      </div>
      <div class="action-buttons">
        <el-button type="primary" size="large" icon="Plus" @click="openApplyDialog('LEAVE')">
          请假申请
        </el-button>
        <el-button type="success" size="large" icon="Timer" @click="openApplyDialog('OVERTIME')">
          加班申请
        </el-button>
        <el-button type="warning" size="large" icon="Calendar" @click="openApplyDialog('CORRECTION')">
          补卡申请
        </el-button>
      </div>
    </div>

    <!-- Main Tabs -->
    <el-tabs v-model="activeTab" class="flow-tabs" @tab-click="handleTabClick">
      <el-tab-pane label="我的申请单据" name="my">
        <div class="panel">
          <el-table :data="myApplies" v-loading="loadingMy" border stripe style="width: 100%">
            <el-table-column prop="applyNo" label="申请单号" width="170" />
            <el-table-column prop="applyType" label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="getTypeTagType(row.applyType)">
                  {{ formatApplyType(row.applyType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="150" />
            <el-table-column prop="reason" label="申请事由" min-width="200" show-overflow-tooltip />
            <el-table-column prop="durationHours" label="时长" width="110">
              <template #default="{ row }">
                {{ row.applyType === 'LEAVE' ? (row.durationHours / 8) + ' 天' : (row.durationHours || 0) + ' 小时' }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ formatStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="提交时间" width="170" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button 
                  link 
                  type="primary" 
                  size="small" 
                  @click="openDetailDialog(row)"
                >
                  详情
                </el-button>
                <el-button 
                  v-if="row.status === 'PENDING'" 
                  link 
                  type="danger" 
                  size="small" 
                  @click="handleCancel(row.id)"
                >
                  撤销
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="canHandleApproval" label="待我审批" name="pending">
        <div class="panel">
          <div class="panel-alert" v-if="pendingApplies.length === 0 && !loadingPending">
            <el-empty description="暂无待您审批的单据" />
          </div>
          <el-table v-else :data="pendingApplies" v-loading="loadingPending" border stripe style="width: 100%">
            <el-table-column prop="applyNo" label="申请单号" width="170" />
            <el-table-column prop="applicantName" label="申请人" width="120" />
            <el-table-column prop="applicantDeptName" label="部门" width="120" />
            <el-table-column prop="applyType" label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="getTypeTagType(row.applyType)">
                  {{ formatApplyType(row.applyType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="150" />
            <el-table-column prop="reason" label="申请事由" min-width="200" show-overflow-tooltip />
            <el-table-column prop="durationHours" label="时长" width="100">
              <template #default="{ row }">
                {{ row.applyType === 'LEAVE' ? (row.durationHours / 8) + 'd' : (row.durationHours || 0) + 'h' }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="提交时间" width="170" />
            <el-table-column label="审批操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button type="info" plain size="small" icon="View" @click="openDetailDialog(row)">
                  详情
                </el-button>
                <el-button type="success" size="small" icon="Check" @click="handleApprove(row)">
                  同意
                </el-button>
                <el-button type="danger" size="small" icon="Close" @click="handleReject(row)">
                  驳回
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="canHandleApproval" label="已办审批" name="processed">
        <div class="panel">
          <div class="panel-alert" v-if="processedApplies.length === 0 && !loadingProcessed">
            <el-empty description="暂无您已处理的单据" />
          </div>
          <el-table v-else :data="processedApplies" v-loading="loadingProcessed" border stripe style="width: 100%">
            <el-table-column prop="applyNo" label="申请单号" width="170" />
            <el-table-column prop="applicantName" label="申请人" width="120" />
            <el-table-column prop="applyType" label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="getTypeTagType(row.applyType)">
                  {{ formatApplyType(row.applyType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="最终状态" width="110">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ formatStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="myAction" label="我的动作" width="110">
              <template #default="{ row }">
                <el-tag :type="row.myAction === 'APPROVE' ? 'success' : 'danger'" effect="plain">
                  {{ row.myAction === 'APPROVE' ? '同意' : '驳回' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="handleTime" label="处理时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openDetailDialog(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 申请弹窗 -->
    <el-dialog
      v-model="applyDialogVisible"
      :title="applyDialogTitle"
      width="540px"
      destroy-on-close
    >
      <el-form :model="applyForm" ref="applyFormRef" :rules="applyRules" label-width="100px">
        <el-form-item label="申请标题" prop="title">
          <el-input v-model="applyForm.title" placeholder="请输入申请标题" />
        </el-form-item>

        <template v-if="applyForm.applyType === 'CORRECTION'">
          <el-form-item label="补卡类型" prop="correctionType">
            <el-radio-group v-model="applyForm.correctionType">
              <el-radio label="CHECK_IN">上班打卡补卡</el-radio>
              <el-radio label="CHECK_OUT">下班打卡补卡</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="补卡时间" prop="correctionTime">
            <el-date-picker
              v-model="applyForm.correctionTime"
              type="datetime"
              placeholder="选择拟补打卡时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </template>

        <template v-else-if="applyForm.applyType === 'OVERTIME'">
          <el-form-item label="开始时间">
            <el-input v-model="applyForm.startTime" disabled />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker
              v-model="applyForm.endTime"
              type="datetime"
              placeholder="选择结束时间（仅限今日）"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
              :disabled-date="disabledOvertimeDate"
              @change="calculateOvertimeDuration"
            />
          </el-form-item>
          <el-form-item label="时长 (小时)">
            <el-input-number v-model="applyForm.durationHours" :min="0.5" :precision="1" :step="0.5" disabled />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="时间范围" prop="timeRange">
            <el-date-picker
              v-model="applyForm.timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
              :disabled-date="disabledApplyDate"
              @change="calculateDuration"
            />
          </el-form-item>
          <el-form-item :label="applyForm.applyType === 'LEAVE' ? '时长 (天)' : '时长 (小时)'">
            <el-input-number v-model="displayDuration" :min="0.5" :precision="1" :step="0.5" />
          </el-form-item>
        </template>

        <el-form-item label="申请事由" prop="reason">
          <el-input
            v-model="applyForm.reason"
            type="textarea"
            :rows="3"
            placeholder="详细说明申请原因"
          />
        </el-form-item>

        <el-form-item label="附件">
          <el-upload
            :http-request="customFlowUpload"
            :file-list="flowFileList"
            :on-remove="removeFlowFile"
            list-type="text"
            multiple
          >
            <el-button size="small" type="primary" plain>
              <el-icon><Upload /></el-icon> 选择文件
            </el-button>
            <template #tip>
              <div class="el-upload__tip">支持图片、PDF、Word 等格式，单文件不超过10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">确认提交</el-button>
      </template>
    </el-dialog>

    <!-- 审批意见弹窗 -->
    <el-dialog
      v-model="actionDialogVisible"
      :title="actionType === 'APPROVE' ? '同意审批' : '驳回审批'"
      width="450px"
    >
      <el-form label-width="80px">
        <el-form-item label="审批意见">
          <el-input
            v-model="actionComment"
            type="textarea"
            :rows="3"
            :placeholder="actionType === 'APPROVE' ? '填写审批同意意见（可选）' : '填写驳回原因（必填）'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDialogVisible = false">取消</el-button>
        <el-button
          :type="actionType === 'APPROVE' ? 'success' : 'danger'"
          :loading="submittingAction"
          @click="confirmAction"
        >
          确定{{ actionType === 'APPROVE' ? '同意' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 申请单详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="审批记录详情"
      width="560px"
    >
      <template v-if="currentDetailRow">
        <!-- 基本信息 -->
        <div class="detail-header">
          <div class="detail-info-line">
            <span class="detail-label">申请单号：</span>
            <span>{{ currentDetailRow.applyNo }}</span>
          </div>
          <div class="detail-info-line">
            <span class="detail-label">申请类型：</span>
            <el-tag :type="getTypeTagType(currentDetailRow.applyType)" size="small">{{ formatApplyType(currentDetailRow.applyType) }}</el-tag>
            <el-tag :type="getStatusTagType(currentDetailRow.status)" size="small" style="margin-left:8px">{{ formatStatusText(currentDetailRow.status) }}</el-tag>
          </div>
          <div class="detail-info-line">
            <span class="detail-label">标题：</span>
            <span>{{ currentDetailRow.title }}</span>
          </div>
          <div class="detail-info-line">
            <span class="detail-label">事由：</span>
            <span>{{ currentDetailRow.reason }}</span>
          </div>
          <div class="detail-info-line" v-if="currentDetailRow.startTime">
            <span class="detail-label">时间：</span>
            <span>{{ currentDetailRow.startTime }} ~ {{ currentDetailRow.endTime }}</span>
          </div>
        </div>

        <el-divider />

        <!-- 审批流程时间线 -->
        <div class="approval-timeline">
          <h4 class="timeline-title">审批流程</h4>
          <div v-if="currentDetailRow.approveRecords && currentDetailRow.approveRecords.length > 0">
            <div
              v-for="(record, idx) in currentDetailRow.approveRecords"
              :key="idx"
              class="timeline-item"
            >
              <div class="timeline-node" :class="record.action === 'APPROVE' ? 'node-approve' : 'node-reject'">
                <el-icon v-if="record.action === 'APPROVE'"><Check /></el-icon>
                <el-icon v-else><Close /></el-icon>
              </div>
              <div class="timeline-content">
                <div class="timeline-actor">
                  <strong>{{ record.approverName || '系统' }}</strong>
                  <el-tag :type="record.action === 'APPROVE' ? 'success' : 'danger'" size="small" effect="light">
                    {{ record.action === 'APPROVE' ? '审批通过' : '审批驳回' }}
                  </el-tag>
                </div>
                <div class="timeline-comment" v-if="record.comment">{{ record.comment }}</div>
                <div class="timeline-time">{{ record.approvedAt }}</div>
              </div>
            </div>
          </div>
          <div v-else class="timeline-empty">暂无审批记录</div>
        </div>

        <!-- 附件 -->
        <div v-if="currentDetailRow.attachments && currentDetailRow.attachments.length > 0" class="detail-attachments">
          <el-divider />
          <h4 class="timeline-title">附件</h4>
          <div v-for="att in currentDetailRow.attachments" :key="att.id" class="attachment-link">
            <el-icon><Link /></el-icon>
            <span>{{ att.fileName }}</span>
          </div>
        </div>
      </template>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Check, Close, Link } from '@element-plus/icons-vue'

const router = useRouter()
import {
  createApplyApi,
  getMyAppliesApi,
  getApplyDetailApi,
  getPendingAppliesApi,
  getProcessedAppliesApi,
  approveApplyApi,
  rejectApplyApi,
  cancelApplyApi,
  uploadFlowAttachmentApi,
  deleteFlowAttachmentApi
} from '@/api/flow'
import { recheckApi } from '@/api/attendance'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activeTab = ref('my')

const canHandleApproval = computed(() => {
  const user = userStore.profile || {}
  const roles = user.roles || []
  const permissions = user.permissions || []
  return user.userType === 'ADMIN'
    || user.userType === 'MANAGER'
    || roles.some(role => ['ADMIN', 'MANAGER'].includes(role.roleCode))
    || permissions.includes('flow:approve')
    || permissions.includes('flow:reject')
})

// 数据
const myApplies = ref([])
const pendingApplies = ref([])
const processedApplies = ref([])

// 加载状态
const loadingMy = ref(false)
const loadingPending = ref(false)
const loadingProcessed = ref(false)

// 申请弹窗
const applyDialogVisible = ref(false)
const submitting = ref(false)
const applyFormRef = ref(null)
const flowFileList = ref([])
const flowAttachmentIds = ref([])

const applyForm = reactive({
  applyType: 'LEAVE',
  title: '',
  reason: '',
  timeRange: [],
  startTime: '',
  endTime: '',
  durationHours: 8,
  correctionType: 'CHECK_IN',
  correctionTime: ''
})

const displayDuration = computed({
  get: () => applyForm.applyType === 'LEAVE' ? applyForm.durationHours / 8 : applyForm.durationHours,
  set: (val) => { applyForm.durationHours = applyForm.applyType === 'LEAVE' ? val * 8 : val }
})

const applyRules = {
  title: [{ required: true, message: '请输入申请标题', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入申请事由', trigger: 'blur' }]
}

const applyDialogTitle = computed(() => {
  switch (applyForm.applyType) {
    case 'LEAVE': return '发起请假申请'
    case 'OVERTIME': return '发起加班申请'
    case 'CORRECTION': return '发起考勤补卡申请'
    default: return '发起申请'
  }
})

const disabledApplyDate = (time) => {
  if (applyForm.applyType === 'LEAVE') {
    // 请假不能选择过去的时间（基于今天0点）
    return time.getTime() < new Date().setHours(0, 0, 0, 0)
  }
  return false
}

const disabledOvertimeDate = (time) => {
  // 加班强制今天
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const tomorrow = new Date(today)
  tomorrow.setDate(tomorrow.getDate() + 1)
  return time.getTime() < today.getTime() || time.getTime() >= tomorrow.getTime()
}

// 审批处理弹窗
const actionDialogVisible = ref(false)
const actionType = ref('APPROVE') // APPROVE or REJECT
const currentTargetRow = ref(null)
const actionComment = ref('')
const submittingAction = ref(false)

// 详情弹窗
const detailDialogVisible = ref(false)
const currentDetailRow = ref(null)

const openDetailDialog = async (row) => {
  try {
    const res = await getApplyDetailApi(row.id)
    if (res.code === 200 && res.data) {
      currentDetailRow.value = res.data
    } else {
      currentDetailRow.value = row
    }
  } catch (e) {
    currentDetailRow.value = row
  }
  detailDialogVisible.value = true
}

const loadMyApplies = async () => {
  loadingMy.value = true
  try {
    const res = await getMyAppliesApi({ pageNum: 1, pageSize: 50 })
    if (res.code === 200 && res.data) {
      myApplies.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载我的申请失败', error)
  } finally {
    loadingMy.value = false
  }
}

const loadPendingApplies = async () => {
  loadingPending.value = true
  try {
    const res = await getPendingAppliesApi({ pageNum: 1, pageSize: 50 })
    if (res.code === 200 && res.data) {
      pendingApplies.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载待审批失败', error)
  } finally {
    loadingPending.value = false
  }
}

const loadProcessedApplies = async () => {
  loadingProcessed.value = true
  try {
    const res = await getProcessedAppliesApi({ pageNum: 1, pageSize: 50 })
    if (res.code === 200 && res.data) {
      processedApplies.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载已审批失败', error)
  } finally {
    loadingProcessed.value = false
  }
}

const handleTabClick = (tab) => {
  if (tab.paneName === 'my') {
    loadMyApplies()
  } else if (tab.paneName === 'pending' && canHandleApproval.value) {
    loadPendingApplies()
  } else if (tab.paneName === 'processed' && canHandleApproval.value) {
    loadProcessedApplies()
  }
}

const openApplyDialog = (type) => {
  if (type === 'CORRECTION') {
    ElMessage.info('已为您跳转至考勤历史页面，请选择具体迟到/旷工日期发起补卡！')
    router.push('/attendance')
    return
  }
  applyForm.applyType = type
  applyForm.title = type === 'LEAVE' ? '事假申请' : '加班申请'
  applyForm.reason = ''
  applyForm.timeRange = []
  
  if (type === 'OVERTIME') {
    const today = new Date()
    const year = today.getFullYear()
    const month = String(today.getMonth() + 1).padStart(2, '0')
    const day = String(today.getDate()).padStart(2, '0')
    applyForm.startTime = `${year}-${month}-${day} 18:00:00`
    applyForm.endTime = ''
    applyForm.durationHours = 0
  } else {
    applyForm.startTime = ''
    applyForm.endTime = ''
    applyForm.durationHours = type === 'LEAVE' ? 8 : 1
  }
  flowFileList.value = []
  flowAttachmentIds.value = []
  
  applyDialogVisible.value = true
}

const calculateDuration = (val) => {
  if (val && val.length === 2) {
    applyForm.startTime = val[0]
    applyForm.endTime = val[1]
    const start = new Date(val[0])
    const end = new Date(val[1])
    
    if (applyForm.applyType === 'LEAVE') {
      let adjustedEnd = new Date(end.getTime())
      if (adjustedEnd.getHours() === 0 && adjustedEnd.getMinutes() === 0 && adjustedEnd.getSeconds() === 0 && end.getTime() > start.getTime()) {
        adjustedEnd = new Date(end.getTime() - 1)
      }
      const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate())
      const endDate = new Date(adjustedEnd.getFullYear(), adjustedEnd.getMonth(), adjustedEnd.getDate())
      const daysDiff = Math.round((endDate - startDate) / (1000 * 3600 * 24)) + 1
      applyForm.durationHours = daysDiff * 8
    } else {
      const diffHours = (end - start) / (1000 * 3600)
      applyForm.durationHours = Math.max(0.5, Math.round(diffHours * 10) / 10)
    }
  }
}

// 附件上传处理
const customFlowUpload = async ({ file }) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadFlowAttachmentApi(formData)
    if (res.code === 200 && res.data) {
      flowAttachmentIds.value.push(res.data.id)
      flowFileList.value.push({
        uid: res.data.id,
        name: res.data.fileName,
        url: res.data.fileUrl,
        status: 'success'
      })
    } else {
      throw new Error(res.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error('附件上传失败: ' + (e.message || '未知错误'))
    throw e
  }
}

const removeFlowFile = async (file) => {
  try {
    if (file.uid) {
      await deleteFlowAttachmentApi(file.uid)
      flowAttachmentIds.value = flowAttachmentIds.value.filter(id => id !== file.uid)
    }
  } catch (e) {
    console.error('删除附件失败', e)
  }
}

const calculateOvertimeDuration = (val) => {
  if (applyForm.startTime && val) {
    const start = new Date(applyForm.startTime)
    const end = new Date(val)
    if (end <= start) {
      ElMessage.warning('加班结束时间必须晚于18:00')
      applyForm.endTime = ''
      applyForm.durationHours = 0
      return
    }
    const diffHours = (end - start) / (1000 * 3600)
    applyForm.durationHours = Math.max(0.5, Math.round(diffHours * 10) / 10)
  } else {
    applyForm.durationHours = 0
  }
}

const submitApply = async () => {
  if (!applyFormRef.value) return
  await applyFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (applyForm.applyType === 'CORRECTION') {
        if (!applyForm.correctionTime) {
          ElMessage.warning('请选择要申请补打卡的时间')
          submitting.value = false
          return
        }
        await recheckApi({
          correctionType: applyForm.correctionType,
          correctionTime: applyForm.correctionTime,
          reason: applyForm.reason
        })
      } else {
        if (!applyForm.startTime || !applyForm.endTime) {
          ElMessage.warning('请选择开始和结束时间')
          submitting.value = false
          return
        }
        await createApplyApi({
          applyType: applyForm.applyType,
          title: applyForm.title,
          reason: applyForm.reason,
          startTime: applyForm.startTime,
          endTime: applyForm.endTime,
          durationHours: applyForm.durationHours,
          attachmentIds: flowAttachmentIds.value
        })
      }

      ElMessage.success('申请提交成功！')
      applyDialogVisible.value = false
      loadMyApplies()
    } catch (error) {
      console.error(error)
    } finally {
      submitting.value = false
    }
  })
}

const handleCancel = async (id) => {
  try {
    await ElMessageBox.confirm('确定要撤销该申请单吗？', '提示', { type: 'warning' })
    await cancelApplyApi(id)
    ElMessage.success('撤销成功')
    loadMyApplies()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('撤销失败', e)
    }
  }
}

const handleApprove = (row) => {
  currentTargetRow.value = row
  actionType.value = 'APPROVE'
  actionComment.value = '同意'
  actionDialogVisible.value = true
}

const handleReject = (row) => {
  currentTargetRow.value = row
  actionType.value = 'REJECT'
  actionComment.value = ''
  actionDialogVisible.value = true
}

const confirmAction = async () => {
  if (actionType.value === 'REJECT' && !actionComment.value.trim()) {
    ElMessage.warning('驳回时必须填写原因')
    return
  }
  submittingAction.value = true
  try {
    if (actionType.value === 'APPROVE') {
      await approveApplyApi(currentTargetRow.value.id, { comment: actionComment.value })
      ElMessage.success('已同意该申请')
    } else {
      await rejectApplyApi(currentTargetRow.value.id, { comment: actionComment.value })
      ElMessage.success('已驳回该申请')
    }
    actionDialogVisible.value = false
    loadPendingApplies()
  } catch (error) {
    console.error('审批操作失败', error)
  } finally {
    submittingAction.value = false
  }
}

const formatApplyType = (type) => {
  switch (type) {
    case 'LEAVE': return '请假'
    case 'OVERTIME': return '加班'
    case 'CORRECTION': return '补卡'
    default: return type
  }
}

const getTypeTagType = (type) => {
  switch (type) {
    case 'LEAVE': return 'primary'
    case 'OVERTIME': return 'success'
    case 'CORRECTION': return 'warning'
    default: return 'info'
  }
}

const formatStatusText = (status) => {
  switch (status) {
    case 'PENDING': return '待审批'
    case 'APPROVED': return '已同意'
    case 'REJECTED': return '已驳回'
    case 'CANCELED': return '已撤销'
    default: return status
  }
}

const getStatusTagType = (status) => {
  switch (status) {
    case 'PENDING': return 'warning'
    case 'APPROVED': return 'success'
    case 'REJECTED': return 'danger'
    case 'CANCELED': return 'info'
    default: return 'info'
  }
}

onMounted(() => {
  loadMyApplies()
  if (canHandleApproval.value) {
    loadPendingApplies()
  }
})
</script>

<style scoped>
.flow-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 6px 0;
}

.page-subtitle {
  color: #64748b;
  margin: 0;
  font-size: 14px;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.flow-tabs {
  background: #ffffff;
  padding: 16px 24px 24px;
  border-radius: 12px;
  box-shadow: 0 4px 20px -2px rgba(0, 0, 0, 0.05);
}

.panel {
  margin-top: 12px;
}

.text-muted {
  color: #94a3b8;
}

/* 详情弹窗 - 时间线 */
.detail-header {
  padding: 4px 0;
}
.detail-info-line {
  padding: 6px 0;
  font-size: 14px;
  line-height: 24px;
}
.detail-label {
  color: #909399;
  font-weight: 500;
}

.approval-timeline {
  padding: 4px 0;
}
.timeline-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: #303133;
}
.timeline-item {
  display: flex;
  gap: 14px;
  padding-bottom: 20px;
  position: relative;
}
.timeline-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 15px;
  top: 34px;
  bottom: 0;
  width: 2px;
  background: #e4e7ed;
}
.timeline-node {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  font-size: 14px;
}
.timeline-node.node-approve {
  background: #67c23a;
}
.timeline-node.node-reject {
  background: #f56c6c;
}
.timeline-content {
  flex: 1;
  background: #fafafa;
  padding: 10px 14px;
  border-radius: 8px;
}
.timeline-actor {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.timeline-comment {
  font-size: 13px;
  color: #606266;
}
.timeline-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.timeline-empty {
  text-align: center;
  color: #c0c4cc;
  padding: 20px 0;
}
.detail-attachments {
  margin-top: 4px;
}
.attachment-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
}
.attachment-link:hover {
  text-decoration: underline;
}
</style>
