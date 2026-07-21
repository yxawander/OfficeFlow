<template>
  <section class="panel">
    <div class="panel-header">
      <h2>角色权限</h2>
      <el-button type="primary" :icon="Plus">新增角色</el-button>
    </div>
    <el-table :data="roles" border>
      <el-table-column prop="name" label="角色名称" />
      <el-table-column prop="code" label="角色编码" />
      <el-table-column prop="menus" label="菜单权限" />
      <el-table-column label="操作" width="160">
        <el-button link type="primary">分配菜单</el-button>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

const roles = [
  { name: '系统管理员', code: 'ADMIN', menus: '全部菜单' },
  { name: '部门主管', code: 'MANAGER', menus: '审批、考勤、公告' },
  { name: '普通员工', code: 'EMPLOYEE', menus: '打卡、申请、公告' }
]
</script>

