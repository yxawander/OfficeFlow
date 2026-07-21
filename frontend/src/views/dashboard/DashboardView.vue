<template>
  <div class="page-grid">
    <el-row :gutter="16">
      <el-col v-for="item in metrics" :key="item.label" :xs="24" :sm="12" :lg="6">
        <div class="metric-card">
          <div class="metric-label">{{ item.label }}</div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-trend">{{ item.trend }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <section class="panel">
          <div class="panel-header">
            <h2>月度考勤趋势</h2>
            <el-tag type="success">实时统计</el-tag>
          </div>
          <div ref="attendanceChartRef" class="chart"></div>
        </section>
      </el-col>
      <el-col :xs="24" :lg="10">
        <section class="panel">
          <div class="panel-header">
            <h2>审批类型占比</h2>
            <el-tag>本月</el-tag>
          </div>
          <div ref="flowChartRef" class="chart"></div>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'

const attendanceChartRef = ref()
const flowChartRef = ref()

const metrics = [
  { label: '员工总数', value: '126', trend: '较上月 +8' },
  { label: '今日打卡', value: '118', trend: '出勤率 93.6%' },
  { label: '待办审批', value: '17', trend: '请及时处理' },
  { label: '公告阅读率', value: '86%', trend: '较昨日 +5%' }
]

onMounted(() => {
  echarts.init(attendanceChartRef.value).setOption({
    tooltip: {},
    grid: { left: 36, right: 20, top: 32, bottom: 32 },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五'] },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: [112, 119, 116, 121, 118],
        itemStyle: { color: '#2563eb' },
        barWidth: 24
      }
    ]
  })

  echarts.init(flowChartRef.value).setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['46%', '68%'],
        data: [
          { value: 42, name: '请假' },
          { value: 26, name: '加班' },
          { value: 18, name: '补卡' }
        ]
      }
    ]
  })
})
</script>

