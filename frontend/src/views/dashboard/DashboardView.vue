<template>
  <div class="dash-body">

    <!-- ── 指标卡 ×4 ── -->
    <div v-for="(m, i) in metrics" :key="m.label" class="pnl mc" :class="{ in: entered }"
         :style="{ '--mc-c': m.c, '--mc-c2': m.c2, transitionDelay: `${i * .07 + .05}s` }">
      <div class="mc-label">{{ m.label }}</div>
      <div class="mc-row">
        <div class="mc-val" v-html="m.animated"></div>
        <svg class="mc-ring" viewBox="0 0 52 52">
          <circle class="mc-ring-bg" cx="26" cy="26" r="22" />
          <circle class="mc-ring-fg" cx="26" cy="26" r="22"
                  :stroke-dasharray="RING_C" :stroke-dashoffset="m.ringOff"
                  :style="{ '--mc-c': m.c }" />
        </svg>
      </div>
      <div class="mc-foot">
        <span class="mc-arrow" :class="m.dir"><svg viewBox="0 0 8 8"><path :d="m.dir === 'up' ? 'M4 1L7 5H1Z' : 'M4 7L7 3H1Z'" fill="currentColor" /></svg></span>
        <span>{{ m.trend }}</span>
      </div>
      <svg class="mc-spark" viewBox="0 0 200 40" preserveAspectRatio="none">
        <polygon :points="m.sparkFill" />
        <polyline :points="m.sparkLine" />
      </svg>
    </div>

    <!-- ── 周趋势 ── -->
    <div class="pnl trend" :class="{ in: entered }" style="transition-delay:.33s">
      <div class="pnl-head"><span class="pnl-title">周考勤趋势</span><span class="pnl-badge blue">本周</span></div>
      <div ref="trendEl" class="chart-lg"></div>
    </div>

    <!-- ── 出勤率仪表 ── -->
    <div class="pnl hero" :class="{ in: entered }" style="transition-delay:.38s">
      <div class="pnl-head" style="width:100%"><span class="pnl-title">{{ overview.dataScope === 'DEPT' ? '部门出勤率' : '今日出勤率' }}</span><span class="pnl-badge" :class="overview.dataScope === 'DEPT' ? 'coral' : 'green'">{{ overview.dataScope === 'DEPT' ? '本部门' : '全公司' }}</span></div>
      <div class="gauge-wrap">
        <svg class="gauge-svg" viewBox="0 0 200 200">
          <circle class="gauge-track" cx="100" cy="100" r="84" />
          <circle class="gauge-fill" cx="100" cy="100" r="84"
                  :stroke-dasharray="GAUGE_C" :stroke-dashoffset="gaugeOff"
                  transform="rotate(-90 100 100)" />
        </svg>
        <div class="gauge-inner">
          <div class="gauge-pct">{{ aRate }}<em>%</em></div>
          <div class="gauge-label">{{ overview.dataScope === 'DEPT' ? '部门出勤率' : '出勤率' }}</div>
          <div class="gauge-sub">{{ aCheckIn }} / {{ overview.totalUsers }} 人</div>
        </div>
      </div>
      <div class="hero-stats">
        <div class="hero-stat"><strong>{{ aLate }}</strong><span>迟到</span></div>
        <div class="hero-stat"><strong>{{ aEarly }}</strong><span>早退</span></div>
        <div class="hero-stat"><strong>{{ aAbs }}</strong><span>缺勤</span></div>
      </div>
    </div>

    <!-- ── 热力图 ── -->
    <div class="pnl heat" :class="{ in: entered }" style="transition-delay:.43s">
      <div class="pnl-head"><span class="pnl-title">{{ overview.dataScope === 'DEPT' ? '团队周出勤热力' : '部门周出勤热力' }}</span><span class="pnl-badge blue">近 7 日</span></div>
      <div class="heat-grid" :style="{ gridTemplateColumns: `72px repeat(7, 1fr)` }">
        <div></div>
        <div v-for="d in days" :key="d" class="heat-day">{{ d }}</div>
        <template v-for="dept in heatData" :key="dept.name">
          <div class="heat-label">{{ dept.name }}</div>
          <div v-for="(cell, ci) in dept.cells" :key="ci" class="heat-cell"
               :style="{ background: cell.bg, color: cell.color }"
               :title="`${dept.name} ${days[ci]}: ${cell.val}/${dept.total}`">
            {{ cell.display }}
          </div>
        </template>
      </div>
    </div>

    <!-- ── 待审批 ── -->
    <div class="pnl apv" :class="{ in: entered }" style="transition-delay:.48s">
      <div class="pnl-head"><span class="pnl-title">待办审批</span><span class="pnl-badge coral">{{ overview.pendingApprovals }} 件</span></div>
      <div v-for="a in approvals" :key="a.name" class="apv-item">
        <div class="apv-icon" :class="a.type"><component :is="a.iconComp" /></div>
        <div class="apv-body"><div class="apv-name">{{ a.name }}</div><div class="apv-desc">{{ a.desc }}</div></div>
        <span class="apv-time">{{ a.time }}</span>
      </div>
    </div>

    <!-- ── 审批分布 ── -->
    <div class="pnl donut" :class="{ in: entered }" style="transition-delay:.53s">
      <div class="pnl-head"><span class="pnl-title">审批类型</span><span class="pnl-badge blue">本月</span></div>
      <div ref="donutEl" class="donut-chart"></div>
      <div class="donut-legend">
        <div v-for="f in flowData" :key="f.name" class="dl-row">
          <span class="dl-dot" :style="{ background: f.color }"></span>{{ f.name }}<span class="dl-val">{{ f.value }}</span>
        </div>
      </div>
    </div>

    <!-- ── 部门出勤率 ── -->
    <div class="pnl bars" :class="{ in: entered }" style="transition-delay:.58s">
      <div class="pnl-head"><span class="pnl-title">部门出勤率</span><span class="pnl-badge green">今日</span></div>
      <div v-for="b in deptBars" :key="b.name" class="bar-row">
        <span class="bar-name">{{ b.name }}</span>
        <div class="bar-track"><span class="bar-fill" :style="{ background: b.color, width: barsIn ? b.pct + '%' : '0%' }"></span></div>
        <span class="bar-num">{{ b.pct }}%</span>
      </div>
    </div>

    <!-- ── 动态流 ── -->
    <div class="pnl feed" :class="{ in: entered }" style="transition-delay:.63s">
      <div class="pnl-head"><span class="pnl-title">实时动态</span><span class="pnl-badge green">LIVE</span></div>
      <div class="feed-line">
        <div v-for="(ev, i) in liveEvents" :key="i" class="fi">
          <span class="fi-dot" :style="{ '--fi-c': ev.color }"></span>
          <span class="fi-text"><strong>{{ ev.who }}</strong> {{ ev.text }}</span>
          <span class="fi-time">{{ ev.time }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, h, markRaw } from 'vue'
import * as echarts from 'echarts'
import { getDashboardOverviewApi, getWeeklyTrendApi, getDeptHeatmapApi, getFlowDistributionApi } from '@/api/dashboard'

/* ── icon components ── */
const IconCal = markRaw({ render: () => h('svg', { width: 16, height: 16, viewBox: '0 0 16 16', fill: 'none', innerHTML: '<rect x="2" y="3" width="12" height="11" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M5 1v3M11 1v3M2 7h12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>' }) })
const IconClk = markRaw({ render: () => h('svg', { width: 16, height: 16, viewBox: '0 0 16 16', fill: 'none', innerHTML: '<circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.5"/><path d="M8 4.5V8l2.5 1.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>' }) })
const IconChk = markRaw({ render: () => h('svg', { width: 16, height: 16, viewBox: '0 0 16 16', fill: 'none', innerHTML: '<path d="M3 8.5l3.5 3.5L13 5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>' }) })

/* ── constants ── */
const RING_C = 2 * Math.PI * 22
const GAUGE_C = 2 * Math.PI * 84
const days = ['周一','周二','周三','周四','周五','周六','周日']

const FLOW_COLORS = { '请假': '#2563eb', '加班': '#8b5cf6', '补卡': '#f59e0b', '出差': '#10b981' }
const BAR_COLORS = ['#2563eb','#10b981','#f59e0b','#f43f5e','#8b5cf6','#06b6d4','#ec4899']

/* ── reactive data ── */
const overview = ref({ totalUsers: 0, todayCheckIn: 0, todayLate: 0, todayEarly: 0, todayAbsent: 0, pendingApprovals: 0, noticeReadRate: 0, attendanceRate: 0 })
const weeklyTrend = ref([])
const heatRaw = ref([])
const flowData = ref([])
const entered = ref(false), barsIn = ref(false)

/* animated counters */
const aTotalUser = ref(0), aCheckIn = ref(0), aPending = ref(0), aReadRate = ref(0)
const aRate = ref(0), aLate = ref(0), aEarly = ref(0), aAbs = ref(0)

/* ── static fallback (no backend endpoints yet) ── */
const approvals = ref([
  { name: '暂无审批', desc: '当前没有待审批的申请', time: '', type: 'leave', iconComp: IconCal }
])

const liveEvents = ref([
  { who: '系统', text: '数据大屏已连接后端 API', time: '刚刚', color: '#10b981' },
  { who: '系统', text: '考勤数据实时更新中', time: '刚刚', color: '#2563eb' }
])

/* ── sparkline helpers ── */
function spark(vals) {
  const step = 200 / (vals.length - 1)
  const pts = vals.map((v, i) => `${i * step},${40 - v}`)
  return { line: pts.join(' '), fill: `0,40 ${pts.join(' ')} 200,40` }
}

/* ── metrics config ── */
const metrics = computed(() => {
  const sp1 = spark([18,22,20,28,24,32,28,36])
  const sp2 = spark([24,28,22,34,28,38,32,40])
  const sp3 = spark([32,26,28,20,24,16,22,14])
  const sp4 = spark([14,18,20,24,28,32,34,38])
  const rate = overview.value.attendanceRate || 0
  const readRate = overview.value.noticeReadRate || 0
  return [
    { label: '员工总数', animated: `${aTotalUser.value}`, c: '#2563eb', c2: '#3b82f6', trend: overview.value.dataScope === 'DEPT' ? '本部门' : '全公司', dir: 'up', ringOff: RING_C * (1 - (entered.value ? .92 : 0)), sparkLine: sp1.line, sparkFill: sp1.fill },
    { label: '今日打卡', animated: `${aCheckIn.value}`, c: '#10b981', c2: '#34d399', trend: `出勤率 ${rate}%`, dir: 'up', ringOff: RING_C * (1 - (entered.value ? (rate / 100) : 0)), sparkLine: sp2.line, sparkFill: sp2.fill },
    { label: '待办审批', animated: `${aPending.value}`, c: '#f43f5e', c2: '#fb7185', trend: '请及时处理', dir: 'dn', ringOff: RING_C * (1 - (entered.value ? .35 : 0)), sparkLine: sp3.line, sparkFill: sp3.fill },
    { label: '公告阅读率', animated: `${aReadRate.value}<span class="unit">%</span>`, c: '#8b5cf6', c2: '#a78bfa', trend: '实时统计', dir: 'up', ringOff: RING_C * (1 - (entered.value ? (readRate / 100) : 0)), sparkLine: sp4.line, sparkFill: sp4.fill }
  ]
})

const gaugeOff = computed(() => GAUGE_C * (1 - aRate.value / 100))

/* ── heatmap ── */
const heatData = computed(() => heatRaw.value.map(dept => ({
  name: dept.name, total: dept.total,
  cells: (dept.data || []).map((val, i) => {
    const ratio = dept.total > 0 ? val / dept.total : 0
    const isWeekend = i >= 5
    if (isWeekend || val === 0) return { val, display: '-', bg: '#f8fafc', color: '#cbd5e1' }
    if (ratio >= .95) return { val, display: val, bg: '#dbeafe', color: '#1e40af' }
    if (ratio >= .85) return { val, display: val, bg: '#e0f2fe', color: '#0369a1' }
    if (ratio >= .75) return { val, display: val, bg: '#fef3c7', color: '#92400e' }
    return { val, display: val, bg: '#fee2e2', color: '#991b1b' }
  })
})))

/* ── dept bars (derived from heatmap: today's rate per dept) ── */
const deptBars = computed(() => {
  const todayIdx = new Date().getDay() // 0=Sun
  const mapped = todayIdx === 0 ? 6 : todayIdx - 1 // 0=Mon..6=Sun
  return heatRaw.value.map((dept, i) => {
    const present = (dept.data || [])[mapped] || 0
    const pct = dept.total > 0 ? Math.round(present * 1000 / dept.total) / 10 : 0
    return { name: dept.name, pct, color: BAR_COLORS[i % BAR_COLORS.length] }
  })
})

/* ── counters ── */
function countTo(setter, target, dur = 1500) {
  const s = performance.now()
  const step = (ts) => { const p = Math.min((ts - s) / dur, 1); setter(Math.round(target * (1 - Math.pow(1 - p, 3)))); if (p < 1) requestAnimationFrame(step) }
  requestAnimationFrame(step)
}

/* ── echarts ── */
const trendEl = ref(), donutEl = ref()
let trendChart = null, donutChart = null

function renderTrend() {
  if (!trendEl.value) return
  if (!trendChart) trendChart = echarts.init(trendEl.value)
  const wt = weeklyTrend.value
  trendChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e2e8f0', borderWidth: 1, textStyle: { color: '#475569', fontSize: 12, fontFamily: 'Outfit' } },
    legend: { data: ['正常出勤','迟到','早退'], top: 0, right: 0, textStyle: { color: '#94a3b8', fontSize: 11, fontFamily: 'Outfit' }, itemWidth: 16, itemHeight: 6 },
    grid: { left: 40, right: 16, top: 40, bottom: 28 },
    xAxis: { type: 'category', data: wt.map(d => d.day), boundaryGap: false, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisTick: { show: false }, axisLabel: { color: '#94a3b8', fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#94a3b8', fontSize: 10 } },
    series: [
      { name: '正常出勤', type: 'line', stack: 't', smooth: .4, symbol: 'circle', symbolSize: 6, lineStyle: { color: '#2563eb', width: 2.5 }, itemStyle: { color: '#2563eb', borderWidth: 2, borderColor: '#fff' }, areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(37,99,235,0.18)'},{offset:1,color:'rgba(37,99,235,0.01)'}]) }, data: wt.map(d => d.normal) },
      { name: '迟到', type: 'line', stack: 't', smooth: .4, symbol: 'circle', symbolSize: 5, lineStyle: { color: '#f59e0b', width: 2 }, itemStyle: { color: '#f59e0b', borderWidth: 2, borderColor: '#fff' }, areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(245,158,11,0.15)'},{offset:1,color:'rgba(245,158,11,0.01)'}]) }, data: wt.map(d => d.late) },
      { name: '早退', type: 'line', stack: 't', smooth: .4, symbol: 'circle', symbolSize: 5, lineStyle: { color: '#f43f5e', width: 2 }, itemStyle: { color: '#f43f5e', borderWidth: 2, borderColor: '#fff' }, areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(244,63,94,0.12)'},{offset:1,color:'rgba(244,63,94,0.01)'}]) }, data: wt.map(d => d.earlyLeave) }
    ]
  })
}

function renderDonut() {
  if (!donutEl.value) return
  if (!donutChart) donutChart = echarts.init(donutEl.value)
  const fd = flowData.value
  const total = fd.reduce((s, f) => s + f.value, 0)
  donutChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: '#e2e8f0', borderWidth: 1, textStyle: { color: '#475569', fontSize: 12 } },
    graphic: [{ type: 'group', left: 'center', top: 'center', children: [
      { type: 'text', style: { text: String(total), fill: '#0f172a', fontSize: 32, fontWeight: 800, fontFamily: 'Outfit', textAlign: 'center', y: -6 } },
      { type: 'text', style: { text: '总申请', fill: '#94a3b8', fontSize: 11, fontWeight: 500, textAlign: 'center', y: 18 } }
    ]}],
    series: [{ type: 'pie', radius: ['56%','78%'], center: ['50%','50%'], label: { show: false },
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
      emphasis: { scaleSize: 6, itemStyle: { shadowBlur: 12, shadowColor: 'rgba(37,99,235,0.2)' } },
      data: fd.map(f => ({ value: f.value, name: f.name, itemStyle: { color: f.color } }))
    }]
  })
}

function onResize() { trendChart?.resize(); donutChart?.resize() }

/* ── load data from API ── */
async function loadData() {
  try {
    const [ovRes, wtRes, hmRes, fdRes] = await Promise.all([
      getDashboardOverviewApi().catch(() => null),
      getWeeklyTrendApi().catch(() => null),
      getDeptHeatmapApi().catch(() => null),
      getFlowDistributionApi().catch(() => null)
    ])

    if (ovRes?.data) {
      const d = ovRes.data
      overview.value = {
        totalUsers: d.totalUsers || 0,
        todayCheckIn: d.todayCheckIn || 0,
        todayLate: d.todayLate || 0,
        todayEarly: d.todayEarly || 0,
        todayAbsent: d.todayAbsent || 0,
        pendingApprovals: d.pendingApprovals || 0,
        noticeReadRate: d.noticeReadRate || 0,
        attendanceRate: d.attendanceRate || 0
      }
    }

    if (wtRes?.data) {
      weeklyTrend.value = wtRes.data
    }

    if (hmRes?.data) {
      heatRaw.value = hmRes.data.map(d => ({
        name: d.name,
        total: d.total,
        data: Array.isArray(d.data) ? d.data : []
      }))
    }

    if (fdRes?.data) {
      flowData.value = fdRes.data.map(f => ({
        value: f.count || 0,
        name: f.name || f.type,
        color: FLOW_COLORS[f.name] || FLOW_COLORS[f.type] || '#64748b'
      }))
    }
  } catch (e) {
    console.warn('Dashboard API 加载失败，使用默认数据', e)
  }
}

onMounted(async () => {
  /* 先加载数据，再启动动画 */
  await loadData()

  requestAnimationFrame(() => {
    entered.value = true
    const ov = overview.value
    countTo(v => aTotalUser.value = v, ov.totalUsers)
    countTo(v => aCheckIn.value = v, ov.todayCheckIn)
    countTo(v => aPending.value = v, ov.pendingApprovals)
    countTo(v => aReadRate.value = v, ov.noticeReadRate)
    countTo(v => aRate.value = v, ov.attendanceRate, 2200)
    countTo(v => aLate.value = v, ov.todayLate, 1800)
    countTo(v => aEarly.value = v, ov.todayEarly, 1800)
    countTo(v => aAbs.value = v, ov.todayAbsent, 1800)
    setTimeout(() => { barsIn.value = true }, 600)
  })

  renderTrend()
  renderDonut()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
  donutChart?.dispose()
})
</script>

<style>
:root{--blue:#2563eb;--blue-d:#1e40af;--blue-l:#3b82f6;--blue-50:#eff6ff;--blue-100:#dbeafe;
  --coral:#f43f5e;--emerald:#10b981;--amber:#f59e0b;--violet:#8b5cf6;
  --bg:#f0f4f8;--card:#fff;--border:#e2e8f0;
  --t1:#0f172a;--t2:#475569;--t3:#94a3b8;
  --r:14px;--r-sm:10px}
</style>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&family=Noto+Sans+SC:wght@400;500;700&display=swap');

*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
.dash-body{
  font-family:'Outfit','Noto Sans SC',system-ui,sans-serif;
  display:grid;grid-template-columns:repeat(12,1fr);grid-auto-rows:minmax(0,auto);
  gap:20px;padding:24px 32px 40px;max-width:1480px;margin:0 auto;
  color:var(--t1);
  background:var(--bg);
  background-image:
    radial-gradient(ellipse 80% 60% at 15% 10%,rgba(37,99,235,0.04),transparent),
    radial-gradient(ellipse 60% 50% at 85% 80%,rgba(139,92,246,0.03),transparent);
  min-height:100vh;
}

/* Panel base */
.pnl{background:var(--card);border-radius:var(--r);border:1px solid var(--border);padding:22px;position:relative;overflow:hidden;
  box-shadow:0 1px 2px rgba(15,23,42,.04),0 1px 3px rgba(15,23,42,.06);
  transition:box-shadow .35s ease,transform .35s ease,opacity .55s ease;
  opacity:0;transform:translateY(16px)}
.pnl.in{opacity:1;transform:translateY(0)}
.pnl.in:hover{box-shadow:0 4px 6px rgba(15,23,42,.04),0 2px 12px rgba(15,23,42,.06);transform:translateY(-2px)}
.pnl-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}
.pnl-title{font-size:14px;font-weight:600;color:var(--t1);letter-spacing:-.1px}
.pnl-badge{font-size:10px;font-weight:600;padding:3px 10px;border-radius:12px;letter-spacing:.3px}
.pnl-badge.blue{background:var(--blue-50);color:var(--blue)}
.pnl-badge.coral{background:#fff1f2;color:var(--coral)}
.pnl-badge.green{background:#ecfdf5;color:var(--emerald)}

/* Metric cards */
.mc{grid-column:span 3}
.mc::after{content:'';position:absolute;top:0;left:0;right:0;height:3px;border-radius:var(--r) var(--r) 0 0;
  background:linear-gradient(90deg,var(--mc-c,var(--blue)),var(--mc-c2,var(--blue-l)))}
.mc-label{font-size:12px;color:var(--t3);font-weight:500;letter-spacing:.3px;text-transform:uppercase}
.mc-row{display:flex;align-items:flex-end;justify-content:space-between;margin-top:10px}
.mc-val{font-size:34px;font-weight:800;letter-spacing:-1px;line-height:1;color:var(--t1);font-variant-numeric:tabular-nums}
.mc-val :deep(.unit){font-size:16px;font-weight:500;color:var(--t3);margin-left:2px}
.mc-ring{width:52px;height:52px;flex-shrink:0;transform:rotate(-90deg)}
.mc-ring-bg{fill:none;stroke:var(--border);stroke-width:4}
.mc-ring-fg{fill:none;stroke:var(--mc-c,var(--blue));stroke-width:4;stroke-linecap:round;transition:stroke-dashoffset 1.8s cubic-bezier(.4,0,.2,1)}
.mc-foot{display:flex;align-items:center;gap:6px;margin-top:10px;font-size:11px;color:var(--t3);font-weight:500}
.mc-arrow{width:14px;height:14px;border-radius:50%;display:grid;place-items:center}
.mc-arrow svg{width:8px;height:8px}
.mc-arrow.up{background:#ecfdf5;color:var(--emerald)}
.mc-arrow.dn{background:#fff1f2;color:var(--coral)}
.mc-spark{position:absolute;bottom:0;left:0;right:0;height:40px;opacity:.12;pointer-events:none}
.mc-spark polyline{fill:none;stroke:var(--mc-c,var(--blue));stroke-width:2;stroke-linejoin:round}
.mc-spark polygon{fill:var(--mc-c,var(--blue));stroke:none}

/* Trend */
.trend{grid-column:span 7;min-height:340px}
.chart-lg{width:100%;height:280px}

/* Hero gauge */
.hero{grid-column:span 5;min-height:340px;display:flex;flex-direction:column;align-items:center;justify-content:center}
.gauge-wrap{position:relative;width:200px;height:200px;margin:0 auto}
.gauge-svg{width:100%;height:100%}
.gauge-track{fill:none;stroke:var(--blue-50);stroke-width:14}
.gauge-fill{fill:none;stroke:var(--blue);stroke-width:14;stroke-linecap:round;
  transition:stroke-dashoffset 2.2s cubic-bezier(.25,.46,.45,.94);filter:drop-shadow(0 2px 8px rgba(37,99,235,0.25))}
.gauge-inner{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center}
.gauge-pct{font-size:48px;font-weight:800;letter-spacing:-2px;color:var(--t1);line-height:1}
.gauge-pct em{font-style:normal;font-size:22px;color:var(--t3);font-weight:500}
.gauge-label{font-size:12px;color:var(--t3);margin-top:4px;font-weight:500}
.gauge-sub{font-size:13px;color:var(--t2);margin-top:2px;font-weight:600}
.hero-stats{display:flex;gap:28px;margin-top:24px}
.hero-stat{text-align:center}
.hero-stat strong{display:block;font-size:20px;font-weight:700;color:var(--t1);font-variant-numeric:tabular-nums}
.hero-stat span{font-size:11px;color:var(--t3);font-weight:500;margin-top:2px;display:block}

/* Heatmap */
.heat{grid-column:span 5}
.heat-grid{display:grid;gap:4px}
.heat-cell{border-radius:6px;aspect-ratio:1;display:grid;place-items:center;font-size:11px;font-weight:600;transition:transform .2s ease;cursor:default}
.heat-cell:hover{transform:scale(1.15);z-index:2}
.heat-label{font-size:11px;color:var(--t3);font-weight:500;display:flex;align-items:center;padding:0 4px}
.heat-day{font-size:10px;color:var(--t3);font-weight:600;text-align:center;padding-bottom:4px}

/* Approval list */
.apv{grid-column:span 4}
.apv-item{display:flex;align-items:center;gap:12px;padding:12px 14px;border-radius:var(--r-sm);border:1px solid var(--border);margin-bottom:8px;transition:all .2s ease;cursor:pointer}
.apv-item:hover{border-color:var(--blue-100);background:var(--blue-50);transform:translateX(4px)}
.apv-icon{width:36px;height:36px;border-radius:10px;display:grid;place-items:center;flex-shrink:0}
.apv-icon.leave{background:linear-gradient(135deg,#dbeafe,#eff6ff);color:var(--blue)}
.apv-icon.overtime{background:linear-gradient(135deg,#fef3c7,#fffbeb);color:var(--amber)}
.apv-icon.recheck{background:linear-gradient(135deg,#d1fae5,#ecfdf5);color:var(--emerald)}
.apv-body{flex:1;min-width:0}
.apv-name{font-size:13px;font-weight:600;color:var(--t1)}
.apv-desc{font-size:11px;color:var(--t3);margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.apv-time{font-size:10px;color:var(--t3);font-weight:500;white-space:nowrap}

/* Donut */
.donut{grid-column:span 3}
.donut-chart{width:100%;height:180px}
.donut-legend{display:flex;flex-direction:column;gap:8px;margin-top:12px}
.dl-row{display:flex;align-items:center;gap:8px;font-size:12px;color:var(--t2);font-weight:500}
.dl-dot{width:8px;height:8px;border-radius:3px;flex-shrink:0}
.dl-val{margin-left:auto;font-weight:700;color:var(--t1);font-variant-numeric:tabular-nums}

/* Dept bars */
.bars{grid-column:span 4}
.bar-row{display:grid;grid-template-columns:64px 1fr 36px;align-items:center;gap:10px;padding:8px 0}
.bar-row+.bar-row{border-top:1px solid #f1f5f9}
.bar-name{font-size:12px;color:var(--t2);font-weight:500}
.bar-track{height:8px;background:#f1f5f9;border-radius:4px;overflow:hidden}
.bar-fill{height:100%;border-radius:4px;transition:width 1.4s cubic-bezier(.4,0,.2,1)}
.bar-num{font-size:12px;font-weight:700;color:var(--t1);text-align:right;font-variant-numeric:tabular-nums}

/* Feed */
.feed{grid-column:span 4}
.feed-line{position:relative;padding-left:22px}
.feed-line::before{content:'';position:absolute;left:7px;top:18px;bottom:0;width:1.5px;background:var(--border)}
.fi{display:flex;align-items:flex-start;gap:10px;padding:8px 0;position:relative}
.fi-dot{position:absolute;left:-18px;top:12px;width:10px;height:10px;border-radius:50%;border:2px solid var(--card);box-shadow:0 0 0 2px var(--fi-c,var(--blue-100));background:var(--fi-c,var(--blue))}
.fi-text{font-size:12px;color:var(--t2);line-height:1.5;flex:1}
.fi-text strong{color:var(--t1);font-weight:600}
.fi-time{font-size:10px;color:var(--t3);font-weight:500;white-space:nowrap;margin-top:2px}

@media(max-width:1100px){.mc{grid-column:span 6}.trend,.hero{grid-column:span 12}.heat,.apv,.donut,.bars,.feed{grid-column:span 6}}
@media(max-width:720px){.dash-body{padding:16px;gap:14px}.mc,.trend,.hero,.heat,.apv,.donut,.bars,.feed{grid-column:span 12}}
</style>
