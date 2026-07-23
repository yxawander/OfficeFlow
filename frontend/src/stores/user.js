import { defineStore } from 'pinia'
import { getProfileApi, loginApi, logoutApi } from '@/api/user'

const TOKEN_KEY = 'officeflow_token'
const USER_KEY = 'officeflow_user'
const MENUS_KEY = 'officeflow_menus'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
    menus: JSON.parse(localStorage.getItem(MENUS_KEY) || '[]')
  }),
  getters: {
    // 判断当前登录用户是否拥有某角色（兼容 userType / roleCode / roles[].roleCode）
    hasRole: (state) => (roleCode) => {
      const user = state.profile
      if (!user) return false
      if ((user.userType || '') === roleCode) return true
      if ((user.roleCode || '') === roleCode) return true
      const roles = Array.isArray(user.roles) ? user.roles : []
      return roles.some((r) => (typeof r === 'string' ? r : r?.roleCode) === roleCode)
    }
  },
  actions: {
    setSession(payload) {
      this.token = payload.token || ''
      this.profile = payload.profile || null
      this.menus = payload.menus || []
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.profile))
      localStorage.setItem(MENUS_KEY, JSON.stringify(this.menus))
    },
    async login(form) {
      const res = await loginApi(form)
      this.setSession(res.data)
      return res.data
    },
    async loadProfile() {
      if (!this.token) return null
      const res = await getProfileApi()
      this.profile = res.data
      localStorage.setItem(USER_KEY, JSON.stringify(this.profile))
      return res.data
    },
    async logout() {
      if (this.token) {
        await logoutApi().catch(() => {})
      }
      this.token = ''
      this.profile = null
      this.menus = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(MENUS_KEY)
    }
  }
})
