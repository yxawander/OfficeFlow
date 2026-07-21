<template>
  <section class="panel">
    <div class="panel-header">
      <h2>员工管理</h2>
      <el-button type="primary" :icon="Plus">新增员工</el-button>
    </div>
    <el-table :data="users" border>
      <el-table-column prop="username" label="账号" width="140" />
      <el-table-column prop="realName" label="姓名" width="140" />
      <el-table-column prop="department" label="部门" />
      <el-table-column prop="post" label="岗位" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === '启用' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

const users = [
  { username: 'admin', realName: '系统管理员', department: '总经办', post: '管理员', status: '启用' },
  { username: 'zhangsan', realName: '张三', department: '研发部', post: '后端工程师', status: '启用' },
  { username: 'lisi', realName: '李四', department: '人事部', post: 'HR', status: '启用' }
]
</script>

