<template>
  <section class="panel">
    <div class="panel-header">
      <h2>公告通知</h2>
      <el-button type="primary" :icon="Plus">发布公告</el-button>
    </div>
    <el-table :data="notices" border>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="publisher" label="发布人" width="140" />
      <el-table-column prop="createdAt" label="发布时间" width="180" />
      <el-table-column prop="read" label="阅读状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.read ? 'success' : 'info'">{{ row.read ? '已读' : '未读' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

const notices = [
  { title: '本周五下午进行项目答辩预演', publisher: '管理员', createdAt: '2026-07-21 09:00', read: false },
  { title: '请各部门及时完成考勤补卡申请', publisher: '人事部', createdAt: '2026-07-20 16:30', read: true },
  { title: '系统维护通知', publisher: '管理员', createdAt: '2026-07-19 18:00', read: true }
]
</script>

