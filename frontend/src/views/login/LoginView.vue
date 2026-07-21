<template>
  <div class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <div class="brand-line">OfficeFlow</div>
        <h1>企业智慧 OA 管理系统</h1>
        <p>微服务网关统一鉴权，覆盖员工、考勤、审批、公告和数据统计核心流程。</p>
      </div>

      <el-form class="login-form" :model="form" label-position="top" @submit.prevent="handleLogin">
        <h2>账号登录</h2>
        <el-form-item label="用户名">
          <el-input v-model="form.username" size="large" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" size="large" placeholder="123456" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" @click="handleLogin">登录</el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: 'admin',
  password: '123456'
})

function handleLogin() {
  userStore.login({
    token: 'mock-dev-token',
    profile: {
      username: form.username,
      realName: '系统管理员'
    }
  })
  ElMessage.success('登录成功')
  router.push('/dashboard')
}
</script>

