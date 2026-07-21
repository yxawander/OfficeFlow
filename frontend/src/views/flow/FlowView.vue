<template>
  <div class="page-grid">
    <section class="panel">
      <div class="panel-header">
        <h2>申请单据</h2>
        <div>
          <el-button type="primary">请假申请</el-button>
          <el-button>加班申请</el-button>
        </div>
      </div>
      <el-table :data="applications" border>
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="reason" label="事由" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === '已同意' ? 'success' : row.status === '待审批' ? 'warning' : 'danger'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>待办审批</h2>
        <el-tag type="warning">3 条</el-tag>
      </div>
      <el-table :data="todos" border>
        <el-table-column prop="applicant" label="申请人" width="140" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="reason" label="事由" />
        <el-table-column label="操作" width="180">
          <el-button link type="success">同意</el-button>
          <el-button link type="danger">驳回</el-button>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
const applications = [
  { type: '请假', reason: '家庭事务', status: '待审批' },
  { type: '加班', reason: '版本上线支持', status: '已同意' }
]

const todos = [
  { applicant: '张三', type: '请假', reason: '病假半天' },
  { applicant: '李四', type: '加班', reason: '客户现场支持' }
]
</script>

