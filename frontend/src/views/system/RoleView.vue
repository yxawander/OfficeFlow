<template>
  <section class="panel">
    <div class="panel-header">
      <div>
        <h2>角色权限</h2>
        <p class="panel-subtitle">维护角色、菜单权限、接口权限和基础日志</p>
      </div>
      <el-button type="primary" :icon="Refresh" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="角色" name="roles">
        <div class="toolbar">
          <el-button type="primary" :icon="Plus" @click="openRoleDialog()">新增角色</el-button>
        </div>
        <el-table :data="roles" border v-loading="loading.roles">
          <el-table-column prop="roleName" label="角色名称" />
          <el-table-column prop="roleCode" label="角色编码" />
          <el-table-column prop="dataScope" label="数据范围" width="150" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRoleDialog(row)">编辑</el-button>
              <el-button link type="primary" @click="openMenuDialog(row)">分配菜单</el-button>
              <el-button link type="primary" @click="openApiDialog(row)">分配接口</el-button>
              <el-button link type="danger" @click="removeRole(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="菜单" name="menus">
        <el-table :data="menus" row-key="id" border default-expand-all v-loading="loading.menus">
          <el-table-column prop="menuName" label="菜单名称" min-width="180" />
          <el-table-column prop="menuType" label="类型" width="110" />
          <el-table-column prop="path" label="路径" min-width="160" />
          <el-table-column prop="permission" label="权限标识" min-width="180" />
          <el-table-column prop="icon" label="图标" width="120" />
          <el-table-column label="显示" width="80">
            <template #default="{ row }">{{ row.visible === 1 ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="接口权限" name="apis">
        <div class="toolbar">
          <el-button type="primary" :icon="Plus" @click="openPermissionDialog()">新增接口权限</el-button>
        </div>
        <el-table :data="apiPermissions" border v-loading="loading.apis">
          <el-table-column prop="permissionName" label="权限名称" min-width="160" />
          <el-table-column prop="permissionCode" label="权限编码" min-width="180" />
          <el-table-column prop="serviceName" label="服务" width="150" />
          <el-table-column prop="requestMethod" label="方法" width="90" />
          <el-table-column prop="requestPath" label="路径" min-width="220" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPermissionDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removePermission(row)">停用</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="日志" name="logs">
        <div class="log-grid">
          <section>
            <h3>登录日志</h3>
            <el-table :data="loginLogs" border height="360">
              <el-table-column prop="username" label="账号" width="120" />
              <el-table-column prop="loginStatus" label="状态" width="100" />
              <el-table-column prop="loginIp" label="IP" width="140" />
              <el-table-column prop="message" label="说明" />
              <el-table-column prop="createdAt" label="时间" width="180" />
            </el-table>
          </section>
          <section>
            <h3>操作日志</h3>
            <el-table :data="operationLogs" border height="360">
              <el-table-column prop="username" label="账号" width="120" />
              <el-table-column prop="moduleName" label="模块" width="120" />
              <el-table-column prop="operationType" label="操作" width="130" />
              <el-table-column prop="requestPath" label="路径" />
              <el-table-column prop="createdAt" label="时间" width="180" />
            </el-table>
          </section>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="roleDialog.visible" :title="roleDialog.form.id ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="roleDialog.form" label-width="92px">
        <el-form-item label="角色名称"><el-input v-model="roleDialog.form.roleName" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="roleDialog.form.roleCode" /></el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="roleDialog.form.dataScope">
            <el-option label="全部数据" value="ALL" />
            <el-option label="本部门及下级" value="DEPT_AND_CHILD" />
            <el-option label="本部门" value="DEPT" />
            <el-option label="本人" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="roleDialog.form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="roleDialog.form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialog.visible" title="分配菜单权限" width="520px">
      <el-tree ref="menuTreeRef" :data="menus" node-key="id" show-checkbox default-expand-all :props="{ label: 'menuName' }" />
      <template #footer>
        <el-button @click="menuDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveRoleMenus">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="apiDialog.visible" title="分配接口权限" width="620px">
      <el-checkbox-group v-model="apiDialog.ids" class="permission-checks">
        <el-checkbox v-for="item in apiPermissions" :key="item.id" :label="item.id">
          {{ item.permissionName }} / {{ item.requestMethod }} {{ item.requestPath }}
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="apiDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveRoleApis">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionDialog.visible" :title="permissionDialog.form.id ? '编辑接口权限' : '新增接口权限'" width="620px">
      <el-form :model="permissionDialog.form" label-width="104px">
        <el-form-item label="权限名称"><el-input v-model="permissionDialog.form.permissionName" /></el-form-item>
        <el-form-item label="权限编码"><el-input v-model="permissionDialog.form.permissionCode" /></el-form-item>
        <el-form-item label="服务名称"><el-input v-model="permissionDialog.form.serviceName" /></el-form-item>
        <el-form-item label="请求方法">
          <el-select v-model="permissionDialog.form.requestMethod">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="请求路径"><el-input v-model="permissionDialog.form.requestPath" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="permissionDialog.form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="savePermission">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  assignRoleApiPermissionsApi,
  assignRoleMenusApi,
  createApiPermissionApi,
  createRoleApi,
  deleteApiPermissionApi,
  deleteRoleApi,
  getApiPermissionListApi,
  getLoginLogsApi,
  getMenuListApi,
  getOperationLogsApi,
  getRoleListApi,
  updateApiPermissionApi,
  updateRoleApi
} from '@/api/user'

const activeTab = ref('roles')
const roles = ref([])
const menus = ref([])
const apiPermissions = ref([])
const loginLogs = ref([])
const operationLogs = ref([])
const menuTreeRef = ref()
const loading = reactive({ roles: false, menus: false, apis: false })

const roleDialog = reactive({ visible: false, form: emptyRole() })
const menuDialog = reactive({ visible: false, roleId: null })
const apiDialog = reactive({ visible: false, roleId: null, ids: [] })
const permissionDialog = reactive({ visible: false, form: emptyPermission() })

function emptyRole() {
  return { id: null, roleName: '', roleCode: '', dataScope: 'SELF', sortOrder: 0, status: 1 }
}

function emptyPermission() {
  return { id: null, permissionName: '', permissionCode: '', serviceName: 'user-service', requestMethod: 'GET', requestPath: '', status: 1 }
}

async function loadRoles() {
  loading.roles = true
  try {
    roles.value = (await getRoleListApi()).data
  } finally {
    loading.roles = false
  }
}

async function loadMenus() {
  loading.menus = true
  try {
    menus.value = (await getMenuListApi()).data
  } finally {
    loading.menus = false
  }
}

async function loadApis() {
  loading.apis = true
  try {
    apiPermissions.value = (await getApiPermissionListApi()).data
  } finally {
    loading.apis = false
  }
}

async function loadLogs() {
  loginLogs.value = (await getLoginLogsApi()).data
  operationLogs.value = (await getOperationLogsApi()).data
}

async function loadAll() {
  await Promise.all([loadRoles(), loadMenus(), loadApis(), loadLogs()])
}

function openRoleDialog(row) {
  roleDialog.form = row ? { ...emptyRole(), ...row } : emptyRole()
  roleDialog.visible = true
}

async function saveRole() {
  if (roleDialog.form.id) {
    await updateRoleApi(roleDialog.form.id, roleDialog.form)
  } else {
    await createRoleApi(roleDialog.form)
  }
  ElMessage.success('保存成功')
  roleDialog.visible = false
  await loadRoles()
}

async function removeRole(row) {
  await ElMessageBox.confirm(`确认删除角色 ${row.roleName}？`, '删除角色')
  await deleteRoleApi(row.id)
  ElMessage.success('删除成功')
  await loadRoles()
}

async function openMenuDialog(row) {
  menuDialog.roleId = row.id
  menuDialog.visible = true
  await nextTick()
  menuTreeRef.value?.setCheckedKeys(row.menuIds || [])
}

async function saveRoleMenus() {
  const checked = menuTreeRef.value.getCheckedKeys(false)
  await assignRoleMenusApi(menuDialog.roleId, checked)
  ElMessage.success('分配成功')
  menuDialog.visible = false
  await loadRoles()
}

function openApiDialog(row) {
  apiDialog.roleId = row.id
  apiDialog.ids = [...(row.apiPermissionIds || [])]
  apiDialog.visible = true
}

async function saveRoleApis() {
  await assignRoleApiPermissionsApi(apiDialog.roleId, apiDialog.ids)
  ElMessage.success('分配成功')
  apiDialog.visible = false
  await loadRoles()
}

function openPermissionDialog(row) {
  permissionDialog.form = row ? { ...emptyPermission(), ...row } : emptyPermission()
  permissionDialog.visible = true
}

async function savePermission() {
  if (permissionDialog.form.id) {
    await updateApiPermissionApi(permissionDialog.form.id, permissionDialog.form)
  } else {
    await createApiPermissionApi(permissionDialog.form)
  }
  ElMessage.success('保存成功')
  permissionDialog.visible = false
  await loadApis()
}

async function removePermission(row) {
  await ElMessageBox.confirm(`确认停用接口权限 ${row.permissionName}？`, '停用接口权限')
  await deleteApiPermissionApi(row.id)
  ElMessage.success('已停用')
  await loadApis()
}

onMounted(loadAll)
</script>
