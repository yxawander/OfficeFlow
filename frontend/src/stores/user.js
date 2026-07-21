import { defineStore } from 'pinia'
import { getProfileApi, loginApi, logoutApi } from '@/api/user'

const TOKEN_KEY = 'officeflow_token'
const USER_KEY = 'officeflow_user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  actions: {
    setSession(payload) {
      this.token = payload.token || ''
      this.profile = payload.profile || null
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.profile))
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
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
