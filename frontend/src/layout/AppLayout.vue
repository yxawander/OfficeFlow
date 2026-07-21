<template>
  <el-container class="app-layout">
    <el-aside width="232px" class="app-sidebar">
      <div class="brand">
        <div class="brand-mark">OF</div>
        <div>
          <div class="brand-title">OfficeFlow</div>
          <div class="brand-subtitle">智慧 OA 管理系统</div>
        </div>
      </div>

      <el-menu router :default-active="route.path" class="side-menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据大屏</span>
        </el-menu-item>
        <el-sub-menu index="/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/users">员工管理</el-menu-item>
          <el-menu-item index="/system/roles">角色权限</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/attendance">
          <el-icon><Calendar /></el-icon>
          <span>考勤打卡</span>
        </el-menu-item>
        <el-menu-item index="/flow">
          <el-icon><Tickets /></el-icon>
          <span>审批中心</span>
        </el-menu-item>
        <el-menu-item index="/notice">
          <el-icon><Bell /></el-icon>
          <span>公告通知</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div>
          <div class="page-title">{{ pageTitle }}</div>
          <div class="page-desc">统一网关、权限拦截、前后端分离</div>
        </div>
        <el-dropdown>
          <el-button>
            {{ userStore.profile?.realName || '管理员' }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const pageTitle = computed(() => route.meta.title || 'OfficeFlow')

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>
