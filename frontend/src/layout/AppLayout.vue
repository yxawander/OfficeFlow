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
        <template v-for="item in visibleMenus" :key="item.id">
          <el-sub-menu v-if="menuChildren(item).length" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon || 'Menu'" /></el-icon>
              <span>{{ item.menuName }}</span>
            </template>
            <el-menu-item v-for="child in menuChildren(item)" :key="child.id" :index="child.path">
              {{ child.menuName }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon || 'Menu'" /></el-icon>
            <span>{{ item.menuName }}</span>
          </el-menu-item>
        </template>
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

const visibleMenus = computed(() => (userStore.menus || []).filter((item) => item.visible === 1 && item.menuType !== 'BUTTON'))
const pageTitle = computed(() => route.meta.title || 'OfficeFlow')

function menuChildren(item) {
  return (item.children || []).filter((child) => child.visible === 1 && child.menuType !== 'BUTTON')
}

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>
