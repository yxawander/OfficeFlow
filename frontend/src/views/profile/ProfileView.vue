<template>
  <section class="profile-page">
    <div class="profile-summary panel">
      <div class="profile-avatar">{{ initials }}</div>
      <div class="profile-main">
        <h2>{{ profile?.realName || '未命名用户' }}</h2>
        <p>{{ profile?.username || '-' }}</p>
        <div class="profile-tags">
          <el-tag effect="plain">{{ profile?.deptName || '未分配部门' }}</el-tag>
          <el-tag effect="plain" type="success">{{ profile?.postName || '未分配岗位' }}</el-tag>
          <el-tag effect="plain" type="warning">{{ userTypeText(profile?.userType) }}</el-tag>
        </div>
      </div>
    </div>

    <div class="profile-grid">
      <section class="panel profile-info-panel">
        <div class="panel-header">
          <div>
            <h2>个人信息</h2>
            <p class="panel-subtitle">查看并维护自己的基础联系方式</p>
          </div>
          <el-button type="primary" :icon="Refresh" @click="loadProfile">刷新</el-button>
        </div>

        <el-descriptions :column="2" border class="profile-desc">
          <el-descriptions-item label="账号">{{ profile?.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ profile?.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderText(profile?.gender) }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ profile?.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ profile?.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ profile?.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="岗位">{{ profile?.postName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="直属领导">{{ profile?.managerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户类型">{{ userTypeText(profile?.userType) }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ profile?.lastLoginAt || '-' }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="panel profile-edit-panel">
        <div class="panel-header">
          <div>
            <h2>修改信息</h2>
            <p class="panel-subtitle">部门、岗位、角色需要管理员在员工管理中维护</p>
          </div>
        </div>

        <el-form :model="profileForm" label-width="86px" class="profile-form">
          <el-form-item label="姓名">
            <el-input v-model="profileForm.realName" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="profileForm.gender" style="width: 100%">
              <el-option label="未知" :value="0" />
              <el-option label="男" :value="1" />
              <el-option label="女" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" />
          </el-form-item>
          <el-form-item label="头像地址">
            <el-input v-model="profileForm.avatar" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存个人信息</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="panel password-panel">
        <div class="panel-header">
          <div>
            <h2>修改密码</h2>
            <p class="panel-subtitle">修改成功后建议重新登录确认新密码</p>
          </div>
        </div>

        <el-form :model="passwordForm" label-width="86px" class="password-form">
          <el-form-item label="原密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingPassword" @click="savePassword">修改密码</el-button>
            <el-button @click="resetPasswordForm">清空</el-button>
          </el-form-item>
        </el-form>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { changePasswordApi, getProfileApi, updateProfileApi } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const profile = computed(() => userStore.profile || {})
const savingProfile = ref(false)
const savingPassword = ref(false)

const profileForm = reactive({
  realName: '',
  gender: 0,
  phone: '',
  email: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const initials = computed(() => {
  const name = profile.value?.realName || profile.value?.username || 'OF'
  return String(name).slice(0, 2).toUpperCase()
})

function fillProfileForm(data) {
  profileForm.realName = data?.realName || ''
  profileForm.gender = data?.gender ?? 0
  profileForm.phone = data?.phone || ''
  profileForm.email = data?.email || ''
  profileForm.avatar = data?.avatar || ''
}

async function loadProfile() {
  const res = await getProfileApi()
  userStore.profile = res.data
  localStorage.setItem('officeflow_user', JSON.stringify(res.data))
  fillProfileForm(res.data)
}

async function saveProfile() {
  if (!profileForm.realName.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  savingProfile.value = true
  try {
    const res = await updateProfileApi(profileForm)
    userStore.profile = res.data
    localStorage.setItem('officeflow_user', JSON.stringify(res.data))
    fillProfileForm(res.data)
    ElMessage.success('个人信息已更新')
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPassword.value = true
  try {
    await changePasswordApi({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    resetPasswordForm()
    ElMessage.success('密码已修改')
  } finally {
    savingPassword.value = false
  }
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

function genderText(value) {
  if (value === 1) return '男'
  if (value === 2) return '女'
  return '未知'
}

function userTypeText(value) {
  if (value === 'ADMIN') return '管理员'
  if (value === 'MANAGER') return '主管'
  return '员工'
}

onMounted(() => {
  fillProfileForm(profile.value)
  loadProfile().catch(() => {})
})
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 16px;
}

.profile-summary {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background: #2563eb;
  color: #ffffff;
  font-size: 22px;
  font-weight: 700;
}

.profile-main h2 {
  margin: 0;
  font-size: 20px;
}

.profile-main p {
  margin: 6px 0 10px;
  color: #7b8794;
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  gap: 16px;
  align-items: stretch;
}

.profile-desc {
  margin-top: 4px;
}

.profile-info-panel,
.profile-edit-panel {
  min-height: 450px;
}

.profile-edit-panel {
  display: flex;
  flex-direction: column;
}

.profile-edit-panel .profile-form {
  flex: 1;
}

.password-panel {
  grid-column: 1 / -1;
}

.password-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr)) auto;
  gap: 0 12px;
  align-items: start;
}

.password-form :deep(.el-form-item:last-child) {
  align-self: end;
  margin-bottom: 18px;
}

@media (max-width: 980px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .password-form {
    grid-template-columns: 1fr;
  }

  .password-form :deep(.el-form-item:last-child) {
    margin-bottom: 0;
  }
}
</style>
