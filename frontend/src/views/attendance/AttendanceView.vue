<template>
  <div class="attendance-page">
    <!-- 顶部 View 模式切换 Segment -->
    <div v-if="isManager || isAdmin" class="mode-switch-card">
      <el-radio-group v-model="activeTab" size="large">
        <el-radio-button label="my">
          <el-icon><Clock /></el-icon> 个人打卡工作台
        </el-radio-button>
        <el-radio-button v-if="isManager || isAdmin" label="dept">
          <el-icon><DataAnalysis /></el-icon> 部门今日考勤实时监控
        </el-radio-button>
        <el-radio-button v-if="isAdmin" label="rule">
          <el-icon><Setting /></el-icon> 考勤规则与部门绑定 (管理员)
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 模式 1：个人打卡工作台 -->
    <template v-if="activeTab === 'my'">
      <!-- 顶部概览 Banner (白色简约风格) -->
      <div class="overview-card">
        <div class="clock-box">
          <div class="live-clock">{{ currentTime }}</div>
          <div class="date-info">
            <span class="date-text">{{ currentDate }}</span>
            <el-tag size="small" type="primary" effect="plain" class="day-tag">{{ currentDayOfWeek }}</el-tag>
          </div>
        </div>

        <div class="metrics-grid">
          <div class="metric-item">
            <div class="metric-title">本月出勤</div>
            <div class="metric-num primary-num">{{ pagination.total }} <small>天</small></div>
          </div>
          <div class="metric-item">
            <div class="metric-title">正常率</div>
            <div class="metric-num success-num">{{ normalRate }}%</div>
          </div>
          <div class="metric-item">
            <div class="metric-title">迟到/早退</div>
            <div class="metric-num warning-num">{{ lateOrEarlyCount }} <small>次</small></div>
          </div>
        </div>
      </div>

      <!-- 中间打卡核心操作区 -->
      <el-row :gutter="20" class="action-row">
        <!-- 上班打卡卡片 -->
        <el-col :xs="24" :sm="12">
          <div class="punch-card" :class="{ 'punch-done': todayStatus.hasCheckIn }">
            <div class="card-head">
              <div class="head-title">
                <el-icon class="icon-sun"><Sunrise /></el-icon>
                <span>上班打卡</span>
                <span class="target-time">规定时间 09:00</span>
              </div>
              <el-tag v-if="todayStatus.hasCheckIn" :type="todayStatus.lateMinutes > 0 ? 'danger' : 'success'" effect="light" round>
                {{ todayStatus.lateMinutes > 0 ? `迟到 ${formatMinutes(todayStatus.lateMinutes)}` : '按时打卡' }}
              </el-tag>
              <el-tag v-else type="info" effect="plain" round>未打卡</el-tag>
            </div>

            <div class="card-body">
              <div v-if="todayStatus.hasCheckIn" class="record-detail">
                <span class="detail-label">实际打卡时间：</span>
                <span class="detail-time success-text">{{ formatTime(todayStatus.checkInTime) }}</span>
              </div>
              <div v-else class="record-detail">
                <span class="detail-label">打卡规则：</span>
                <span class="detail-hint">允许 10 分钟弹性时间（09:10 前不计迟到）</span>
              </div>

              <el-button
                type="primary"
                size="large"
                class="action-button"
                :disabled="todayStatus.hasCheckIn"
                :loading="loading"
                @click="openCheckDialog('checkIn')"
              >
                <el-icon><Clock /></el-icon>
                <span>{{ todayStatus.hasCheckIn ? '上班已打卡' : '立即打上班卡 (09:00)' }}</span>
              </el-button>
            </div>
          </div>
        </el-col>

        <!-- 下班打卡卡片 -->
        <el-col :xs="24" :sm="12">
          <div class="punch-card" :class="{ 'punch-done': todayStatus.hasCheckOut }">
            <div class="card-head">
              <div class="head-title">
                <el-icon class="icon-moon"><Sunset /></el-icon>
                <span>下班打卡</span>
                <span class="target-time">规定时间 18:00</span>
              </div>
              <el-tag v-if="todayStatus.hasCheckOut" :type="todayStatus.earlyLeaveMinutes > 0 ? 'warning' : 'success'" effect="light" round>
                {{ todayStatus.earlyLeaveMinutes > 0 ? `早退 ${todayStatus.earlyLeaveMinutes} 分钟` : '正常下班' }}
              </el-tag>
              <el-tag v-else type="info" effect="plain" round>未打卡</el-tag>
            </div>

            <div class="card-body">
              <div v-if="todayStatus.hasCheckOut" class="record-detail">
                <span class="detail-label">实际打卡时间：</span>
                <span class="detail-time warning-text">
                  {{ formatTime(todayStatus.checkOutTime) }}
                  <small v-if="todayStatus.workMinutes > 0">(共工时 {{ formatMinutes(todayStatus.workMinutes) }})</small>
                </span>
              </div>
              <div v-else class="record-detail">
                <span class="detail-label">打卡规则：</span>
                <span class="detail-hint">满 9 小时算完整勤务</span>
              </div>

              <el-button
                type="success"
                size="large"
                class="action-button"
                :disabled="!todayStatus.hasCheckIn || todayStatus.hasCheckOut"
                :loading="loading"
                @click="openCheckDialog('checkOut')"
              >
                <el-icon><Check /></el-icon>
                <span>{{ todayStatus.hasCheckOut ? '下班已打卡' : (todayStatus.hasCheckIn ? '立即打下班卡 (18:00)' : '请先完成上班打卡') }}</span>
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 底部打卡记录表格卡片 -->
      <div class="table-card">
        <div class="table-card-header">
          <div class="header-title-group">
            <el-icon class="header-icon"><Calendar /></el-icon>
            <h2>个人考勤历史记录</h2>
          </div>
          <div class="header-filter-group">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              size="default"
              style="width: 240px;"
              @change="fetchMyRecords"
            />
            <el-button type="primary" plain size="default" @click="fetchMyRecords">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>

        <el-table :data="records" stripe border style="width: 100%" v-loading="tableLoading">
          <el-table-column prop="workDate" label="考勤日期" width="140" align="center">
            <template #default="{ row }">
              <span class="date-column">{{ row.workDate }}</span>
            </template>
          </el-table-column>

          <el-table-column label="上班打卡时间" width="180" align="center">
            <template #default="{ row }">
              <span v-if="row.checkInTime" class="time-text success-text">{{ formatTime(row.checkInTime) }}</span>
              <el-tag v-else type="info" size="small">未打卡</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="下班打卡时间" width="180" align="center">
            <template #default="{ row }">
              <span v-if="row.checkOutTime" class="time-text warning-text">{{ formatTime(row.checkOutTime) }}</span>
              <el-tag v-else type="info" size="small">未打卡</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="当日工时" width="140" align="center">
            <template #default="{ row }">
              <span v-if="row.workMinutes">{{ formatMinutes(row.workMinutes) }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" min-width="140" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" effect="light">
                {{ formatStatusText(row.status, row.lateMinutes, row.earlyLeaveMinutes) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="checkInRemark" label="打卡备注" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.checkInRemark || row.checkOutRemark || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="130" align="center" fixed="right">
            <template #default="{ row }">
              <el-tag v-if="row.correctionStatus === 'PENDING'" type="warning" effect="dark" size="small">
                <el-icon class="el-icon--left"><Clock /></el-icon>补卡审核中
              </el-tag>
              <el-tag v-else-if="row.correctionStatus === 'REJECTED'" type="danger" effect="plain" size="small">
                <el-icon class="el-icon--left"><CircleClose /></el-icon>补卡已驳回
              </el-tag>
              <el-button
                v-else-if="row.status === 'LATE' || row.status === 'EARLY_LEAVE' || row.status === 'LATE_AND_EARLY' || row.status === 'ABSENT' || row.status === 'MISSING_CARD'"
                type="warning"
                plain
                size="small"
                icon="Edit"
                @click="openCorrectionDialog(row)"
              >
                申请补卡
              </el-button>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="pagination.total"
            @size-change="fetchMyRecords"
            @current-change="fetchMyRecords"
          />
        </div>
      </div>
    </template>

    <!-- 模式 2：部门今日考勤实时监控 -->
    <template v-else-if="activeTab === 'dept'">
      <div class="overview-card">
        <div class="clock-box">
          <div class="live-clock">部门今日考勤看板</div>
          <div class="date-info">
            <span class="date-text">实时监控日期：{{ deptOverview.todayDate || currentDate }}</span>
          </div>
        </div>

        <div class="metrics-grid">
          <div class="metric-item">
            <div class="metric-title">部门总人数</div>
            <div class="metric-num primary-num">{{ deptOverview.totalUsers || 0 }} <small>人</small></div>
          </div>
          <div class="metric-item">
            <div class="metric-title">已打卡人数</div>
            <div class="metric-num success-num">{{ deptOverview.checkedInUsers || 0 }} <small>人</small></div>
          </div>
          <div class="metric-item">
            <div class="metric-title">迟到人数</div>
            <div class="metric-num warning-num">{{ deptOverview.lateUsers || 0 }} <small>人</small></div>
          </div>
          <div class="metric-item">
            <div class="metric-title">未打卡人数</div>
            <div class="metric-num danger-num">{{ deptOverview.notCheckedUsers || 0 }} <small>人</small></div>
          </div>
        </div>
      </div>

      <div class="table-card">
        <div class="table-card-header">
          <div class="header-title-group">
            <el-icon class="header-icon"><DataAnalysis /></el-icon>
            <h2>部门员工今日考勤明细</h2>
          </div>
          <div class="header-filter-group">
            <el-button type="primary" plain size="default" @click="fetchDeptOverview">
              <el-icon><Refresh /></el-icon> 刷新部门数据
            </el-button>
          </div>
        </div>

        <el-table :data="deptOverview.list || []" stripe border style="width: 100%" v-loading="deptLoading">
          <el-table-column prop="realName" label="员工姓名" width="140" align="center">
            <template #default="{ row }">
              <span class="date-column">{{ row.realName }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="username" label="登录账号" width="140" align="center" />

          <el-table-column prop="deptName" label="所属部门" width="160" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.deptName || '研发部' }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="上班打卡时间" width="180" align="center">
            <template #default="{ row }">
              <span v-if="row.checkInTime" class="time-text success-text">{{ formatTime(row.checkInTime) }}</span>
              <el-tag v-else type="info" size="small">未打卡</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="下班打卡时间" width="180" align="center">
            <template #default="{ row }">
              <span v-if="row.checkOutTime" class="time-text warning-text">{{ formatTime(row.checkOutTime) }}</span>
              <el-tag v-else type="info" size="small">未打卡</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="当日工时" width="140" align="center">
            <template #default="{ row }">
              <span v-if="row.workMinutes">{{ formatMinutes(row.workMinutes) }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>

          <el-table-column label="打卡状态" min-width="140" align="center">
            <template #default="{ row }">
              <el-tag :type="getDeptStatusTagType(row.status)" effect="light">
                {{ formatDeptStatusText(row.status, row.lateMinutes) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- 模式 3：考勤规则与部门绑定 (管理员) -->
    <template v-else-if="activeTab === 'rule'">
      <div class="overview-card">
        <div class="clock-box">
          <div class="live-clock">考勤规则与部门绑定设置</div>
          <div class="date-info">
            <span class="date-text">可分别管理考勤班次规则 (`attendance_rule`) 与部门考勤组绑定 (`attendance_group`)</span>
          </div>
        </div>
        <div style="display: flex; gap: 12px;">
          <el-button type="primary" size="large" @click="openRuleDialog(null)">
            <el-icon><Plus /></el-icon> 新增班次规则
          </el-button>
          <el-button type="success" size="large" @click="openGroupDialog(null)">
            <el-icon><Plus /></el-icon> 绑定部门考勤组
          </el-button>
        </div>
      </div>

      <!-- 表格 1：考勤班次规则配置 -->
      <div class="table-card">
        <div class="table-card-header">
          <div class="header-title-group">
            <el-icon class="header-icon"><Setting /></el-icon>
            <h2>1. 考勤班次规则库 (attendance_rule 表)</h2>
          </div>
          <el-button type="primary" plain size="default" @click="fetchRules">
            <el-icon><Refresh /></el-icon> 刷新规则列表
          </el-button>
        </div>

        <el-table :data="ruleList" stripe border style="width: 100%" v-loading="ruleLoading">
          <el-table-column prop="id" label="规则ID" width="90" align="center" />
          <el-table-column prop="ruleName" label="规则/班次名称" min-width="180" align="center">
            <template #default="{ row }">
              <span class="date-column">{{ row.ruleName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="workStartTime" label="规定上班时间" width="140" align="center" />
          <el-table-column prop="workEndTime" label="规定下班时间" width="140" align="center" />
          <el-table-column label="迟到缓冲" width="130" align="center">
            <template #default="{ row }">
              <el-tag type="warning">{{ row.lateThresholdMinutes }} 分钟</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="早退缓冲" width="130" align="center">
            <template #default="{ row }">
              <el-tag type="info">{{ row.earlyLeaveThresholdMinutes }} 分钟</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openRuleDialog(row)">编辑规则</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 表格 2：考勤组与部门绑定管理 -->
      <div class="table-card">
        <div class="table-card-header">
          <div class="header-title-group">
            <el-icon class="header-icon"><DataAnalysis /></el-icon>
            <h2>2. 考勤组与部门绑定管理 (attendance_group 表)</h2>
          </div>
          <el-button type="primary" plain size="default" @click="fetchGroups">
            <el-icon><Refresh /></el-icon> 刷新绑定列表
          </el-button>
        </div>

        <el-table :data="groupList" stripe border style="width: 100%" v-loading="groupLoading">
          <el-table-column prop="id" label="考勤组ID" width="100" align="center" />
          <el-table-column prop="groupName" label="考勤组名称" min-width="180" align="center">
            <template #default="{ row }">
              <span class="date-column">{{ row.groupName }}</span>
            </template>
          </el-table-column>

          <el-table-column label="绑定部门" min-width="160" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.deptName" type="success" effect="light">{{ row.deptName }}</el-tag>
              <el-tag v-else type="info" effect="plain">全公司通用 (默认)</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="应用考勤规则" min-width="200" align="center">
            <template #default="{ row }">
              <span class="font-bold">{{ row.ruleName || `规则 ID: ${row.ruleId}` }}</span>
              <div v-if="row.workStartTime" class="text-muted" style="font-size: 12px;">
                ({{ row.workStartTime }} ~ {{ row.workEndTime }})
              </div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openGroupDialog(row)">编辑绑定</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- 打卡备注弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'checkIn' ? '上班打卡确认' : '下班打卡确认'"
      width="440px"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="当前打卡时间">
          <el-input :value="currentFullTime" readonly />
        </el-form-item>
        <el-form-item label="打卡备注（可选）">
          <el-input
            v-model="remark"
            type="textarea"
            :rows="3"
            placeholder="如有外勤、加班或迟到说明，请在此输入..."
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitCheck">确认打卡</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 管理员规则修改弹窗 -->
    <el-dialog
      v-model="ruleDialogVisible"
      :title="ruleForm.id ? '编辑考勤班次规则' : '新增考勤班次规则'"
      width="500px"
      destroy-on-close
    >
      <el-form :model="ruleForm" label-width="120px">
        <el-form-item label="规则名称" required>
          <el-input v-model="ruleForm.ruleName" placeholder="例如：默认工作日考勤规则" />
        </el-form-item>
        <el-form-item label="规定上班时间" required>
          <el-time-picker v-model="ruleForm.workStartTime" value-format="HH:mm:ss" placeholder="选择时间" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="规定下班时间" required>
          <el-time-picker v-model="ruleForm.workEndTime" value-format="HH:mm:ss" placeholder="选择时间" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="迟到缓冲分钟" required>
          <el-input-number v-model="ruleForm.lateThresholdMinutes" :min="0" :max="60" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="早退缓冲分钟" required>
          <el-input-number v-model="ruleForm.earlyLeaveThresholdMinutes" :min="0" :max="60" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="旷工判定分钟" required>
          <el-input-number v-model="ruleForm.absentThresholdMinutes" :min="60" :max="480" :step="30" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="ruleDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="ruleSubmitting" @click="saveRule">保存配置</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 考勤组与部门绑定修改弹窗 -->
    <el-dialog
      v-model="groupDialogVisible"
      :title="groupForm.id ? '编辑部门考勤组绑定' : '新增部门考勤组绑定'"
      width="500px"
      destroy-on-close
    >
      <el-form :model="groupForm" label-width="120px">
        <el-form-item label="考勤组名称" required>
          <el-input v-model="groupForm.groupName" placeholder="例如：研发部弹性考勤组" />
        </el-form-item>
        <el-form-item label="应用规则" required>
          <el-select v-model="groupForm.ruleId" placeholder="请选择考勤规则" style="width: 100%;">
            <el-option
              v-for="rule in ruleList"
              :key="rule.id"
              :label="`${rule.ruleName} (${rule.workStartTime}~${rule.workEndTime})`"
              :value="rule.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定部门">
          <el-select v-model="groupForm.deptId" placeholder="通用/选填部门" clearable style="width: 100%;">
            <el-option label="总经办" :value="1" />
            <el-option label="研发部" :value="2" />
            <el-option label="人事部" :value="3" />
            <el-option label="行政部" :value="4" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="groupDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="groupSubmitting" @click="saveGroup">保存绑定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 考勤补卡申请对话框 -->
    <el-dialog v-model="correctionDialogVisible" title="发起考勤补卡申请" width="500px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="考勤日期">
          <el-input :value="correctionForm.workDate" disabled />
        </el-form-item>
        <el-form-item label="补卡类型">
          <el-radio-group v-model="correctionForm.correctionType" @change="onCorrectionTypeChange">
            <el-radio label="CHECK_IN">上班打卡补卡</el-radio>
            <el-radio label="CHECK_OUT">下班打卡补卡</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="补打卡时间">
          <el-date-picker
            v-model="correctionForm.correctionTime"
            type="datetime"
            placeholder="选择补打卡精确时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            :disabled-date="disabledCorrectionDate"
          />
        </el-form-item>
        <el-form-item label="补卡原因">
          <el-input
            v-model="correctionForm.reason"
            type="textarea"
            :rows="3"
            placeholder="说明忘记打卡或设备异常的原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="correctionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingCorrection" @click="submitCorrection">提交补卡申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Sunrise, Sunset, Calendar, Check, Refresh, DataAnalysis, Setting, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getTodayStatusApi,
  checkInApi,
  checkOutApi,
  getMyRecordsApi,
  getDeptTodayOverviewApi,
  getRulesApi,
  createRuleApi,
  updateRuleApi,
  getGroupsApi,
  createGroupApi,
  updateGroupApi,
  recheckApi
} from '@/api/attendance'

const userStore = useUserStore()
const activeTab = ref('my')

// 补卡申请弹窗状态
const correctionDialogVisible = ref(false)
const submittingCorrection = ref(false)
const correctionForm = ref({
  attendanceRecordId: null,
  workDate: '',
  correctionType: 'CHECK_IN',
  correctionTime: '',
  reason: ''
})

const disabledCorrectionDate = (time) => {
  if (!correctionForm.value.workDate) return false
  // 将补卡日期严格限制为当天缺卡的日期
  const targetDate = new Date(correctionForm.value.workDate).setHours(0, 0, 0, 0)
  const currentTime = new Date(time).setHours(0, 0, 0, 0)
  return currentTime !== targetDate
}

const openCorrectionDialog = (row) => {
  correctionForm.value.attendanceRecordId = row.id
  correctionForm.value.workDate = row.workDate
  correctionForm.value.correctionType = row.checkInTime ? 'CHECK_OUT' : 'CHECK_IN'
  correctionForm.value.correctionTime = `${row.workDate} ${correctionForm.value.correctionType === 'CHECK_IN' ? '09:00:00' : '18:00:00'}`
  correctionForm.value.reason = ''
  correctionDialogVisible.value = true
}

const submitCorrection = async () => {
  if (!correctionForm.value.correctionTime) {
    ElMessage.warning('请选择补打卡时间')
    return
  }
  if (!correctionForm.value.reason.trim()) {
    ElMessage.warning('请输入补卡原因')
    return
  }
  submittingCorrection.value = true
  try {
    await recheckApi({
      attendanceRecordId: correctionForm.value.attendanceRecordId,
      correctionType: correctionForm.value.correctionType,
      correctionTime: correctionForm.value.correctionTime,
      reason: correctionForm.value.reason
    })
    ElMessage.success('补卡申请提交成功，请等待主管审批！')
    correctionDialogVisible.value = false
    fetchMyRecords()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submittingCorrection.value = false
  }
}

// 角色判定：管理员与主管权限分离
const isAdmin = computed(() => {
  const user = userStore.profile
  if (!user) return false
  const username = user.username || ''
  const userType = user.userType || ''
  const roleCode = user.roleCode || ''
  const roles = user.roles || []
  return username === 'admin' || userType === 'ADMIN' || roleCode === 'ADMIN' || roles.some(r => r.roleCode === 'ADMIN' || r.id === 1)
})

const isManager = computed(() => {
  const user = userStore.profile
  if (!user) return false
  if (isAdmin.value) return true
  const userType = user.userType || ''
  const roleCode = user.roleCode || ''
  const roles = user.roles || []
  return userType === 'MANAGER' || roleCode === 'MANAGER' || roles.some(r => r.roleCode === 'MANAGER')
})

// 时钟与日期
const currentTime = ref('')
const currentDate = ref('')
const currentDayOfWeek = ref('')
const currentFullTime = ref('')
let timer = null

const updateClock = () => {
  const now = new Date()
  currentTime.value = now.toTimeString().split(' ')[0]
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  currentDayOfWeek.value = days[now.getDay()]
  currentFullTime.value = `${currentDate.value} ${currentTime.value}`
}

// 个人打卡数据
const loading = ref(false)
const tableLoading = ref(false)
const todayStatus = ref({
  hasCheckIn: false,
  checkInTime: null,
  hasCheckOut: false,
  checkOutTime: null,
  workMinutes: 0,
  lateMinutes: 0,
  earlyLeaveMinutes: 0,
  status: 'NORMAL'
})

const records = ref([])
const dateRange = ref([])
const pagination = ref({ page: 1, pageSize: 10, total: 0 })

// 部门监控数据
const deptLoading = ref(false)
const deptOverview = ref({
  deptId: null,
  todayDate: '',
  totalUsers: 0,
  checkedInUsers: 0,
  lateUsers: 0,
  notCheckedUsers: 0,
  list: []
})

// 考勤规则数据
const ruleLoading = ref(false)
const ruleSubmitting = ref(false)
const ruleList = ref([])
const ruleDialogVisible = ref(false)
const ruleForm = ref({
  id: null,
  ruleName: '',
  workStartTime: '09:00:00',
  workEndTime: '18:00:00',
  lateThresholdMinutes: 10,
  earlyLeaveThresholdMinutes: 10,
  absentThresholdMinutes: 240
})

// 考勤组数据
const groupLoading = ref(false)
const groupSubmitting = ref(false)
const groupList = ref([])
const groupDialogVisible = ref(false)
const groupForm = ref({
  id: null,
  groupName: '',
  ruleId: null,
  deptId: null
})

// 弹窗
const dialogVisible = ref(false)
const dialogType = ref('checkIn')
const remark = ref('')

// 计算统计
const normalRate = computed(() => {
  if (!records.value.length) return 100
  const normalCount = records.value.filter(r => r.status === 'NORMAL' || r.status === 'RECHECKED').length
  return Math.round((normalCount / records.value.length) * 100)
})

const lateOrEarlyCount = computed(() => {
  return records.value.filter(r => r.status === 'LATE' || r.status === 'EARLY_LEAVE').length
})

// 接口方法
const fetchTodayStatus = async () => {
  try {
    const res = await getTodayStatusApi()
    if (res && res.data) {
      todayStatus.value = res.data
    }
  } catch (err) {
    console.error('获取今日打卡状态失败:', err)
  }
}

const fetchMyRecords = async () => {
  tableLoading.value = true
  try {
    const params = {
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
      startDate: dateRange.value?.[0] || '',
      endDate: dateRange.value?.[1] || ''
    }
    const res = await getMyRecordsApi(params)
    if (res && res.data) {
      records.value = res.data.list || []
      pagination.value.total = res.data.total || 0
    }
  } catch (err) {
    console.error('获取考勤记录失败:', err)
  } finally {
    tableLoading.value = false
  }
}

const fetchDeptOverview = async () => {
  deptLoading.value = true
  try {
    const res = await getDeptTodayOverviewApi()
    if (res && res.data) {
      deptOverview.value = res.data
    }
  } catch (err) {
    console.error('获取部门今日考勤失败:', err)
  } finally {
    deptLoading.value = false
  }
}

const fetchRules = async () => {
  ruleLoading.value = true
  try {
    const res = await getRulesApi()
    if (res && res.data) {
      ruleList.value = res.data
    }
  } catch (err) {
    console.error('获取考勤规则失败:', err)
  } finally {
    ruleLoading.value = false
  }
}

const fetchGroups = async () => {
  groupLoading.value = true
  try {
    const res = await getGroupsApi()
    if (res && res.data) {
      groupList.value = res.data
    }
  } catch (err) {
    console.error('获取考勤组失败:', err)
  } finally {
    groupLoading.value = false
  }
}

watch(activeTab, (val) => {
  if (val === 'dept') {
    fetchDeptOverview()
  } else if (val === 'rule') {
    fetchRules()
    fetchGroups()
  }
})

const openCheckDialog = (type) => {
  dialogType.value = type
  remark.value = ''
  dialogVisible.value = true
}

const submitCheck = async () => {
  loading.value = true
  try {
    const data = { remark: remark.value }
    if (dialogType.value === 'checkIn') {
      await checkInApi(data)
      ElMessage.success('上班打卡成功！')
    } else {
      await checkOutApi(data)
      ElMessage.success('下班打卡成功！')
    }
    dialogVisible.value = false
    await fetchTodayStatus()
    await fetchMyRecords()
  } catch (err) {
    // 拦截器处理
  } finally {
    loading.value = false
  }
}

// 规则编辑与保存
const openRuleDialog = (row) => {
  if (row) {
    ruleForm.value = {
      id: row.id,
      ruleName: row.ruleName,
      workStartTime: row.workStartTime,
      workEndTime: row.workEndTime,
      lateThresholdMinutes: row.lateThresholdMinutes,
      earlyLeaveThresholdMinutes: row.earlyLeaveThresholdMinutes,
      absentThresholdMinutes: row.absentThresholdMinutes
    }
  } else {
    ruleForm.value = {
      id: null,
      ruleName: '新考勤班次规则',
      workStartTime: '09:00:00',
      workEndTime: '18:00:00',
      lateThresholdMinutes: 10,
      earlyLeaveThresholdMinutes: 10,
      absentThresholdMinutes: 240
    }
  }
  ruleDialogVisible.value = true
}

const saveRule = async () => {
  ruleSubmitting.value = true
  try {
    if (ruleForm.value.id) {
      await updateRuleApi(ruleForm.value.id, ruleForm.value)
      ElMessage.success('修改考勤规则成功！')
    } else {
      await createRuleApi(ruleForm.value)
      ElMessage.success('新建考勤规则成功！')
    }
    ruleDialogVisible.value = false
    await fetchRules()
  } catch (err) {
    // 拦截器处理
  } finally {
    ruleSubmitting.value = false
  }
}

// 考勤组编辑与保存
const openGroupDialog = (row) => {
  if (row) {
    groupForm.value = {
      id: row.id,
      groupName: row.groupName,
      ruleId: row.ruleId,
      deptId: row.deptId
    }
  } else {
    groupForm.value = {
      id: null,
      groupName: '新部门考勤组',
      ruleId: ruleList.value[0]?.id || 1,
      deptId: 2
    }
  }
  groupDialogVisible.value = true
}

const saveGroup = async () => {
  groupSubmitting.value = true
  try {
    if (groupForm.value.id) {
      await updateGroupApi(groupForm.value.id, groupForm.value)
      ElMessage.success('修改部门考勤组绑定成功！')
    } else {
      await createGroupApi(groupForm.value)
      ElMessage.success('新建部门考勤组绑定成功！')
    }
    groupDialogVisible.value = false
    await fetchGroups()
  } catch (err) {
    // 拦截器处理
  } finally {
    groupSubmitting.value = false
  }
}

// 转换助手
const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return new Date(timeStr).toTimeString().split(' ')[0]
}

const formatMinutes = (mins) => {
  if (!mins || mins <= 0) return '0分钟'
  const h = Math.floor(mins / 60)
  const m = mins % 60
  if (h > 0 && m > 0) return `${h}小时${m}分钟`
  if (h > 0) return `${h}小时`
  return `${m}分钟`
}

const getStatusTagType = (status) => {
  switch (status) {
    case 'NORMAL': return 'success'
    case 'RECHECKED': return 'primary'
    case 'ON_LEAVE': return 'warning'
    case 'LATE': return 'danger'
    case 'EARLY_LEAVE': return 'warning'
    case 'LATE_AND_EARLY': return 'danger'
    case 'ABSENT': return 'danger'
    case 'MISSING_CARD': return 'warning'
    default: return 'info'
  }
}

const formatStatusText = (status, lateMins, earlyMins) => {
  switch (status) {
    case 'NORMAL': return '正常打卡'
    case 'RECHECKED': return '已补卡'
    case 'ON_LEAVE': return '休假中'
    case 'LATE': return `迟到 (${formatMinutes(lateMins)})`
    case 'EARLY_LEAVE': return `早退 (${formatMinutes(earlyMins)})`
    case 'LATE_AND_EARLY': return `迟到(${formatMinutes(lateMins)}) + 早退(${formatMinutes(earlyMins)})`
    case 'ABSENT': return `旷工 (${formatMinutes(lateMins)})`
    case 'MISSING_CARD': return '缺卡'
    default: return status || '正常'
  }
}

const getDeptStatusTagType = (status) => {
  switch (status) {
    case 'NORMAL': return 'success'
    case 'RECHECKED': return 'primary'
    case 'ON_LEAVE': return 'warning'
    case 'LATE': return 'danger'
    case 'EARLY_LEAVE': return 'warning'
    case 'LATE_AND_EARLY': return 'danger'
    case 'ABSENT': return 'danger'
    case 'MISSING_CARD': return 'warning'
    case 'NOT_CHECKED': return 'info'
    default: return 'info'
  }
}

const formatDeptStatusText = (status, lateMins) => {
  switch (status) {
    case 'NORMAL': return '正常打卡'
    case 'RECHECKED': return '已补卡'
    case 'ON_LEAVE': return '休假中'
    case 'LATE': return `迟到 (${formatMinutes(lateMins)})`
    case 'EARLY_LEAVE': return '早退'
    case 'LATE_AND_EARLY': return `迟到且早退 (${formatMinutes(lateMins)})`
    case 'ABSENT': return `旷工 (${formatMinutes(lateMins)})`
    case 'MISSING_CARD': return '缺卡'
    case 'NOT_CHECKED': return '未打卡'
    default: return status || '未打卡'
  }
}

onMounted(() => {
  updateClock()
  timer = setInterval(updateClock, 1000)
  fetchTodayStatus()
  fetchMyRecords()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.attendance-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.mode-switch-card {
  background: #ffffff;
  padding: 12px 20px;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  display: flex;
  justify-content: flex-start;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

/* 顶部概览 Banner (白色卡片) */
.overview-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.live-clock {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  font-family: 'Inter', -apple-system, sans-serif;
  letter-spacing: -1px;
}

.date-info {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-text {
  font-size: 14px;
  color: #64748b;
}

.metrics-grid {
  display: flex;
  gap: 36px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.metric-title {
  font-size: 13px;
  color: #64748b;
}

.metric-num {
  font-size: 24px;
  font-weight: 700;
  margin-top: 4px;
}

.metric-num small {
  font-size: 13px;
  font-weight: 400;
  color: #94a3b8;
}

.primary-num { color: #2563eb; }
.success-num { color: #16a34a; }
.warning-num { color: #ea580c; }
.danger-num { color: #dc2626; }

/* 打卡卡片区 */
.punch-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px 24px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.punch-card:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f8fafc;
}

.head-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.target-time {
  font-size: 12px;
  font-weight: 400;
  color: #94a3b8;
  margin-left: 6px;
}

.icon-sun { color: #ea580c; font-size: 20px; }
.icon-moon { color: #6366f1; font-size: 20px; }

.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 16px;
}

.record-detail {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.detail-label {
  font-size: 13px;
  color: #64748b;
}

.detail-time {
  font-size: 20px;
  font-weight: 700;
}

.detail-time small {
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
}

.detail-hint {
  font-size: 13px;
  color: #94a3b8;
}

.action-button {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
}

.success-text { color: #16a34a; }
.warning-text { color: #d97706; }
.text-muted { color: #94a3b8; }

/* 底部表格卡片 */
.table-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.table-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title-group h2 {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.header-icon {
  font-size: 20px;
  color: #2563eb;
}

.header-filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-column {
  font-weight: 600;
  color: #334155;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
