<template>
  <div class="flow-container">
    <!-- 页面标题区 -->
    <div class="page-hero">
      <div class="hero-left">
        <h1 class="hero-title">审批中心</h1>
        <p class="hero-subtitle">提交请假、加班与补卡申请，高效完成团队审批</p>
      </div>
      <div class="hero-actions" v-if="!isAdmin">
        <el-button class="hero-btn hero-btn-leave" @click="openApplyDialog('LEAVE')">
          <el-icon><EditPen /></el-icon>
          <span>请假申请</span>
        </el-button>
        <el-button class="hero-btn hero-btn-overtime" @click="openApplyDialog('OVERTIME')">
          <el-icon><Timer /></el-icon>
          <span>加班申请</span>
        </el-button>
        <el-button class="hero-btn hero-btn-correction" @click="openApplyDialog('CORRECTION')">
          <el-icon><Calendar /></el-icon>
          <span>补卡申请</span>
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div v-if="!isAdmin" class="stat-card" :class="{ 'is-active': activeTab === 'my' }" @click="activeTab='my';handleTabClick({paneName:'my'})">
        <div class="stat-icon stat-icon-my">
          <el-icon size="20"><Document /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-num">{{ myApplies.length }}</span>
          <span class="stat-label">我的申请</span>
        </div>
      </div>
      <div v-if="canHandleApproval" class="stat-card" :class="{ 'is-active': activeTab === 'pending' }" @click="activeTab='pending';handleTabClick({paneName:'pending'})">
        <div class="stat-icon stat-icon-pending">
          <el-icon size="20"><Clock /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-num">{{ pendingApplies.length }}</span>
          <span class="stat-label">待我审批</span>
        </div>
      </div>
      <div v-if="canHandleApproval" class="stat-card" :class="{ 'is-active': activeTab === 'processed' }" @click="activeTab='processed';handleTabClick({paneName:'processed'})">
        <div class="stat-icon stat-icon-done">
          <el-icon size="20"><Check /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-num">{{ processedApplies.length }}</span>
          <span class="stat-label">已办审批</span>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="content-card">
      <el-tabs v-model="activeTab" class="flow-tabs" @tab-click="handleTabClick">
        <!-- 我的申请 -->
        <el-tab-pane v-if="!isAdmin" label="我的申请单据" name="my">
          <div class="tab-panel">
            <div class="panel-search">
              <div class="search-input-wrap">
                <el-icon class="search-icon"><Search /></el-icon>
                <input
                  v-model="searchKeyword"
                  class="search-native"
                  placeholder="搜索标题或申请事由..."
                  @keyup.enter="handleSearch"
                />
                <el-button v-if="searchKeyword" class="search-clear" link @click="searchKeyword='';handleSearch()">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
              <div class="search-filters">
                <el-select v-model="searchApplyType" placeholder="申请类型" clearable style="width:130px" @change="handleSearch">
                  <el-option label="请假" value="LEAVE" />
                  <el-option label="加班" value="OVERTIME" />
                  <el-option label="补卡" value="CORRECTION" />
                </el-select>
                <el-select v-model="searchStatus" placeholder="审批状态" clearable style="width:130px" @change="handleSearch">
                  <el-option label="待审批" value="PENDING" />
                  <el-option label="已同意" value="APPROVED" />
                  <el-option label="已驳回" value="REJECTED" />
                  <el-option label="已撤销" value="CANCELED" />
                </el-select>
                <el-button type="primary" @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
                <el-button @click="resetSearch">重置</el-button>
              </div>
            </div>

            <div v-if="!loadingMy && myApplies.length === 0" class="empty-block">
              <el-empty description="暂无申请记录">
                <el-button type="primary" @click="openApplyDialog('LEAVE')">发起申请</el-button>
              </el-empty>
            </div>

            <el-table v-else :data="myApplies" v-loading="loadingMy" class="clean-table" row-class-name="table-row">
              <el-table-column prop="applyNo" label="申请单号" width="180">
                <template #default="{ row }">
                  <span class="apply-no">{{ row.applyNo }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="applyType" label="类型" width="100">
                <template #default="{ row }">
                  <el-tag :type="getTypeTagType(row.applyType)" size="small" effect="dark" round>
                    {{ formatApplyType(row.applyType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="标题" min-width="150">
                <template #default="{ row }">
                  <span class="col-title">{{ row.title }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="申请事由" min-width="180" show-overflow-tooltip />
              <el-table-column prop="durationHours" label="时长" width="100">
                <template #default="{ row }">
                  <span class="col-duration">
                    {{ row.applyType === 'LEAVE' ? (row.durationHours / 8) + ' 天' : (row.durationHours || 0) + 'h' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)" size="small" round>
                    {{ formatStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="提交时间" width="170" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openDetailDialog(row)">详情</el-button>
                  <el-button v-if="row.status === 'PENDING'" link type="danger" size="small" @click="handleCancel(row.id)">撤销</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 待我审批 -->
        <el-tab-pane v-if="canHandleApproval" label="待我审批" name="pending">
          <div class="tab-panel">
            <div v-if="!loadingPending && pendingApplies.length === 0" class="empty-block">
              <el-empty description="暂无待审批单据" />
            </div>

            <el-table v-else :data="pendingApplies" v-loading="loadingPending" class="clean-table" row-class-name="table-row">
              <el-table-column prop="applyNo" label="申请单号" width="180">
                <template #default="{ row }">
                  <span class="apply-no">{{ row.applyNo }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="applicantName" label="申请人" width="100" />
              <el-table-column prop="applicantDeptName" label="部门" width="110" />
              <el-table-column prop="applyType" label="类型" width="100">
                <template #default="{ row }">
                  <el-tag :type="getTypeTagType(row.applyType)" size="small" effect="dark" round>
                    {{ formatApplyType(row.applyType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="标题" min-width="150">
                <template #default="{ row }">
                  <span class="col-title">{{ row.title }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="申请事由" min-width="160" show-overflow-tooltip />
              <el-table-column prop="durationHours" label="时长" width="90">
                <template #default="{ row }">
                  <span class="col-duration">
                    {{ row.applyType === 'LEAVE' ? (row.durationHours / 8) + 'd' : (row.durationHours || 0) + 'h' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="提交时间" width="170" />
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="{ row }">
                  <el-button class="action-sm" @click="openDetailDialog(row)">详情</el-button>
                  <el-button class="action-sm action-approve" @click="handleApprove(row)">同意</el-button>
                  <el-button class="action-sm action-reject" @click="handleReject(row)">驳回</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 已办审批 -->
        <el-tab-pane v-if="canHandleApproval" label="已办审批" name="processed">
          <div class="tab-panel">
            <div v-if="!loadingProcessed && processedApplies.length === 0" class="empty-block">
              <el-empty description="暂无已处理的单据" />
            </div>

            <el-table v-else :data="processedApplies" v-loading="loadingProcessed" class="clean-table" row-class-name="table-row">
              <el-table-column prop="applyNo" label="申请单号" width="180">
                <template #default="{ row }">
                  <span class="apply-no">{{ row.applyNo }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="applicantName" label="申请人" width="100" />
              <el-table-column prop="applyType" label="类型" width="100">
                <template #default="{ row }">
                  <el-tag :type="getTypeTagType(row.applyType)" size="small" effect="dark" round>
                    {{ formatApplyType(row.applyType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="col-title">{{ row.title }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="最终状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)" size="small" round>
                    {{ formatStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="myAction" label="我的动作" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.myAction === 'APPROVE' ? 'success' : 'danger'" size="small" effect="plain" round>
                    {{ row.myAction === 'APPROVE' ? '同意' : '驳回' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="approvedAt" label="处理时间" width="170" />
              <el-table-column label="操作" width="80" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openDetailDialog(row)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 申请弹窗 -->
    <el-dialog
      v-model="applyDialogVisible"
      width="580px"
      class="apply-dialog"
      destroy-on-close
    >
      <template #header>
        <div class="apply-dialog-header">
          <span class="apply-dialog-title">{{ applyDialogTitle }}</span>
          <el-tag :type="getTypeTagType(applyForm.applyType)" effect="dark" round size="default">
            {{ formatApplyType(applyForm.applyType) }}
          </el-tag>
        </div>
      </template>

      <el-form :model="applyForm" ref="applyFormRef" :rules="applyRules" label-width="0" class="apply-form">
        <div class="form-section">
          <div class="form-section-label">基本信息</div>
          <el-form-item prop="title">
            <el-input v-model="applyForm.title" placeholder="请输入申请标题" size="large" />
          </el-form-item>
        </div>

        <!-- 补卡 -->
        <template v-if="applyForm.applyType === 'CORRECTION'">
          <div class="form-section">
            <div class="form-section-label">补卡设置</div>
            <el-form-item prop="correctionType">
              <div class="radio-card-group">
                <div
                  class="radio-card"
                  :class="{ active: applyForm.correctionType === 'CHECK_IN' }"
                  @click="applyForm.correctionType = 'CHECK_IN'"
                >
                  <div class="radio-card-icon" style="background:#eff6ff;color:#3b82f6;">
                    <el-icon size="18"><Upload /></el-icon>
                  </div>
                  <span>上班打卡补卡</span>
                </div>
                <div
                  class="radio-card"
                  :class="{ active: applyForm.correctionType === 'CHECK_OUT' }"
                  @click="applyForm.correctionType = 'CHECK_OUT'"
                >
                  <div class="radio-card-icon" style="background:#fef3c7;color:#d97706;">
                    <el-icon size="18"><Download /></el-icon>
                  </div>
                  <span>下班打卡补卡</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item prop="correctionTime">
              <el-date-picker
                v-model="applyForm.correctionTime"
                type="datetime"
                placeholder="选择拟补打卡时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width:100%"
                size="large"
              />
            </el-form-item>
          </div>
        </template>

        <!-- 加班 -->
        <template v-else-if="applyForm.applyType === 'OVERTIME'">
          <div class="form-section">
            <div class="form-section-label">加班时间</div>
            <div class="time-card-row">
              <div class="time-card">
                <div class="time-card-icon" style="background:#f0fdf4;color:#22c55e;">
                  <el-icon size="16"><VideoPlay /></el-icon>
                </div>
                <div class="time-card-body">
                  <span class="time-card-label">开始时间</span>
                  <span class="time-card-val">{{ applyForm.startTime }}</span>
                </div>
              </div>
              <div class="time-arrow">
                <el-icon size="20"><ArrowRight /></el-icon>
              </div>
              <div class="time-card">
                <div class="time-card-icon" style="background:#fef2f2;color:#ef4444;">
                  <el-icon size="16"><Close /></el-icon>
                </div>
                <div class="time-card-body">
                  <span class="time-card-label">结束时间</span>
                  <span class="time-card-val" :class="{ muted: !applyForm.endTime }">{{ applyForm.endTime || '待选择' }}</span>
                </div>
              </div>
            </div>
            <el-form-item prop="endTime" style="margin-top: 16px;">
              <el-date-picker
                v-model="applyForm.endTime"
                type="datetime"
                placeholder="选择结束时间（仅限今日）"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width:100%"
                size="large"
                :disabled-date="disabledOvertimeDate"
                @change="calculateOvertimeDuration"
              />
            </el-form-item>
            <div class="duration-badge" v-if="applyForm.durationHours > 0">
              <span>预计加班时长</span>
              <strong>{{ applyForm.durationHours }} 小时</strong>
            </div>
          </div>
        </template>

        <!-- 请假 -->
        <template v-else>
          <div class="form-section">
            <div class="form-section-label">请假时间</div>
            <el-form-item prop="timeRange">
              <el-date-picker
                v-model="applyForm.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width:100%"
                size="large"
                :disabled-date="disabledApplyDate"
                @change="calculateDuration"
              />
            </el-form-item>
            <div class="duration-badge" v-if="applyForm.durationHours > 0">
              <span>累计时长</span>
              <strong>{{ applyForm.applyType === 'LEAVE' ? (applyForm.durationHours / 8) + ' 天' : applyForm.durationHours + ' 小时' }}</strong>
            </div>
          </div>
        </template>

        <div class="form-section">
          <div class="form-section-label">补充说明</div>
          <el-form-item prop="reason">
            <el-input
              v-model="applyForm.reason"
              type="textarea"
              :rows="3"
              placeholder="详细说明申请原因"
            />
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="form-section-label">附件 <span class="label-optional">(可选)</span></div>
          <el-upload
            :http-request="customFlowUpload"
            :file-list="flowFileList"
            :on-remove="removeFlowFile"
            list-type="text"
            multiple
          >
            <div class="upload-trigger">
              <el-icon size="20"><Upload /></el-icon>
              <span>点击上传附件</span>
            </div>
            <template #tip>
              <div class="upload-tip">支持图片、PDF、Word 等格式，单文件不超过10MB</div>
            </template>
          </el-upload>
        </div>
      </el-form>

      <template #footer>
        <el-button size="large" @click="applyDialogVisible = false">取消</el-button>
        <el-button size="large" type="primary" :loading="submitting" @click="submitApply">确认提交</el-button>
      </template>
    </el-dialog>

    <!-- 审批意见弹窗 -->
    <el-dialog
      v-model="actionDialogVisible"
      :title="actionType === 'APPROVE' ? '同意审批' : '驳回审批'"
      width="460px"
      class="action-dialog"
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
      width="620px"
      class="detail-dialog"
      destroy-on-close
    >
      <template v-if="currentDetailRow">
        <!-- 基本信息卡片 -->
        <div class="detail-card info-card">
          <div class="info-grid">
            <div class="info-cell">
              <span class="info-label">申请单号</span>
              <span class="info-val">{{ currentDetailRow.applyNo }}</span>
            </div>
            <div class="info-cell">
              <span class="info-label">申请类型</span>
              <el-tag :type="getTypeTagType(currentDetailRow.applyType)" size="small" effect="dark" round>
                {{ formatApplyType(currentDetailRow.applyType) }}
              </el-tag>
            </div>
            <div class="info-cell">
              <span class="info-label">申请人</span>
              <span class="info-val">{{ currentDetailRow.applicantName || '-' }}</span>
            </div>
            <div class="info-cell" v-if="currentDetailRow.applicantDeptName">
              <span class="info-label">所属部门</span>
              <span class="info-val">{{ currentDetailRow.applicantDeptName }}</span>
            </div>
            <div class="info-cell">
              <span class="info-label">当前状态</span>
              <el-tag :type="getStatusTagType(currentDetailRow.status)" size="small" round>
                {{ formatStatusText(currentDetailRow.status) }}
              </el-tag>
            </div>
            <div class="info-cell" v-if="currentDetailRow.approverName">
              <span class="info-label">审批人</span>
              <span class="info-val">{{ currentDetailRow.approverName }}</span>
            </div>
            <div class="info-cell">
              <span class="info-label">提交时间</span>
              <span class="info-val">{{ currentDetailRow.createdAt || '-' }}</span>
            </div>
            <div class="info-cell info-cell-full">
              <span class="info-label">标题</span>
              <span class="info-val">{{ currentDetailRow.title }}</span>
            </div>
            <div class="info-cell info-cell-full">
              <span class="info-label">事由</span>
              <span class="info-val">{{ currentDetailRow.reason }}</span>
            </div>
            <div class="info-cell info-cell-full" v-if="currentDetailRow.startTime">
              <span class="info-label">时间范围</span>
              <span class="info-val">{{ currentDetailRow.startTime }} ~ {{ currentDetailRow.endTime }}</span>
            </div>
          </div>
        </div>

        <!-- 审批流程 -->
        <div class="detail-section">
          <div class="section-header">
            <span class="section-title">审批流程</span>
          </div>
          <div v-if="currentDetailRow.approveRecords && currentDetailRow.approveRecords.length > 0" class="timeline">
            <div
              v-for="(record, idx) in currentDetailRow.approveRecords"
              :key="idx"
              class="tl-item"
            >
              <div class="tl-dot" :class="record.action === 'APPROVE' ? 'dot-approve' : record.action === 'REJECT' ? 'dot-reject' : 'dot-submit'">
                <el-icon v-if="record.action === 'APPROVE'"><Check /></el-icon>
                <el-icon v-else-if="record.action === 'REJECT'"><Close /></el-icon>
                <el-icon v-else><Edit /></el-icon>
              </div>
              <div class="tl-body">
                <div class="tl-head">
                  <strong>{{ record.approverName || record.applicantName || '系统' }}</strong>
                  <el-tag
                    :type="record.action === 'APPROVE' ? 'success' : record.action === 'REJECT' ? 'danger' : 'primary'"
                    size="small"
                    effect="light"
                    round
                  >
                    {{ record.action === 'APPROVE' ? '审批通过' : record.action === 'REJECT' ? '审批驳回' : '提交申请' }}
                  </el-tag>
                </div>
                <div class="tl-comment" v-if="record.comment">{{ record.comment }}</div>
                <div class="tl-time">{{ record.approvedAt || record.createdAt }}</div>
              </div>
            </div>
          </div>
          <div v-else class="empty-block"><el-empty description="暂无审批记录" :image-size="60" /></div>
        </div>

        <!-- 附件 -->
        <div v-if="currentDetailRow.attachments && currentDetailRow.attachments.length > 0" class="detail-section">
          <div class="section-header">
            <span class="section-title">附件</span>
          </div>
          <div class="attach-list">
            <div v-for="att in currentDetailRow.attachments" :key="att.id" class="attach-item" @click="downloadFile('/flow/attachments/' + att.id + '/download', att.fileName)">
              <div class="attach-icon">
                <el-icon size="20"><Document /></el-icon>
              </div>
              <span class="attach-name">{{ att.fileName }}</span>
              <el-icon class="attach-download-icon"><Download /></el-icon>
            </div>
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
import { Search, Close, Upload, Download, Check, Clock, Calendar, Timer, Document, Edit, EditPen, VideoPlay, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
import {
  createApplyApi,
  getMyAppliesApi,
  searchAppliesApi,
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
import { downloadFile } from '@/utils/download'

const userStore = useUserStore()
const activeTab = ref('my')

const isAdmin = computed(() => userStore.hasRole('ADMIN'))

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

const myApplies = ref([])
const pendingApplies = ref([])
const processedApplies = ref([])

const loadingMy = ref(false)
const loadingPending = ref(false)
const loadingProcessed = ref(false)

const searchKeyword = ref('')
const searchApplyType = ref('')
const searchStatus = ref('')

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
    return time.getTime() < new Date().setHours(0, 0, 0, 0)
  }
  return false
}

const disabledOvertimeDate = (time) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const tomorrow = new Date(today)
  tomorrow.setDate(tomorrow.getDate() + 1)
  return time.getTime() < today.getTime() || time.getTime() >= tomorrow.getTime()
}

const actionDialogVisible = ref(false)
const actionType = ref('APPROVE')
const currentTargetRow = ref(null)
const actionComment = ref('')
const submittingAction = ref(false)

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

const buildSearchParams = () => {
  const params = { pageNum: 1, pageSize: 50 }
  if (searchKeyword.value) params.keyword = searchKeyword.value
  if (searchApplyType.value) params.applyType = searchApplyType.value
  if (searchStatus.value) params.status = searchStatus.value
  return params
}

const loadMyApplies = async () => {
  loadingMy.value = true
  try {
    const hasFilter = searchKeyword.value || searchApplyType.value || searchStatus.value
    const api = hasFilter ? searchAppliesApi : getMyAppliesApi
    const params = hasFilter ? buildSearchParams() : { pageNum: 1, pageSize: 50 }
    const res = await api(params)
    if (res.code === 200 && res.data) {
      myApplies.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载我的申请失败', error)
  } finally {
    loadingMy.value = false
  }
}

const handleSearch = () => {
  loadMyApplies()
}

const resetSearch = () => {
  searchKeyword.value = ''
  searchApplyType.value = ''
  searchStatus.value = ''
  loadMyApplies()
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
  if (isAdmin.value) {
    activeTab.value = 'pending'
  } else {
    loadMyApplies()
  }
  if (canHandleApproval.value) {
    loadPendingApplies()
    loadProcessedApplies()
  }
})
</script>

<style scoped>
/* ====== 容器 ====== */
.flow-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* ====== 顶部标题区 ====== */
.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 24px;
  flex-wrap: wrap;
}
.hero-left {
  flex: 1;
  min-width: 200px;
}
.hero-title {
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 4px 0;
  letter-spacing: -0.5px;
}
.hero-subtitle {
  color: #64748b;
  margin: 0;
  font-size: 14px;
}
.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.hero-btn {
  height: 44px;
  padding: 0 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  display: flex;
  align-items: center;
  gap: 7px;
  transition: all 0.2s;
}
.hero-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.hero-btn-leave {
  background: #eff6ff;
  color: #2563eb;
}
.hero-btn-leave:hover { background: #dbeafe; }
.hero-btn-overtime {
  background: #f0fdf4;
  color: #16a34a;
}
.hero-btn-overtime:hover { background: #dcfce7; }
.hero-btn-correction {
  background: #fffbeb;
  color: #d97706;
}
.hero-btn-correction:hover { background: #fef3c7; }

/* ====== 统计卡片 ====== */
.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  flex: 1;
  min-width: 160px;
  background: #fff;
  border-radius: 14px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 2px solid transparent;
}
.stat-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}
.stat-card.is-active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 3px rgba(64,158,255,0.08);
}
.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon-my { background: #eff6ff; color: #3b82f6; }
.stat-icon-pending { background: #fffbeb; color: #f59e0b; }
.stat-icon-done { background: #f0fdf4; color: #22c55e; }
.stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}
.stat-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

/* ====== 内容卡片 ====== */
.content-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  overflow: hidden;
}
.flow-tabs {
  padding: 0 24px 24px;
}
.flow-tabs :deep(.el-tabs__header) {
  margin: 0 0 20px 0;
  padding: 0 24px;
  background: #fff;
}
.flow-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}
.flow-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  height: 48px;
  line-height: 48px;
}
.flow-tabs :deep(.el-tabs__item.is-active) {
  color: #0f172a;
  font-weight: 700;
}

.tab-panel {
  margin-top: 0;
}

/* ====== 搜索栏 ====== */
.panel-search {
  margin-bottom: 20px;
}
.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 16px;
  pointer-events: none;
  z-index: 1;
}
.search-native {
  width: 100%;
  height: 44px;
  padding: 0 40px 0 40px;
  border: 2px solid #e2e8f0;
  border-radius: 11px;
  font-size: 14px;
  color: #1e293b;
  background: #f8fafc;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}
.search-native::placeholder { color: #94a3b8; }
.search-native:focus {
  border-color: var(--el-color-primary);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(64,158,255,0.06);
}
.search-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
}
.search-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

/* ====== 表格 ====== */
.clean-table {
  border-radius: 12px;
  overflow: hidden;
}
.clean-table :deep(.el-table__header) th {
  background: #f8fafc !important;
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
  border-bottom: 1px solid #e2e8f0;
  padding: 14px 0;
}
.clean-table :deep(.el-table__body) td {
  border-bottom: 1px solid #f1f5f9;
  padding: 14px 0;
  font-size: 14px;
  color: #334155;
}
.clean-table :deep(.el-table__body) tr:hover > td {
  background: #f8fafc;
}
.clean-table :deep(.el-table__body) tr:last-child td {
  border-bottom: none;
}
.clean-table::before {
  display: none;
}
.clean-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}
.apply-no {
  font-family: 'SF Mono', 'Menlo', monospace;
  font-size: 13px;
  color: #64748b;
  letter-spacing: -0.3px;
}
.col-title {
  font-weight: 500;
  color: #1e293b;
}
.col-duration {
  font-weight: 600;
  color: #0f172a;
}

/* 操作按钮 */
.action-sm {
  height: 30px;
  padding: 0 12px;
  font-size: 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}
.action-sm:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}
.action-approve {
  border-color: #bbf7d0;
  color: #16a34a;
  background: #f0fdf4;
}
.action-approve:hover {
  border-color: #86efac;
  background: #dcfce7;
}
.action-reject {
  border-color: #fecaca;
  color: #dc2626;
  background: #fef2f2;
}
.action-reject:hover {
  border-color: #fca5a5;
  background: #fee2e2;
}

/* ====== 空状态 ====== */
.empty-block {
  padding: 40px 0;
  display: flex;
  justify-content: center;
}

/* ====== 申请弹窗 ====== */
/* ====== 申请弹窗 ====== */
.apply-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  padding: 20px 24px 16px;
}
.apply-dialog :deep(.el-dialog__body) {
  padding: 16px 24px 24px;
}
.apply-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid #f1f5f9;
  padding: 16px 24px;
}
.apply-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.apply-dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

/* 表单分区 */
.form-section {
  margin-bottom: 20px;
}
.form-section:last-child {
  margin-bottom: 0;
}
.form-section-label {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.label-optional {
  font-weight: 400;
  color: #94a3b8;
  font-size: 12px;
}
.apply-form .el-form-item {
  margin-bottom: 14px;
}
.apply-form .el-form-item:last-child {
  margin-bottom: 0;
}

/* 补卡类型卡片选择 */
.radio-card-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: 100%;
}
.radio-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}
.radio-card:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}
.radio-card.active {
  border-color: var(--el-color-primary);
  background: #eff6ff;
  color: #1e293b;
}
.radio-card-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 时间卡片 */
.time-card-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}
.time-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}
.time-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.time-card-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.time-card-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}
.time-card-val {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
}
.time-card-val.muted {
  color: #94a3b8;
  font-weight: 400;
}
.time-arrow {
  color: #94a3b8;
  flex-shrink: 0;
  padding-top: 10px;
}

/* 时长徽章 */
.duration-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f0fdf4;
  border-radius: 10px;
  margin-top: 16px;
  font-size: 13px;
  color: #16a34a;
}
.duration-badge strong {
  font-size: 16px;
  font-weight: 700;
}

/* 上传 */
.upload-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px;
  border: 2px dashed #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: #64748b;
  font-size: 14px;
  justify-content: center;
}
.upload-trigger:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  background: #f8fafc;
}
.upload-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 6px;
}

/* ====== 审批意见弹窗 ====== */
.action-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  padding: 20px 24px 16px;
}
.action-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid #f1f5f9;
  padding: 16px 24px;
}

/* ====== 详情弹窗 ====== */
.detail-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  padding: 20px 24px 16px;
}
.detail-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}
.detail-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid #f1f5f9;
  padding: 16px 24px;
}

.detail-card {
  background: #f8fafc;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 24px;
}
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 24px;
}
.info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.info-cell-full {
  grid-column: 1 / -1;
}
.info-label {
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
}
.info-val {
  font-size: 14px;
  color: #334155;
  word-break: break-word;
}

.detail-section {
  margin-bottom: 24px;
}
.detail-section:last-child {
  margin-bottom: 0;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
}
.section-header::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

/* 时间线 */
.timeline {
  padding: 4px 0;
}
.tl-item {
  display: flex;
  gap: 14px;
  padding-bottom: 20px;
  position: relative;
}
.tl-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 15px;
  top: 34px;
  bottom: 0;
  width: 2px;
  background: #e2e8f0;
}
.tl-dot {
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
.tl-dot.dot-approve { background: #22c55e; }
.tl-dot.dot-reject { background: #ef4444; }
.tl-dot.dot-submit { background: #3b82f6; }
.tl-body {
  flex: 1;
  background: #f8fafc;
  padding: 12px 16px;
  border-radius: 10px;
}
.tl-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.tl-comment {
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}
.tl-time {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 6px;
}

/* 附件 */
.attach-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.attach-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.attach-item:hover { background: #f1f5f9; }
.attach-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #e2e8f0;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.attach-download-icon {
  margin-left: auto;
  color: #94a3b8;
  font-size: 16px;
  flex-shrink: 0;
}
.attach-item:hover .attach-download-icon { color: #3b82f6; }
.attach-name {
  font-size: 13px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
