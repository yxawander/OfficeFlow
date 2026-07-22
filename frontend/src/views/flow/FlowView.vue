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
            <el-table-column prop="durationHours" label="时长(小时)" width="110">
              <template #default="{ row }">
                {{ row.durationHours || 0 }} 小时
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
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button 
                  v-if="row.status === 'PENDING'" 
                  link 
                  type="danger" 
                  size="small" 
                  @click="handleCancel(row.id)"
                >
                  撤销
                </el-button>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="待我审批" name="pending">
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
                {{ row.durationHours || 0 }}h
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="提交时间" width="170" />
            <el-table-column label="审批操作" width="180" fixed="right">
              <template #default="{ row }">
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
              @change="calculateDuration"
            />
          </el-form-item>
          <el-form-item label="时长 (小时)">
            <el-input-number v-model="applyForm.durationHours" :min="0.5" :precision="1" :step="0.5" />
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
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
import {
  getMyAppliesApi,
  getPendingAppliesApi,
  createApplyApi,
  approveApplyApi,
  rejectApplyApi,
  cancelApplyApi
} from '@/api/flow'
import { recheckApi } from '@/api/attendance'

const activeTab = ref('my')
const loadingMy = ref(false)
const loadingPending = ref(false)
const myApplies = ref([])
const pendingApplies = ref([])

// 申请弹窗
const applyDialogVisible = ref(false)
const submitting = ref(false)
const applyFormRef = ref(null)

const applyForm = reactive({
  applyType: 'LEAVE',
  title: '',
  reason: '',
  timeRange: [],
  startTime: '',
  endTime: '',
  durationHours: 1,
  correctionType: 'CHECK_IN',
  correctionTime: ''
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

// 审批处理弹窗
const actionDialogVisible = ref(false)
const actionType = ref('APPROVE') // APPROVE or REJECT
const currentTargetRow = ref(null)
const actionComment = ref('')
const submittingAction = ref(false)

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

const handleTabClick = (tab) => {
  if (tab.paneName === 'my') {
    loadMyApplies()
  } else if (tab.paneName === 'pending') {
    loadPendingApplies()
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
  applyForm.startTime = ''
  applyForm.endTime = ''
  applyForm.durationHours = 1
  applyDialogVisible.value = true
}

const calculateDuration = (val) => {
  if (val && val.length === 2) {
    applyForm.startTime = val[0]
    applyForm.endTime = val[1]
    const start = new Date(val[0])
    const end = new Date(val[1])
    const diffHours = (end - start) / (1000 * 3600)
    applyForm.durationHours = Math.max(0.5, Math.round(diffHours * 10) / 10)
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
          durationHours: applyForm.durationHours
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
  loadPendingApplies()
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
</style>
