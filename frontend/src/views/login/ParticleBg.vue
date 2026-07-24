<template>
  <canvas ref="canvasRef" class="particle-bg" />
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const canvasRef = ref(null)
let animId = 0
let w = 0
let h = 0
let ctx

/* ---------- Particles ---------- */
const particles = []
const PARTICLE_COUNT = 55
const LINK_DIST = 150

function initParticles() {
  particles.length = 0
  for (let i = 0; i < PARTICLE_COUNT; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.35,
      vy: (Math.random() - 0.5) * 0.35,
      r: Math.random() * 1.6 + 0.6,
      // 蓝青色调，与品牌 #12355b 和 #8bd3ff 呼应
      color: `hsla(${200 + Math.random() * 20}, 70%, ${55 + Math.random() * 15}%, ${0.4 + Math.random() * 0.5})`
    })
  }
}

function drawParticles() {
  for (const p of particles) {
    p.x += p.vx; p.y += p.vy
    if (p.x < 0 || p.x > w) p.vx *= -1
    if (p.y < 0 || p.y > h) p.vy *= -1
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = p.color
    ctx.fill()
  }
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < LINK_DIST) {
        const alpha = (1 - dist / LINK_DIST) * 0.18
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.strokeStyle = `rgba(100,180,255,${alpha})`
        ctx.lineWidth = 0.5
        ctx.stroke()
      }
    }
  }
}

/* ---------- Grid ---------- */
const GRID_SIZE = 70
let gridOffset = 0
function drawGrid() {
  gridOffset = (gridOffset + 0.12) % GRID_SIZE
  ctx.strokeStyle = 'rgba(100,180,255,0.03)'
  ctx.lineWidth = 0.5
  ctx.beginPath()
  for (let x = -GRID_SIZE + gridOffset; x < w + GRID_SIZE; x += GRID_SIZE) {
    ctx.moveTo(x, 0); ctx.lineTo(x, h)
  }
  for (let y = -GRID_SIZE + gridOffset; y < h + GRID_SIZE; y += GRID_SIZE) {
    ctx.moveTo(0, y); ctx.lineTo(w, y)
  }
  ctx.stroke()
}

/* ---------- Binary Rain ---------- */
const rainDrops = []
const RAIN_COUNT = 14
function initRain() {
  rainDrops.length = 0
  for (let i = 0; i < RAIN_COUNT; i++) {
    const len = Math.floor(Math.random() * 10) + 5
    const chars = []
    for (let j = 0; j < len; j++) chars.push(Math.random() > 0.5 ? '1' : '0')
    rainDrops.push({
      x: Math.random() * w,
      y: Math.random() * h,
      speed: 0.25 + Math.random() * 0.5,
      chars,
      len
    })
  }
}
function drawRain() {
  ctx.font = '11px monospace'
  for (const d of rainDrops) {
    d.y += d.speed
    if (d.y - d.len * 14 > h) {
      d.y = -d.len * 14
      d.x = Math.random() * w
    }
    for (let i = 0; i < d.chars.length; i++) {
      const cy = d.y - i * 14
      if (cy < 0 || cy > h) continue
      const alpha = (1 - i / d.chars.length) * 0.08
      ctx.fillStyle = `rgba(80,180,255,${alpha})`
      ctx.fillText(d.chars[i], d.x, cy)
    }
  }
}

/* ---------- Floating Geometry ---------- */
const geos = []
const GEO_COUNT = 6
function initGeos() {
  geos.length = 0
  const types = ['tri', 'sq', 'hex']
  for (let i = 0; i < GEO_COUNT; i++) {
    geos.push({
      x: Math.random() * w,
      y: Math.random() * h,
      size: 12 + Math.random() * 25,
      rot: Math.random() * Math.PI * 2,
      rotSpeed: (Math.random() - 0.5) * 0.006,
      vx: (Math.random() - 0.5) * 0.15,
      vy: (Math.random() - 0.5) * 0.15,
      type: types[i % 3],
      alpha: 0.025 + Math.random() * 0.04
    })
  }
}
function drawGeos() {
  for (const g of geos) {
    g.x += g.vx; g.y += g.vy; g.rot += g.rotSpeed
    if (g.x < -50 || g.x > w + 50) g.vx *= -1
    if (g.y < -50 || g.y > h + 50) g.vy *= -1
    ctx.save()
    ctx.translate(g.x, g.y)
    ctx.rotate(g.rot)
    ctx.strokeStyle = `rgba(100,180,255,${g.alpha})`
    ctx.lineWidth = 0.8
    ctx.beginPath()
    if (g.type === 'tri') {
      const s = g.size
      ctx.moveTo(0, -s); ctx.lineTo(s * 0.866, s * 0.5); ctx.lineTo(-s * 0.866, s * 0.5)
      ctx.closePath()
    } else if (g.type === 'sq') {
      const half = g.size * 0.7
      ctx.rect(-half, -half, half * 2, half * 2)
    } else {
      const s = g.size * 0.8
      for (let i = 0; i < 6; i++) {
        const a = (Math.PI / 3) * i
        const px = s * Math.cos(a), py = s * Math.sin(a)
        i === 0 ? ctx.moveTo(px, py) : ctx.lineTo(px, py)
      }
      ctx.closePath()
    }
    ctx.stroke()
    ctx.restore()
  }
}

/* ---------- Pulse Rings ---------- */
const rings = []
let ringTimer = 0
function updateRings() {
  ringTimer++
  if (ringTimer % 220 === 0 && rings.length < 3) {
    rings.push({
      x: w * 0.2 + Math.random() * w * 0.6,
      y: h * 0.2 + Math.random() * h * 0.6,
      r: 0, maxR: 100 + Math.random() * 80, alpha: 0.1
    })
  }
  for (let i = rings.length - 1; i >= 0; i--) {
    const ring = rings[i]
    ring.r += 0.5
    ring.alpha = 0.1 * (1 - ring.r / ring.maxR)
    if (ring.r >= ring.maxR) { rings.splice(i, 1); continue }
    ctx.beginPath()
    ctx.arc(ring.x, ring.y, ring.r, 0, Math.PI * 2)
    ctx.strokeStyle = `rgba(80,160,255,${ring.alpha})`
    ctx.lineWidth = 1
    ctx.stroke()
  }
}

/* ---------- Data Flow Lines ---------- */
const flowLines = []
function initFlowLines() {
  flowLines.length = 0
  for (let i = 0; i < 6; i++) {
    flowLines.push({
      x: Math.random() * w,
      y: Math.random() * h,
      len: 30 + Math.random() * 60,
      speed: 0.4 + Math.random() * 0.8,
      angle: Math.random() * Math.PI * 2,
      alpha: 0.04 + Math.random() * 0.06
    })
  }
}
function drawFlowLines() {
  for (const f of flowLines) {
    f.x += Math.cos(f.angle) * f.speed
    f.y += Math.sin(f.angle) * f.speed
    if (f.x < -100 || f.x > w + 100 || f.y < -100 || f.y > h + 100) {
      f.x = Math.random() * w; f.y = Math.random() * h
      f.angle = Math.random() * Math.PI * 2
    }
    const ex = f.x - Math.cos(f.angle) * f.len
    const ey = f.y - Math.sin(f.angle) * f.len
    const grad = ctx.createLinearGradient(f.x, f.y, ex, ey)
    grad.addColorStop(0, `rgba(80,180,255,${f.alpha})`)
    grad.addColorStop(1, 'rgba(80,180,255,0)')
    ctx.beginPath()
    ctx.moveTo(f.x, f.y)
    ctx.lineTo(ex, ey)
    ctx.strokeStyle = grad
    ctx.lineWidth = 1.2
    ctx.stroke()
  }
}

/* ---------- Main Loop ---------- */
function draw() {
  ctx.clearRect(0, 0, w, h)
  drawGrid()
  drawRain()
  drawFlowLines()
  drawGeos()
  drawParticles()
  updateRings()
  animId = requestAnimationFrame(draw)
}

function resize() {
  const c = canvasRef.value
  if (!c) return
  w = c.width = window.innerWidth
  h = c.height = window.innerHeight
  initParticles()
  initRain()
  initGeos()
  initFlowLines()
}

onMounted(() => {
  const c = canvasRef.value
  if (!c) return
  ctx = c.getContext('2d')
  resize()
  draw()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
.particle-bg {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  z-index: 0;
  pointer-events: none;
}
</style>
