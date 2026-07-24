<template>
  <div class="salary-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">工资结算与档案</h1>
        <p class="page-subtitle">根据月度考勤明细与岗位薪资标准，精准自动核算基本薪资、加班费及考勤扣款</p>
      </div>
      <div class="action-buttons" v-if="isAdminOrManager">
        <el-button type="success" size="large" icon="Money" :loading="generating" @click="handleGenerateSalary">
          一键全员月度工资结算
        </el-button>
      </div>
    </div>

    <!-- Mode Tabs -->
    <el-tabs v-model="activeTab" class="salary-tabs">
      <!-- 个人工资条 -->
      <el-tab-pane label="我的月度工资条" name="my">
        <div class="my-salary-wrapper">
          <div class="month-picker-bar">
            <span>选择结算月份：</span>
            <el-date-picker
              v-model="myMonth"
              type="month"
              format="YYYY-MM"
              value-format="YYYY-MM"
              :clearable="false"
              @change="fetchMySalary"
            />
          </div>

          <div v-loading="loadingMy" class="payslip-card">
            <div class="payslip-header">
              <div class="payslip-title">
                <h2>{{ mySalary?.settleMonth || myMonth }} 月份薪资明细单</h2>
                <el-tag v-if="!mySalary" type="info" effect="dark">未生成</el-tag>
                <el-tag v-else :type="mySalary.status === 'PUBLISHED' ? 'success' : 'warning'" effect="dark">
                  {{ mySalary.status === 'PUBLISHED' ? '已发放' : '待发放(核算中)' }}
                </el-tag>
              </div>
              <div class="payslip-emp-info">
                <span>姓名：<strong>{{ mySalary?.realName || userStore.profile?.realName }}</strong></span>
                <span>部门：<strong>{{ mySalary?.deptName || '-' }}</strong></span>
                <span>岗位：<strong>{{ mySalary?.postName || '-' }}</strong></span>
              </div>
            </div>

            <template v-if="mySalary">
              <div class="payslip-body">
                <div class="net-salary-box">
                  <div class="net-label">本月实发工资 (元)</div>
                  <div class="net-value">￥{{ formatMoney(mySalary?.actualSalary) }}</div>
                </div>

                <div class="salary-breakdown-grid">
                  <!-- 收入项 -->
                  <div class="breakdown-column income-col">
                    <div class="column-title">
                      <el-icon><Plus /></el-icon> 应发收入明细
                    </div>
                    <div class="item-row">
                      <span>基本工资</span>
                      <span class="amount">￥{{ formatMoney(mySalary?.baseSalary) }}</span>
                    </div>
                    <div class="item-row">
                      <span>岗位津贴/补贴</span>
                      <span class="amount">￥{{ formatMoney(mySalary?.allowance) }}</span>
                    </div>
                    <div class="item-row">
                      <span>考勤绩效奖金</span>
                      <span class="amount highlight-income">￥{{ formatMoney(mySalary?.performanceBonus) }}</span>
                    </div>
                    <div class="item-row">
                      <div class="flex-col">
                        <span>加班费补贴</span>
                        <span class="sub-text" v-if="mySalary?.hourlyWage">({{ mySalary.overtimeHours }}小时 × ￥{{ formatMoney(mySalary.hourlyWage) }}/时 × 1.5)</span>
                      </div>
                      <span class="amount highlight-income">￥{{ formatMoney(mySalary?.overtimePay) }}</span>
                    </div>
                  </div>

                  <!-- 扣款项 -->
                  <div class="breakdown-column deduction-col">
                    <div class="column-title">
                      <el-icon><Minus /></el-icon> 考勤扣款明细
                    </div>
                    <div class="item-row">
                      <div class="flex-col">
                        <span>迟到早退/缺卡扣款 (仅扣绩效)</span>
                        <span class="sub-text" v-if="mySalary?.hourlyWage">({{ mySalary.offWorkHours }}小时 × ￥{{ formatMoney(mySalary.hourlyWage) }}/时)</span>
                      </div>
                      <span class="amount text-danger">- ￥{{ formatMoney(mySalary?.lateDeduction) }}</span>
                    </div>
                    <div class="item-row">
                      <div class="flex-col">
                        <span>缺卡扣款 (仅扣绩效)</span>
                      </div>
                      <span class="amount text-danger">- ￥{{ formatMoney(mySalary?.missingCardDeduction) }}</span>
                    </div>
                    <div class="item-row" style="background: #fff1f2; padding: 10px; border-radius: 6px; margin: 4px 0 10px 0;">
                      <div class="flex-col">
                        <span style="color: #dc2626; font-weight: bold; font-size: 13px;">实际生效违纪扣款</span>
                        <span class="sub-text" style="color: #ef4444;">(最多扣除全部考勤绩效 ￥{{ formatMoney(mySalary?.performanceBonus) }})</span>
                      </div>
                      <span class="amount text-danger">- ￥{{ formatMoney(Math.min((mySalary?.lateDeduction || 0) + (mySalary?.missingCardDeduction || 0), mySalary?.performanceBonus || 0)) }}</span>
                    </div>
                    <div class="item-row">
                      <div class="flex-col">
                        <span>旷工扣款</span>
                        <span class="sub-text" v-if="mySalary?.dailyWage">({{ mySalary.absentDays }}天 × ￥{{ formatMoney(mySalary.dailyWage) }}/天 × 2)</span>
                      </div>
                      <span class="amount text-danger">- ￥{{ formatMoney(mySalary?.absentDeduction) }}</span>
                    </div>
                    <div class="item-row">
                      <div class="flex-col">
                        <span>请假扣款</span>
                        <span class="sub-text" v-if="mySalary?.dailyWage">({{ mySalary.leaveDays }}天 × ￥{{ formatMoney(mySalary.dailyWage) }}/天)</span>
                      </div>
                      <span class="amount text-danger">- ￥{{ formatMoney(mySalary?.leaveDeduction) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="payslip-footer">
                <p>注：工资核算依据当月考勤打卡及流程审批数据自动生成，如有异议请联系人事部门。</p>
              </div>
            </template>
            <el-empty v-else description="本月工资条尚未生成，请等待管理员结算" />
          </div>
        </div>
      </el-tab-pane>

      <!-- 管理员/主管全员薪资档案 -->
      <el-tab-pane label="全员薪资结算档案" name="all" v-if="isAdminOrManager">
        <div class="filter-card">
          <div class="filter-item">
            <span class="filter-label">结算月份：</span>
            <el-date-picker
              v-model="queryForm.month"
              type="month"
              format="YYYY-MM"
              value-format="YYYY-MM"
              :clearable="false"
              @change="fetchAllSalary"
            />
          </div>

          <div class="filter-item">
            <span class="filter-label">部门筛选：</span>
            <el-select v-model="queryForm.deptId" placeholder="全部部门" clearable @change="fetchAllSalary">
              <el-option v-for="dept in deptList" :key="dept.id" :label="dept.name" :value="dept.id" />
            </el-select>
          </div>

          <div class="filter-item search-item">
            <el-input
              v-model="queryForm.keyword"
              placeholder="搜索姓名或账号"
              clearable
              prefix-icon="Search"
              @keyup.enter="fetchAllSalary"
              @clear="fetchAllSalary"
            />
            <el-button type="primary" @click="fetchAllSalary">查询</el-button>
            <el-button type="success" :disabled="selectedIds.length === 0" :loading="publishing" @click="handlePublish">发布选中记录</el-button>
          </div>
        </div>

        <div class="table-card">
          <el-table :data="allSalaryList" border stripe style="width: 100%" v-loading="loadingAll" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" :selectable="row => row.status === 'DRAFT'" />
            <el-table-column prop="realName" label="员工姓名" width="130" fixed="left">
              <template #default="{ row }">
                <span class="font-bold">{{ row.realName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="deptName" label="部门" width="120" />
            <el-table-column prop="postName" label="岗位" width="130" />
            <el-table-column prop="settleMonth" label="结算月份" width="100" align="center" />
            
            <el-table-column prop="baseSalary" label="基本工资" width="120" align="right">
              <template #default="{ row }">￥{{ formatMoney(row.baseSalary) }}</template>
            </el-table-column>
            
            <el-table-column prop="allowance" label="津贴" width="100" align="right">
              <template #default="{ row }">￥{{ formatMoney(row.allowance) }}</template>
            </el-table-column>

            <el-table-column prop="performanceBonus" label="考勤绩效" width="100" align="right">
              <template #default="{ row }">￥{{ formatMoney(row.performanceBonus) }}</template>
            </el-table-column>
            
            <el-table-column prop="overtimePay" label="加班费" width="110" align="right">
              <template #default="{ row }">
                <span class="success-text">￥{{ formatMoney(row.overtimePay) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="考勤总扣款" width="120" align="right">
              <template #default="{ row }">
                <span class="danger-text">
                  - ￥{{ formatMoney((row.absentDeduction || 0) + (row.leaveDeduction || 0) + Math.min((row.lateDeduction || 0) + (row.missingCardDeduction || 0), row.performanceBonus || 0)) }}
                </span>
              </template>
            </el-table-column>

            <el-table-column prop="actualSalary" label="实发工资" width="140" align="right" fixed="right">
              <template #default="{ row }">
                <span class="font-bold actual-salary-text">￥{{ formatMoney(row.actualSalary) }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="状态" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'" size="small">
                  {{ row.status === 'PUBLISHED' ? '已发放' : '待发放' }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleViewDetail(row)">详情</el-button>
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
              @size-change="fetchAllSalary"
              @current-change="fetchAllSalary"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 薪资详情弹窗 -->
    <el-dialog v-model="detailVisible" title="薪资账单明细" width="750px" destroy-on-close class="salary-detail-dialog">
      <div class="payslip-card" style="box-shadow: none; border: none; padding: 0;">
        <div class="payslip-header">
          <div class="payslip-title">
            <h2>{{ detailSalary?.settleMonth }} 月份薪资明细单</h2>
            <el-tag :type="detailSalary?.status === 'PUBLISHED' ? 'success' : 'info'" effect="dark">
              {{ detailSalary?.status === 'PUBLISHED' ? '已结清发放' : '未结清发放' }}
            </el-tag>
          </div>
          <div class="payslip-emp-info">
            <span>姓名：<strong>{{ detailSalary?.realName }}</strong></span>
            <span>部门：<strong>{{ detailSalary?.deptName || '-' }}</strong></span>
            <span>岗位：<strong>{{ detailSalary?.postName || '-' }}</strong></span>
          </div>
        </div>

        <div class="payslip-body">
          <div class="net-salary-box">
            <div class="net-label">本月实发工资 (元)</div>
            <div class="net-value">￥{{ formatMoney(detailSalary?.actualSalary) }}</div>
          </div>

          <div class="salary-breakdown-grid">
            <!-- 收入项 -->
            <div class="breakdown-column income-col">
              <div class="column-title">
                <el-icon><Plus /></el-icon> 应发收入明细
              </div>
              <div class="item-row">
                <span>基本工资</span>
                <span class="amount">￥{{ formatMoney(detailSalary?.baseSalary) }}</span>
              </div>
              <div class="item-row">
                <span>岗位津贴/补贴</span>
                <span class="amount">￥{{ formatMoney(detailSalary?.allowance) }}</span>
              </div>
              <div class="item-row">
                <span>考勤绩效奖金</span>
                <span class="amount highlight-income">￥{{ formatMoney(detailSalary?.performanceBonus) }}</span>
              </div>
              <div class="item-row">
                <div class="flex-col">
                  <span>加班费补贴</span>
                  <span class="sub-text" v-if="detailSalary?.hourlyWage">({{ detailSalary.overtimeHours }}小时 × ￥{{ formatMoney(detailSalary.hourlyWage) }}/时 × 1.5)</span>
                </div>
                <span class="amount highlight-income">￥{{ formatMoney(detailSalary?.overtimePay) }}</span>
              </div>
            </div>

            <!-- 扣款项 -->
            <div class="breakdown-column deduction-col">
              <div class="column-title">
                <el-icon><Minus /></el-icon> 考勤扣款明细
              </div>
              <div class="item-row">
                <div class="flex-col">
                  <span>迟到早退/缺卡扣款 (仅扣绩效)</span>
                  <span class="sub-text" v-if="detailSalary?.hourlyWage">({{ detailSalary.offWorkHours }}小时 × ￥{{ formatMoney(detailSalary.hourlyWage) }}/时)</span>
                </div>
                <span class="amount text-danger">- ￥{{ formatMoney(detailSalary?.lateDeduction) }}</span>
              </div>
              <div class="item-row">
                <div class="flex-col">
                  <span>缺卡扣款 (仅扣绩效)</span>
                </div>
                <span class="amount text-danger">- ￥{{ formatMoney(detailSalary?.missingCardDeduction) }}</span>
              </div>
              <div class="item-row" style="background: #fff1f2; padding: 10px; border-radius: 6px; margin: 4px 0 10px 0;">
                <div class="flex-col">
                  <span style="color: #dc2626; font-weight: bold; font-size: 13px;">实际生效违纪扣款</span>
                  <span class="sub-text" style="color: #ef4444;">(最多扣除全部考勤绩效 ￥{{ formatMoney(detailSalary?.performanceBonus) }})</span>
                </div>
                <span class="amount text-danger">- ￥{{ formatMoney(Math.min((detailSalary?.lateDeduction || 0) + (detailSalary?.missingCardDeduction || 0), detailSalary?.performanceBonus || 0)) }}</span>
              </div>
              <div class="item-row">
                <div class="flex-col">
                  <span>旷工扣款</span>
                  <span class="sub-text" v-if="detailSalary?.dailyWage">({{ detailSalary.absentDays }}天 × ￥{{ formatMoney(detailSalary.dailyWage) }}/天 × 2)</span>
                </div>
                <span class="amount text-danger">- ￥{{ formatMoney(detailSalary?.absentDeduction) }}</span>
              </div>
              <div class="item-row">
                <div class="flex-col">
                  <span>请假扣款</span>
                  <span class="sub-text" v-if="detailSalary?.dailyWage">({{ detailSalary.leaveDays }}天 × ￥{{ formatMoney(detailSalary.dailyWage) }}/天)</span>
                </div>
                <span class="amount text-danger">- ￥{{ formatMoney(detailSalary?.leaveDeduction) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Minus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMySalaryStatementApi, getSalaryStatementsApi, generateMonthlySalaryApi, publishSalaryStatementsApi } from '@/api/report'
import { getDeptTreeApi } from '@/api/user'

const userStore = useUserStore()
const currentMonthStr = new Date().toISOString().slice(0, 7)

const activeTab = ref('my')
const myMonth = ref(currentMonthStr)
const mySalary = ref(null)
const loadingMy = ref(false)

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

const loadingAll = ref(false)
const generating = ref(false)
const allSalaryList = ref([])
const deptList = ref([])

const detailVisible = ref(false)
const detailSalary = ref(null)

const isAdminOrManager = computed(() => {
  const user = userStore.profile
  if (!user) return false
  const userType = user.userType || ''
  const roleCode = user.roleCode || ''
  const roles = user.roles || []
  return userType === 'ADMIN' || userType === 'MANAGER' || roleCode === 'ADMIN' || roles.some(r => r.roleCode === 'ADMIN')
})

const fetchMySalary = async () => {
  loadingMy.value = true
  try {
    const res = await getMySalaryStatementApi({ month: myMonth.value })
    if (res.code === 200) {
      mySalary.value = res.data
    }
  } catch (error) {
    console.error('获取个人工资条失败', error)
  } finally {
    loadingMy.value = false
  }
}

const fetchAllSalary = async () => {
  loadingAll.value = true
  try {
    const res = await getSalaryStatementsApi({
      month: queryForm.month,
      deptId: queryForm.deptId,
      keyword: queryForm.keyword,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      allSalaryList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('获取全员工资列表失败', error)
  } finally {
    loadingAll.value = false
  }
}

const handleGenerateSalary = async () => {
  generating.value = true
  try {
    await generateMonthlySalaryApi(queryForm.month)
    ElMessage.success(`${queryForm.month} 月全员薪资计算与结算完成！`)
    fetchAllSalary()
    if (myMonth.value === queryForm.month) {
      fetchMySalary()
    }
  } catch (error) {
    console.error('计算工资失败', error)
  } finally {
    generating.value = false
  }
}

const selectedIds = ref([])
const publishing = ref(false)

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const handlePublish = async () => {
  if (selectedIds.value.length === 0) return
  publishing.value = true
  try {
    const res = await publishSalaryStatementsApi(selectedIds.value)
    if (res.code === 200) {
      ElMessage.success(`成功发布 ${selectedIds.value.length} 条工资单！`)
      fetchAllSalary()
    }
  } catch (error) {
    console.error('发布失败', error)
  } finally {
    publishing.value = false
  }
}

const formatMoney = (val) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toFixed(2)
}

const handleViewDetail = (row) => {
  detailSalary.value = row
  detailVisible.value = true
}

const fetchDepts = async () => {
  try {
    const res = await getDeptTreeApi()
    if (res.code === 200 && res.data) {
      // 扁平化部门树用于下拉框
      const flatDepts = []
      const flatten = (list) => {
        list.forEach(dept => {
          flatDepts.push({ id: dept.id, name: dept.name })
          if (dept.children && dept.children.length > 0) flatten(dept.children)
        })
      }
      flatten(res.data)
      deptList.value = flatDepts
    }
  } catch (error) {
    console.error('获取部门失败', error)
  }
}

onMounted(() => {
  fetchDepts()
  fetchMySalary()
  if (isAdminOrManager.value) {
    fetchAllSalary()
  }
})
</script>

<style scoped>
.salary-container {
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

.salary-tabs {
  background: #ffffff;
  padding: 20px 24px 24px;
  border-radius: 12px;
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.05);
}

.my-salary-wrapper {
  max-width: 800px;
  margin: 0 auto;
}

.month-picker-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 20px;
  font-size: 14px;
  color: #475569;
}

.payslip-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 10px 30px -5px rgba(0, 0, 0, 0.08);
}

.payslip-header {
  border-bottom: 2px dashed #cbd5e1;
  padding-bottom: 20px;
  margin-bottom: 24px;
}

.payslip-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.payslip-title h2 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
  font-weight: 700;
}

.payslip-emp-info {
  display: flex;
  gap: 32px;
  color: #64748b;
  font-size: 14px;
}

.net-salary-box {
  background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
  color: #ffffff;
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  margin-bottom: 28px;
}

.net-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 6px;
}

.net-value {
  font-size: 36px;
  font-weight: 800;
  letter-spacing: 1px;
}

.salary-breakdown-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.breakdown-column {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f1f5f9;
}

.column-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.income-col .column-title {
  color: #16a34a;
}

.deduction-col .column-title {
  color: #dc2626;
}

.item-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f8fafc;
  font-size: 14px;
  color: #334155;
}

.amount {
  font-weight: 600;
  display: flex;
  align-items: center;
}

.flex-col {
  display: flex;
  flex-direction: column;
}

.sub-text {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.highlight-income {
  color: #16a34a;
}

.text-danger {
  color: #dc2626;
}

.payslip-footer {
  margin-top: 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
}

.filter-card {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  background: #f8fafc;
  padding: 14px 18px;
  border-radius: 8px;
}

.filter-item {
  display: flex;
  align-items: center;
}

.filter-label {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
  margin-right: 8px;
}

.search-item {
  margin-left: auto;
  gap: 10px;
}

.actual-salary-text {
  color: #0284c7;
  font-size: 16px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
