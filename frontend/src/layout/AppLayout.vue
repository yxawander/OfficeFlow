<!-- 
  AppLayout.vue
  主要布局组件，包含左侧边栏 (Sidebar)、顶部导航栏 (Header) 以及主内容区域 (Main)。
-->
<template>
  <el-container class="app-layout">
    <!-- 左侧边栏：控制菜单导航与品牌标志展示，支持折叠 -->
    <el-aside :width="sidebarCollapsed ? '72px' : '232px'" class="app-sidebar">
      <!-- 极光动效背景层 -->
      <div class="aurora-blobs" aria-hidden="true">
        <span class="aurora aurora-1"></span>
        <span class="aurora aurora-2"></span>
        <span class="aurora aurora-3"></span>
      </div>

      <div class="brand">
        <div class="brand-mark">OF</div>
        <div v-show="!sidebarCollapsed" class="brand-copy">
          <div class="brand-title">OfficeFlow</div>
          <div class="brand-subtitle">智慧 OA 管理系统</div>
        </div>
      </div>

      <!-- 侧边菜单栏：动态渲染可见的路由菜单项 -->
      <el-menu
        router
        :default-active="route.path"
        class="side-menu"
        :collapse="sidebarCollapsed"
        :collapse-transition="false"
        background-color="transparent"
        text-color="rgba(255,255,255,0.88)"
        active-text-color="#0d6e7e"
      >
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

<style scoped>
/* ================================================================
   Aurora Luminous — OfficeFlow Sidebar Design System
   ================================================================
   中等明度青色基底，极光色块漂浮动画，白色文字增强可读性，
   交错入场动效 + 滑动激活指示器 + 品牌旋转光环。
   ================================================================ */

/* ==================== 全局布局 ==================== */
.app-layout {
  height: 100vh;
  overflow: hidden;
}

/* ==================== 侧边栏容器 ==================== */
.app-sidebar {
  background: linear-gradient(170deg, #4dd4e8 0%, #38bacc 50%, #2ca3b8 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
}

/* 微点阵纹理叠层 */
.app-sidebar::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(255, 255, 255, 0.07) 0.7px, transparent 0.7px);
  background-size: 22px 22px;
  pointer-events: none;
  z-index: 0;
}

/* ==================== 极光动效背景 ==================== */
.aurora-blobs {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.aurora {
  position: absolute;
  border-radius: 50%;
  filter: blur(50px);
  will-change: transform;
}

.aurora-1 {
  width: 180px;
  height: 180px;
  top: -30px;
  right: -50px;
  background: rgba(99, 102, 241, 0.22);
  animation: aurora-drift-1 12s ease-in-out infinite;
}

.aurora-2 {
  width: 150px;
  height: 150px;
  top: 45%;
  left: -40px;
  background: rgba(34, 211, 238, 0.28);
  animation: aurora-drift-2 16s ease-in-out infinite;
}

.aurora-3 {
  width: 120px;
  height: 120px;
  bottom: 60px;
  right: -30px;
  background: rgba(129, 140, 248, 0.18);
  animation: aurora-drift-3 20s ease-in-out infinite;
}

@keyframes aurora-drift-1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-35px, 55px) scale(1.25); }
  66% { transform: translate(25px, -25px) scale(0.85); }
}

@keyframes aurora-drift-2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(55px, -45px) scale(1.18); }
  66% { transform: translate(-25px, 35px) scale(0.88); }
}

@keyframes aurora-drift-3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-45px, -35px) scale(1.3); }
}

/* ==================== 品牌区域 ==================== */
.brand {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 24px 18px 20px;
  position: relative;
  z-index: 1;
}

/* 动画渐变分隔线 */
.brand::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 24px;
  right: 24px;
  height: 2px;
  border-radius: 1px;
  background: linear-gradient(90deg,
    transparent 0%, rgba(255, 255, 255, 0.5) 30%,
    rgba(129, 140, 248, 0.4) 50%, rgba(255, 255, 255, 0.5) 70%, transparent 100%);
  background-size: 200% 100%;
  animation: brand-line-flow 4s linear infinite;
}

@keyframes brand-line-flow {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 品牌 Logo — 旋转渐变光环 + 白色内核 */
.brand-mark {
  position: relative;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  font-weight: 800;
  font-size: 13px;
  color: #0d7f8f;
  letter-spacing: 0.5px;
  z-index: 1;
  box-shadow: 0 3px 15px rgba(0, 60, 80, 0.18);
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.brand-mark:hover {
  transform: scale(1.1) rotate(3deg);
}

/* 旋转 conic-gradient 光环 */
.brand-mark::before {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 15px;
  background: conic-gradient(
    from 0deg,
    transparent 0%,
    rgba(255, 255, 255, 0.75) 15%,
    transparent 30%,
    rgba(129, 140, 248, 0.5) 50%,
    transparent 65%,
    rgba(255, 255, 255, 0.75) 80%,
    transparent 100%
  );
  animation: brand-ring-spin 5s linear infinite;
  z-index: -1;
}

@keyframes brand-ring-spin {
  to { transform: rotate(360deg); }
}

.brand-copy {
  overflow: hidden;
  white-space: nowrap;
}

.brand-title {
  font-size: 17px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 0.3px;
  text-shadow:
    0 1px 4px rgba(0, 50, 65, 0.22),
    0 0 20px rgba(255, 255, 255, 0.08);
  line-height: 1.2;
}

.brand-subtitle {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.72);
  margin-top: 3px;
  letter-spacing: 1.5px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 50, 65, 0.15);
}

/* ==================== 侧边菜单 ==================== */
.side-menu {
  flex: 1;
  border-right: none !important;
  background: transparent !important;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  z-index: 1;
  padding-top: 10px;
}

.side-menu::-webkit-scrollbar { width: 3px; }
.side-menu::-webkit-scrollbar-track { background: transparent; }
.side-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.25);
  border-radius: 3px;
}

/* ==================== 菜单项 — 交错入场动画 ==================== */
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  position: relative;
  color: rgba(255, 255, 255, 0.92) !important;
  border-radius: 10px !important;
  margin: 4px 10px !important;
  height: 46px !important;
  line-height: 46px !important;
  font-size: 14.5px !important;
  font-weight: 600 !important;
  letter-spacing: 0.2px !important;
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1) !important;
  text-shadow: 0 1px 3px rgba(0, 50, 65, 0.2);
  opacity: 0;
  transform: translateX(-14px);
  animation: menu-slide-in 0.5s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

/* 交错延迟 — 逐项渐显 */
:deep(.el-menu > li:nth-child(1) > .el-menu-item),
:deep(.el-menu > li:nth-child(1) > .el-sub-menu__title) { animation-delay: 0.06s; }
:deep(.el-menu > li:nth-child(2) > .el-menu-item),
:deep(.el-menu > li:nth-child(2) > .el-sub-menu__title) { animation-delay: 0.12s; }
:deep(.el-menu > li:nth-child(3) > .el-menu-item),
:deep(.el-menu > li:nth-child(3) > .el-sub-menu__title) { animation-delay: 0.18s; }
:deep(.el-menu > li:nth-child(4) > .el-menu-item),
:deep(.el-menu > li:nth-child(4) > .el-sub-menu__title) { animation-delay: 0.24s; }
:deep(.el-menu > li:nth-child(5) > .el-menu-item),
:deep(.el-menu > li:nth-child(5) > .el-sub-menu__title) { animation-delay: 0.30s; }
:deep(.el-menu > li:nth-child(6) > .el-menu-item),
:deep(.el-menu > li:nth-child(6) > .el-sub-menu__title) { animation-delay: 0.36s; }
:deep(.el-menu > li:nth-child(7) > .el-menu-item),
:deep(.el-menu > li:nth-child(7) > .el-sub-menu__title) { animation-delay: 0.42s; }
:deep(.el-menu > li:nth-child(8) > .el-menu-item),
:deep(.el-menu > li:nth-child(8) > .el-sub-menu__title) { animation-delay: 0.48s; }
:deep(.el-menu > li:nth-child(9) > .el-menu-item),
:deep(.el-menu > li:nth-child(9) > .el-sub-menu__title) { animation-delay: 0.54s; }
:deep(.el-menu > li:nth-child(10) > .el-menu-item),
:deep(.el-menu > li:nth-child(10) > .el-sub-menu__title) { animation-delay: 0.60s; }

@keyframes menu-slide-in {
  to { opacity: 1; transform: translateX(0); }
}

/* 图标样式 */
:deep(.el-menu-item .el-icon),
:deep(.el-sub-menu__title .el-icon) {
  color: rgba(255, 255, 255, 0.85) !important;
  font-size: 19px !important;
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

/* ==================== 悬停状态 ==================== */
:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.16) !important;
  color: #ffffff !important;
  transform: translateX(3px) !important;
}

:deep(.el-menu-item:hover .el-icon),
:deep(.el-sub-menu__title:hover .el-icon) {
  color: #ffffff !important;
  transform: scale(1.15);
}

/* ==================== 激活状态 ==================== */
/* ::before — 左侧滑入指示条 */
:deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: -10px;
  top: 50%;
  transform: translateY(-50%);
  width: 3.5px;
  height: 22px;
  border-radius: 0 3px 3px 0;
  background: #ffffff;
  box-shadow: 0 0 12px rgba(255, 255, 255, 0.6);
  animation: indicator-slide 0.35s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

@keyframes indicator-slide {
  from { height: 0; opacity: 0; }
  to { height: 22px; opacity: 1; }
}

/* ::after — 激活项微光扫过 */
:deep(.el-menu-item.is-active::after) {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 10px;
  background: linear-gradient(
    105deg,
    transparent 30%,
    rgba(255, 255, 255, 0.22) 48%,
    rgba(255, 255, 255, 0.22) 52%,
    transparent 70%
  );
  background-size: 280% 100%;
  animation: shimmer-sweep 3.5s ease-in-out infinite;
  pointer-events: none;
}

@keyframes shimmer-sweep {
  0% { background-position: 250% 0; }
  100% { background-position: -250% 0; }
}

:deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.94) !important;
  color: #0b6b7a !important;
  font-weight: 700 !important;
  text-shadow: none !important;
  box-shadow:
    0 3px 16px rgba(0, 50, 65, 0.14),
    0 0 0 1px rgba(255, 255, 255, 0.4),
    0 0 30px rgba(255, 255, 255, 0.08) !important;
  transform: translateX(0) !important;
}

:deep(.el-menu-item.is-active .el-icon) {
  color: #0b6b7a !important;
}

/* ==================== 子菜单 ==================== */
:deep(.el-sub-menu .el-menu) {
  background: transparent !important;
}

:deep(.el-sub-menu .el-menu .el-menu-item) {
  height: 40px !important;
  line-height: 40px !important;
  padding-left: 52px !important;
  font-size: 13.5px !important;
  margin: 2px 10px !important;
  animation-delay: 0.04s !important;
}

:deep(.el-sub-menu__icon-arrow) {
  color: rgba(255, 255, 255, 0.5) !important;
  transition: transform 0.3s ease !important;
}

/* ==================== 折叠弹出菜单 ==================== */
:deep(.el-menu--popup) {
  background: linear-gradient(170deg, #40c4d8 0%, #2ea8bc 100%) !important;
  border: 1px solid rgba(255, 255, 255, 0.28) !important;
  border-radius: 14px !important;
  box-shadow:
    0 16px 48px rgba(0, 60, 80, 0.25),
    0 4px 12px rgba(0, 60, 80, 0.12) !important;
  padding: 8px 0 !important;
  backdrop-filter: blur(16px);
  animation: popup-appear 0.25s cubic-bezier(0.22, 1, 0.36, 1) !important;
}

@keyframes popup-appear {
  from { opacity: 0; transform: scale(0.92) translateX(-8px); }
  to { opacity: 1; transform: scale(1) translateX(0); }
}

:deep(.el-menu--popup .el-menu-item) {
  color: rgba(255, 255, 255, 0.9) !important;
  margin: 3px 8px !important;
  border-radius: 9px !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  opacity: 1 !important;
  transform: none !important;
  animation: none !important;
}

:deep(.el-menu--popup .el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.2) !important;
  color: #ffffff !important;
  transform: translateX(3px) !important;
}

:deep(.el-menu--popup .el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.94) !important;
  color: #0b6b7a !important;
}

:deep(.el-menu--popup .el-menu-item.is-active::before),
:deep(.el-menu--popup .el-menu-item.is-active::after) {
  display: none;
}

/* ==================== 顶部导航栏 ==================== */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid rgba(56, 186, 204, 0.12);
  padding: 0 28px;
  height: 62px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sidebar-toggle {
  border: none !important;
  background: rgba(56, 186, 204, 0.08) !important;
  color: #1a8fa3 !important;
  transition: all 0.25s ease !important;
}

.sidebar-toggle:hover {
  background: rgba(56, 186, 204, 0.18) !important;
  color: #0d6e7e !important;
  transform: scale(1.05);
}

.page-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.3;
}

.page-desc {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
  letter-spacing: 0.3px;
}

/* ==================== 主内容区 ==================== */
.app-main {
  background: #f3fafb;
  padding: 20px;
  overflow-y: auto;
}
</style>
