<template>
  <div class="login-page">
    <ParticleBg />

    <Transition name="panel-enter">
      <section v-if="showPanel" class="login-panel">
        <!-- 左侧品牌区 -->
        <div class="login-copy">
          <div class="brand-line">
            <span class="brand-glow">OfficeFlow</span>
          </div>
          <h1>企业智慧 OA 管理系统</h1>
          <p>微服务网关统一鉴权，覆盖员工、考勤、审批、公告和数据统计核心流程。</p>
          <div class="brand-dots">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
        </div>

        <!-- 右侧表单区 -->
        <el-form class="login-form" :model="form" label-position="top" @submit.prevent="handleLogin">
          <h2>账号登录</h2>
          <el-form-item label="用户名">
            <el-input v-model="form.username" size="large" placeholder="admin" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" size="large" placeholder="123456" show-password />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form>
      </section>
    </Transition>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import ParticleBg from './ParticleBg.vue'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: 'admin',
  password: '123456'
})

const loading = ref(false)
const showPanel = ref(false)

onMounted(() => {
  setTimeout(() => { showPanel.value = true }, 150)
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ==================== 页面容器 ==================== */
.login-page {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #0a1628 0%, #0d1f3c 40%, #0f172a 100%);
  overflow: hidden;
}

/* ==================== 双栏面板 ==================== */
.login-panel {
  position: relative;
  z-index: 10;
  display: grid;
  grid-template-columns: 1.1fr 420px;
  width: min(980px, 100%);
  min-height: 520px;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(20px) saturate(1.3);
  -webkit-backdrop-filter: blur(20px) saturate(1.3);
  border: 1px solid rgba(139, 211, 255, 0.12);
  border-radius: 16px;
  overflow: hidden;
  box-shadow:
    0 0 50px rgba(50, 140, 255, 0.06),
    0 25px 60px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  animation: panel-float 8s ease-in-out infinite;
}

@keyframes panel-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* 入场动画 */
.panel-enter-active {
  transition: all 0.9s cubic-bezier(0.16, 1, 0.3, 1);
}
.panel-enter-from {
  opacity: 0;
  transform: translateY(50px) scale(0.97);
  filter: blur(10px);
}

/* ==================== 左侧品牌区 ==================== */
.login-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px;
  background: linear-gradient(160deg, rgba(18, 53, 91, 0.85) 0%, rgba(15, 30, 60, 0.9) 100%);
  color: #ffffff;
  position: relative;
  overflow: hidden;
}

/* 品牌区背景光晕 */
.login-copy::before {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  top: -80px;
  left: -80px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(100, 180, 255, 0.08) 0%, transparent 70%);
  animation: copy-glow 6s ease-in-out infinite alternate;
}

.login-copy::after {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  bottom: -40px;
  right: -40px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(139, 211, 255, 0.06) 0%, transparent 70%);
  animation: copy-glow 8s ease-in-out infinite alternate-reverse;
}

@keyframes copy-glow {
  0% { opacity: 0.5; transform: scale(1); }
  100% { opacity: 1; transform: scale(1.2); }
}

.brand-line {
  font-weight: 700;
  position: relative;
  z-index: 1;
}

.brand-glow {
  color: #8bd3ff;
  text-shadow: 0 0 20px rgba(139, 211, 255, 0.4), 0 0 40px rgba(139, 211, 255, 0.15);
  animation: text-glow 3s ease-in-out infinite alternate;
}

@keyframes text-glow {
  0% { text-shadow: 0 0 20px rgba(139, 211, 255, 0.3), 0 0 40px rgba(139, 211, 255, 0.1); }
  100% { text-shadow: 0 0 25px rgba(139, 211, 255, 0.5), 0 0 50px rgba(139, 211, 255, 0.2); }
}

.login-copy h1 {
  margin: 16px 0;
  font-size: 36px;
  line-height: 1.2;
  position: relative;
  z-index: 1;
}

.login-copy p {
  margin: 0;
  max-width: 420px;
  color: #d7e5f4;
  line-height: 1.8;
  position: relative;
  z-index: 1;
}

/* 装饰小圆点 */
.brand-dots {
  display: flex;
  gap: 8px;
  margin-top: 32px;
  position: relative;
  z-index: 1;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(139, 211, 255, 0.4);
  animation: dot-pulse 2s ease-in-out infinite;
}
.dot:nth-child(2) { animation-delay: 0.3s; }
.dot:nth-child(3) { animation-delay: 0.6s; }

@keyframes dot-pulse {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.4); }
}

/* ==================== 右侧表单区 ==================== */
.login-form {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px;
  background: rgba(255, 255, 255, 0.03);
}

.login-form h2 {
  margin: 0 0 28px;
  font-size: 24px;
  color: #e2e8f0;
  font-weight: 500;
  letter-spacing: 1px;
}

/* Element Plus 样式覆盖 — 在 scoped 中用 :deep */
:deep(.el-form-item__label) {
  color: rgba(200, 215, 230, 0.7) !important;
  font-size: 13px;
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(139, 211, 255, 0.12) !important;
  border-radius: 10px !important;
  box-shadow: none !important;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  border-color: rgba(139, 211, 255, 0.25) !important;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(100, 190, 255, 0.5) !important;
  box-shadow: 0 0 20px rgba(100, 180, 255, 0.1), 0 0 4px rgba(100, 180, 255, 0.2) !important;
}

:deep(.el-input__inner) {
  color: #e2e8f0 !important;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(148, 163, 184, 0.4) !important;
}

:deep(.el-input__password) {
  color: rgba(148, 163, 184, 0.5) !important;
}

/* 登录按钮 */
.login-button {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #1a6fb5, #2196f3, #42a5f5) !important;
  border: none !important;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.login-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 25px rgba(33, 150, 243, 0.35), 0 0 40px rgba(33, 150, 243, 0.12);
}

.login-button:active {
  transform: translateY(0);
}

/* 按钮悬停脉冲 */
.login-button::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 10px;
  background: radial-gradient(circle at center, rgba(255, 255, 255, 0.12) 0%, transparent 70%);
  animation: btn-pulse 2.5s ease-in-out infinite;
  pointer-events: none;
}

@keyframes btn-pulse {
  0%, 100% { transform: scale(1); opacity: 0; }
  50% { transform: scale(1.04); opacity: 1; }
}

/* ==================== 响应式 ==================== */
@media (max-width: 820px) {
  .login-panel {
    grid-template-columns: 1fr;
  }

  .login-copy {
    padding: 36px;
  }

  .login-copy h1 {
    font-size: 28px;
  }

  .login-form {
    padding: 36px;
  }
}
</style>
