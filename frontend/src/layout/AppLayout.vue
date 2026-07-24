<!-- 
  AppLayout.vue
  主要布局组件，包含左侧边栏 (Sidebar)、顶部导航栏 (Header) 以及主内容区域 (Main)。
-->
<template>
  <el-container class="app-layout">
    <!-- 左侧边栏：控制菜单导航与品牌标志展示，支持折叠 -->
    <el-aside :width="sidebarCollapsed ? '72px' : '232px'" class="app-sidebar">
      <div class="brand">
        <div class="brand-mark">OF</div>
        <div v-show="!sidebarCollapsed" class="brand-copy">
          <div class="brand-title">OfficeFlow</div>
          <div class="brand-subtitle">智慧 OA 管理系统</div>
        </div>
      </div>

      <!-- 侧边菜单栏：动态渲染可见的路由菜单项 -->
      <el-menu router :default-active="route.path" class="side-menu" :collapse="sidebarCollapsed" :collapse-transition="false">
        <template v-for="item in visibleMenus" :key="item.id">
          <!-- 包含子菜单的项 -->
          <el-sub-menu v-if="menuChildren(item).length" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon || 'Menu'" /></el-icon>
              <span>{{ item.menuName }}</span>
            </template>
            <el-menu-item v-for="child in menuChildren(item)" :key="child.id" :index="child.path">
              {{ child.menuName }}
            </el-menu-item>
          </el-sub-menu>
          <!-- 无子菜单的顶级独立菜单 -->
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon || 'Menu'" /></el-icon>
            <span>{{ item.menuName }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航区：包含折叠按钮、页面标题和个人下拉菜单 -->
      <el-header class="app-header">
        <div class="header-left">
          <!-- 侧边栏折叠/展开控制按钮 -->
          <el-button
            class="sidebar-toggle"
            :icon="sidebarCollapsed ? Expand : Fold"
            circle
            @click="toggleSidebar"
          />
          <div>
            <!-- 显示当前路由所配置的标题 -->
            <div class="page-title">{{ pageTitle }}</div>
            <div class="page-desc">统一网关、权限拦截、前后端分离</div>
          </div>
        </div>
        
        <!-- 用户信息下拉菜单 -->
        <el-dropdown>
          <el-button>
            {{ userStore.profile?.realName || '管理员' }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 核心页面视图容器 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Expand, Fold } from '@element-plus/icons-vue'

// 路由与状态存储实例初始化
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 控制左侧边栏展开/折叠状态的响应式变量
const sidebarCollapsed = ref(false)

/**
 * 计算属性：获取可见的菜单列表。
 * 从 userStore 中获取当前用户的权限菜单，
 * 并过滤掉隐藏的菜单或仅仅是按钮类型的权限节点。
 */
const visibleMenus = computed(() => {
  const menus = userStore.menus || []
  // 过滤：只保留 visible === 1 且不为按钮的项
  const filtered = menus.filter((item) => item.visible === 1 && item.menuType !== 'BUTTON')
  // 深拷贝，避免直接修改状态中的原始数据 (Deep clone and patch)
  const patched = JSON.parse(JSON.stringify(filtered))
  
  // 递归处理方法：修补冗长的菜单名称，提升 UI 显示效果
  const patchMenuName = (list) => {
    for (const item of list) {
      if (item.menuName === '部门今日考勤实时监控' || item.path === '/attendance/dept') {
        item.menuName = '部门考勤监控'
      }
      if (item.children && item.children.length) {
        patchMenuName(item.children)
      }
    }
  }
  patchMenuName(patched)
  return patched
})

// 计算属性：当前页面的标题，如果在路由 meta 中未定义则回退到 'OfficeFlow'
const pageTitle = computed(() => route.meta.title || 'OfficeFlow')

/**
 * 辅助函数：获取特定菜单项下应该显示在侧边栏的子菜单
 * 会过滤掉隐藏的项和按钮类型权限。
 * @param {Object} item - 菜单节点对象
 * @returns {Array} 过滤后的子菜单数组
 */
function menuChildren(item) {
  return (item.children || []).filter((child) => child.visible === 1 && child.menuType !== 'BUTTON')
}

// 跳转到个人中心页面
function goProfile() {
  router.push('/profile')
}

// 切换侧边栏的折叠/展开状态
function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

// 处理用户退出登录的逻辑
async function handleLogout() {
  await userStore.logout() // 清除相关 token 与用户状态
  router.push('/login')    // 重定向至登录页
}
</script>
