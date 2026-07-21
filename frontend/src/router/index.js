import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue')
  },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '数据大屏', icon: 'DataAnalysis' }
      },
      {
        path: 'system/users',
        name: 'system-users',
        component: () => import('@/views/system/UserView.vue'),
        meta: { title: '员工管理', icon: 'User' }
      },
      {
        path: 'system/roles',
        name: 'system-roles',
        component: () => import('@/views/system/RoleView.vue'),
        meta: { title: '角色权限', icon: 'Lock' }
      },
      {
        path: 'attendance',
        name: 'attendance',
        component: () => import('@/views/attendance/AttendanceView.vue'),
        meta: { title: '考勤打卡', icon: 'Calendar' }
      },
      {
        path: 'flow',
        name: 'flow',
        component: () => import('@/views/flow/FlowView.vue'),
        meta: { title: '审批中心', icon: 'Tickets' }
      },
      {
        path: 'notice',
        name: 'notice',
        component: () => import('@/views/notice/NoticeView.vue'),
        meta: { title: '公告通知', icon: 'Bell' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    return '/login'
  }
  if (to.path === '/login' && userStore.token) {
    return '/dashboard'
  }
  return true
})

export default router
