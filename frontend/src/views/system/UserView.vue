<template>
  <section class="panel">
    <div class="panel-header">
      <div>
        <h2>员工与组织管理</h2>
        <p class="panel-subtitle">维护员工账号、部门树、岗位和直属领导关系</p>
      </div>
      <el-button type="primary" :icon="Refresh" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="员工" name="users">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="账号 / 姓名 / 手机号" clearable style="width: 220px" @keyup.enter="loadUsers" />
          <el-tree-select v-model="query.deptId" :data="deptTree" node-key="id" check-strictly clearable placeholder="部门" style="width: 200px" />
          <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadUsers">查询</el-button>
          <el-button :icon="Plus" @click="openUserDialog()">新增员工</el-button>
        </div>

        <el-table :data="users" border v-loading="loading.users">
          <el-table-column prop="username" label="账号" width="130" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column prop="deptName" label="部门" min-width="120" />
          <el-table-column prop="postName" label="岗位" min-width="120" />
          <el-table-column prop="managerName" label="直属领导" width="120" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column prop="userType" label="类型" width="110" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openUserDialog(row)">编辑</el-button>
              <el-button link type="primary" @click="openRoleDialog(row)">分配角色</el-button>
              <el-button link type="warning" @click="toggleUserStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
              <el-button link type="warning" @click="resetPassword(row)">重置密码</el-button>
              <el-button link type="danger" :disabled="row.id === 1" @click="removeUser(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="userTotal"
            layout="total, sizes, prev, pager, next"
            @change="loadUsers"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="部门" name="depts">
        <div class="toolbar">
          <el-button type="primary" :icon="Plus" @click="openDeptDialog()">新增部门</el-button>
        </div>
        <el-table :data="deptTree" row-key="id" border default-expand-all v-loading="loading.depts">
          <el-table-column prop="deptName" label="部门名称" min-width="180" />
          <el-table-column prop="deptCode" label="部门编码" width="150" />
          <el-table-column prop="leaderName" label="负责人" width="140" />
          <el-table-column prop="phone" label="电话" width="140" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDeptDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeDept(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="岗位" name="posts">
        <div class="toolbar">
          <el-button type="primary" :icon="Plus" @click="openPostDialog()">新增岗位</el-button>
        </div>
        <el-table :data="posts" border v-loading="loading.posts">
          <el-table-column prop="postName" label="岗位名称" />
          <el-table-column prop="postCode" label="岗位编码" />
          <el-table-column prop="sortOrder" label="排序" width="90" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPostDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removePost(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="userDialog.visible" :title="userDialog.form.id ? '编辑员工' : '新增员工'" width="720px">
      <el-form :model="userDialog.form" label-width="92px">
        <div class="form-grid">
          <el-form-item label="账号"><el-input v-model="userDialog.form.username" :disabled="!!userDialog.form.id" /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="userDialog.form.realName" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="userDialog.form.password" type="password" show-password placeholder="新增默认 123456" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="userDialog.form.phone" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="userDialog.form.email" /></el-form-item>
          <el-form-item label="部门"><el-tree-select v-model="userDialog.form.deptId" :data="deptTree" node-key="id" check-strictly clearable /></el-form-item>
          <el-form-item label="岗位"><el-select v-model="userDialog.form.postId" clearable><el-option v-for="item in posts" :key="item.id" :label="item.postName" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="直属领导"><el-select v-model="userDialog.form.managerId" clearable><el-option v-for="item in userOptions" :key="item.id" :label="item.realName" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="用户类型"><el-select v-model="userDialog.form.userType"><el-option label="管理员" value="ADMIN" /><el-option label="主管" value="MANAGER" /><el-option label="员工" value="EMPLOYEE" /></el-select></el-form-item>
          <el-form-item label="状态"><el-switch v-model="userDialog.form.status" :active-value="1" :inactive-value="0" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="userDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialog.visible" title="分配角色" width="420px">
      <el-checkbox-group v-model="roleDialog.ids">
        <el-checkbox v-for="role in roles" :key="role.id" :label="role.id">{{ role.roleName }}</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveUserRoles">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deptDialog.visible" :title="deptDialog.form.id ? '编辑部门' : '新增部门'" width="560px">
      <el-form :model="deptDialog.form" label-width="92px">
        <el-form-item label="上级部门"><el-tree-select v-model="deptDialog.form.parentId" :data="deptTree" node-key="id" check-strictly clearable /></el-form-item>
        <el-form-item label="部门名称"><el-input v-model="deptDialog.form.deptName" /></el-form-item>
        <el-form-item label="部门编码"><el-input v-model="deptDialog.form.deptCode" /></el-form-item>
        <el-form-item label="负责人"><el-select v-model="deptDialog.form.leaderId" clearable><el-option v-for="item in userOptions" :key="item.id" :label="item.realName" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="电话"><el-input v-model="deptDialog.form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="deptDialog.form.email" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="deptDialog.form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="deptDialog.form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveDept">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="postDialog.visible" :title="postDialog.form.id ? '编辑岗位' : '新增岗位'" width="460px">
      <el-form :model="postDialog.form" label-width="92px">
        <el-form-item label="岗位名称"><el-input v-model="postDialog.form.postName" /></el-form-item>
        <el-form-item label="岗位编码"><el-input v-model="postDialog.form.postCode" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="postDialog.form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="postDialog.form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="postDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="savePost">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  assignUserRolesApi,
  createDeptApi,
  createPostApi,
  createUserApi,
  deleteDeptApi,
  deletePostApi,
  deleteUserApi,
  getDeptTreeApi,
  getPostListApi,
  getRoleListApi,
  getUserOptionsApi,
  getUserPageApi,
  resetUserPasswordApi,
  updateDeptApi,
  updatePostApi,
  updateUserApi,
  updateUserStatusApi
} from '@/api/user'

const activeTab = ref('users')
const users = ref([])
const deptTree = ref([])
const posts = ref([])
const roles = ref([])
const userOptions = ref([])
const userTotal = ref(0)
const loading = reactive({ users: false, depts: false, posts: false })
const query = reactive({ keyword: '', deptId: null, status: null, pageNum: 1, pageSize: 10 })

const userDialog = reactive({ visible: false, form: emptyUser() })
const deptDialog = reactive({ visible: false, form: emptyDept() })
const postDialog = reactive({ visible: false, form: emptyPost() })
const roleDialog = reactive({ visible: false, userId: null, ids: [] })

function emptyUser() {
  return { id: null, username: '', password: '', realName: '', gender: 0, phone: '', email: '', deptId: null, postId: null, managerId: null, userType: 'EMPLOYEE', status: 1 }
}

function emptyDept() {
  return { id: null, parentId: 0, deptName: '', deptCode: '', leaderId: null, phone: '', email: '', sortOrder: 0, status: 1 }
}

function emptyPost() {
  return { id: null, postName: '', postCode: '', sortOrder: 0, status: 1 }
}

async function loadUsers() {
  loading.users = true
  try {
    const res = await getUserPageApi(query)
    users.value = res.data.records
    userTotal.value = res.data.total
  } finally {
    loading.users = false
  }
}

async function loadDepts() {
  loading.depts = true
  try {
    deptTree.value = (await getDeptTreeApi()).data
  } finally {
    loading.depts = false
  }
}

async function loadPosts() {
  loading.posts = true
  try {
    posts.value = (await getPostListApi()).data
  } finally {
    loading.posts = false
  }
}

async function loadBaseOptions() {
  roles.value = (await getRoleListApi()).data
  userOptions.value = (await getUserOptionsApi()).data
}

async function loadAll() {
  await Promise.all([loadUsers(), loadDepts(), loadPosts(), loadBaseOptions()])
}

function openUserDialog(row) {
  userDialog.form = row ? { ...emptyUser(), ...row, password: '' } : emptyUser()
  userDialog.visible = true
}

async function saveUser() {
  if (userDialog.form.id) {
    await updateUserApi(userDialog.form.id, userDialog.form)
  } else {
    await createUserApi(userDialog.form)
  }
  ElMessage.success('保存成功')
  userDialog.visible = false
  await Promise.all([loadUsers(), loadBaseOptions()])
}

function openRoleDialog(row) {
  roleDialog.userId = row.id
  roleDialog.ids = [...(row.roleIds || [])]
  roleDialog.visible = true
}

async function saveUserRoles() {
  await assignUserRolesApi(roleDialog.userId, roleDialog.ids)
  ElMessage.success('分配成功')
  roleDialog.visible = false
  await loadUsers()
}

async function toggleUserStatus(row) {
  await updateUserStatusApi(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('状态已更新')
  await loadUsers()
}

async function resetPassword(row) {
  await ElMessageBox.confirm(`确认将 ${row.realName} 的密码重置为 123456？`, '重置密码')
  await resetUserPasswordApi(row.id, '123456')
  ElMessage.success('密码已重置')
}

async function removeUser(row) {
  await ElMessageBox.confirm(`确认删除员工 ${row.realName}？`, '删除员工')
  await deleteUserApi(row.id)
  ElMessage.success('删除成功')
  await loadUsers()
}

function openDeptDialog(row) {
  deptDialog.form = row ? { ...emptyDept(), ...row } : emptyDept()
  deptDialog.visible = true
}

async function saveDept() {
  if (deptDialog.form.id) {
    await updateDeptApi(deptDialog.form.id, deptDialog.form)
  } else {
    await createDeptApi(deptDialog.form)
  }
  ElMessage.success('保存成功')
  deptDialog.visible = false
  await Promise.all([loadDepts(), loadUsers()])
}

async function removeDept(row) {
  await ElMessageBox.confirm(`确认删除部门 ${row.deptName}？`, '删除部门')
  await deleteDeptApi(row.id)
  ElMessage.success('删除成功')
  await loadDepts()
}

function openPostDialog(row) {
  postDialog.form = row ? { ...emptyPost(), ...row } : emptyPost()
  postDialog.visible = true
}

async function savePost() {
  if (postDialog.form.id) {
    await updatePostApi(postDialog.form.id, postDialog.form)
  } else {
    await createPostApi(postDialog.form)
  }
  ElMessage.success('保存成功')
  postDialog.visible = false
  await Promise.all([loadPosts(), loadUsers()])
}

async function removePost(row) {
  await ElMessageBox.confirm(`确认删除岗位 ${row.postName}？`, '删除岗位')
  await deletePostApi(row.id)
  ElMessage.success('删除成功')
  await loadPosts()
}

onMounted(loadAll)
</script>
