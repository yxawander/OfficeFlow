import { defineStore } from 'pinia'

const TOKEN_KEY = 'officeflow_token'
const USER_KEY = 'officeflow_user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  actions: {
    login(payload) {
      this.token = payload.token
      this.profile = payload.profile
      localStorage.setItem(TOKEN_KEY, payload.token)
      localStorage.setItem(USER_KEY, JSON.stringify(payload.profile))
    },
    logout() {
      this.token = ''
      this.profile = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})

