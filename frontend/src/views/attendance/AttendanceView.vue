<template>
  <div class="page-grid">
    <section class="panel action-panel">
      <div>
        <h2>今日考勤</h2>
        <p>{{ today }}</p>
      </div>
      <div class="action-buttons">
        <el-button type="primary" size="large" :icon="Clock">上班打卡</el-button>
        <el-button size="large" :icon="Clock">下班打卡</el-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>个人打卡记录</h2>
        <el-button>导出</el-button>
      </div>
      <el-table :data="records" border>
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="checkIn" label="上班时间" />
        <el-table-column prop="checkOut" label="下班时间" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === '正常' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { Clock } from '@element-plus/icons-vue'

const today = new Date().toLocaleDateString()
const records = [
  { date: '2026-07-20', checkIn: '08:55', checkOut: '18:04', status: '正常' },
  { date: '2026-07-19', checkIn: '09:12', checkOut: '18:01', status: '迟到' },
  { date: '2026-07-18', checkIn: '08:48', checkOut: '18:10', status: '正常' }
]
</script>

