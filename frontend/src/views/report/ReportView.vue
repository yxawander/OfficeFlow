<template>
  <div class="report-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">月度考勤统计报表</h1>
        <p class="page-subtitle">自动联动每日打卡与审批单据，精准统计全员出勤、加班与请假多维指标</p>
      </div>
      <div class="action-buttons">
        <el-button type="primary" size="large" icon="Refresh" :loading="generating" @click="handleGenerate">
          重新生成本月报表
        </el-button>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-card">
      <div class="filter-item month-filter">
        <span class="filter-label">统计月份：</span>
        <el-date-picker
          v-model="queryForm.month"
          type="month"
          placeholder="选择月份"
          format="YYYY-MM"
          value-format="YYYY-MM"
          :clearable="false"
          @change="fetchReports"
        />
      </div>

      <div class="filter-item dept-filter">
        <span class="filter-label">部门筛选：</span>
        <el-tree-select
          v-model="queryForm.deptId"
          :data="deptList"
          :props="{ label: 'deptName', children: 'children' }"
          node-key="id"
          check-strictly
          clearable
          placeholder="全部部门"
          @change="fetchReports"
        />
      </div>

      <div class="filter-item search-item">
        <el-input
          v-model="queryForm.keyword"
          placeholder="搜索员工姓名或账号"
          clearable
          prefix-icon="Search"
          @keyup.enter="fetchReports"
          @clear="fetchReports"
        />
        <el-button type="primary" @click="fetchReports">查询</el-button>
      </div>
    </div>

    <!-- Data Table -->
    <div class="table-card">
      <el-table :data="reportList" border stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="realName" label="员工姓名" width="130" fixed="left">
          <template #default="{ row }">
            <span class="font-bold">{{ row.realName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="deptName" label="所属部门" width="130" />
        <el-table-column prop="reportMonth" label="月份" width="100" align="center" />
        
        <el-table-column label="出勤天数 (天)" align="center">
          <el-table-column prop="shouldWorkDays" label="应出勤" width="90" align="center" />
          <el-table-column prop="actualWorkDays" label="实际出勤" width="100" align="center">
            <template #default="{ row }">
              <span class="success-text font-bold">{{ row.actualWorkDays }}</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="考勤异常统计" align="center">
          <el-table-column prop="lateCount" label="迟到" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.lateCount > 0" type="danger" size="small">{{ row.lateCount }}次</el-tag>
              <span v-else class="text-muted">0</span>
            </template>
          </el-table-column>
          <el-table-column prop="earlyLeaveCount" label="早退" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.earlyLeaveCount > 0" type="warning" size="small">{{ row.earlyLeaveCount }}次</el-tag>
              <span v-else class="text-muted">0</span>
            </template>
          </el-table-column>
          <el-table-column prop="absentCount" label="旷工" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.absentCount > 0" type="danger" effect="dark" size="small">{{ row.absentCount }}天</el-tag>
              <span v-else class="text-muted">0</span>
            </template>
          </el-table-column>
          <el-table-column prop="missingCardCount" label="缺卡" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.missingCardCount > 0" type="info" size="small">{{ row.missingCardCount }}次</el-tag>
              <span v-else class="text-muted">0</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="流程审批联动" align="center">
          <el-table-column prop="leaveDays" label="请假天数" width="110" align="center">
            <template #default="{ row }">
              <span v-if="row.leaveDays > 0" class="primary-text font-bold">{{ row.leaveDays }} 天</span>
              <span v-else class="text-muted">0</span>
            </template>
          </el-table-column>
          <el-table-column prop="overtimeHours" label="加班工时" width="130" align="center">
            <template #default="{ row }">
              <div v-if="row.overtimeHours > 0">
                <span class="warning-text font-bold">{{ row.overtimeHours }}h</span>
                <el-tooltip content="按 Min(审批加班时长, 实际打卡时长) 精准认定" placement="top">
                  <el-icon class="info-icon"><InfoFilled /></el-icon>
                </el-tooltip>
              </div>
              <span v-else class="text-muted">0h</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column prop="generatedAt" label="更新时间" min-width="170" align="center" />
      </el-table>

      <!-- Pagination -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          @size-change="fetchReports"
          @current-change="fetchReports"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getMonthlyReportsApi, generateMonthlyReportApi } from '@/api/report'
import { getDeptTreeApi } from '@/api/user'

const currentMonthStr = new Date().toISOString().slice(0, 7)

const queryForm = reactive({
  month: currentMonthStr,
  deptId: null,
  keyword: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const loading = ref(false)
const generating = ref(false)
const reportList = ref([])
const deptList = ref([])

const fetchReports = async () => {
  loading.value = true
  try {
    const res = await getMonthlyReportsApi({
      month: queryForm.month,
      deptId: queryForm.deptId,
      keyword: queryForm.keyword,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      reportList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('获取月度报表失败', error)
  } finally {
    loading.value = false
  }
}

const handleGenerate = async () => {
  generating.value = true
  try {
    await generateMonthlyReportApi(queryForm.month)
    ElMessage.success(`${queryForm.month} 月度考勤报表重新汇总成功！`)
    fetchReports()
  } catch (error) {
    console.error('生成报表失败', error)
  } finally {
    generating.value = false
  }
}

const fetchDepts = async () => {
  try {
    const res = await getDeptTreeApi()
    if (res.code === 200 && res.data) {
      deptList.value = res.data
    }
  } catch (error) {
    console.error('获取部门失败', error)
  }
}

onMounted(() => {
  fetchDepts()
  fetchReports()
})
</script>

<style scoped>
.report-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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

.filter-card {
  background: #ffffff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.05);
  display: grid;
  grid-template-columns: 360px 260px minmax(280px, 1fr);
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.filter-item {
  display: flex;
  align-items: center;
  min-width: 0;
}

.filter-label {
  flex: 0 0 auto;
  font-size: 14px;
  color: #475569;
  font-weight: 500;
  margin-right: 8px;
  white-space: nowrap;
}

.month-filter :deep(.el-date-editor) {
  width: 220px;
}

.dept-filter :deep(.el-select) {
  width: 160px;
}

.search-item {
  justify-content: flex-end;
  gap: 10px;
}

.search-item :deep(.el-input) {
  width: min(320px, 100%);
}

.table-card {
  background: #ffffff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.05);
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.font-bold {
  font-weight: 600;
}

.success-text {
  color: #10b981;
}

.warning-text {
  color: #f59e0b;
}

.primary-text {
  color: #3b82f6;
}

.text-muted {
  color: #94a3b8;
}

.info-icon {
  margin-left: 4px;
  color: #94a3b8;
  cursor: pointer;
}

@media (max-width: 1080px) {
  .filter-card {
    grid-template-columns: 1fr 1fr;
  }

  .search-item {
    grid-column: 1 / -1;
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .filter-card {
    grid-template-columns: 1fr;
  }

  .filter-item {
    align-items: stretch;
    flex-direction: column;
    gap: 8px;
  }

  .filter-label {
    margin-right: 0;
  }

  .month-filter :deep(.el-date-editor),
  .dept-filter :deep(.el-select),
  .search-item :deep(.el-input) {
    width: 100%;
  }
}
</style>
