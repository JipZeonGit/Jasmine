<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { getAllMenus } from '@/api/menu'
import { roleApi } from '@/api/role'
import type { MenuItem, RoleVO } from '@/types'

const list = ref<RoleVO[]>([])
const menus = ref<MenuItem[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const menuRef = ref()
const searchModel = reactive({ roleName: '', pageNo: 1, pageSize: 10 })
const form = reactive({ roleId: null as number | null, roleName: '', roleDesc: '', menuIdList: [] as number[] })

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleDesc: [{ required: true, message: '请输入角色描述', trigger: 'blur' }],
}

async function loadMenus() {
  const response = await getAllMenus()
  menus.value = response.data
}

async function loadList() {
  const response = await roleApi.list(searchModel)
  list.value = response.data.rows
  total.value = response.data.total
}

async function openDialog(id?: number) {
  Object.assign(form, { roleId: null, roleName: '', roleDesc: '', menuIdList: [] })
  dialogTitle.value = id == null ? '新增角色' : '修改角色'
  dialogVisible.value = true
  if (id == null) {
    queueMicrotask(() => menuRef.value?.setCheckedKeys([]))
    return
  }
  const response = await roleApi.getById(id)
  Object.assign(form, response.data)
  queueMicrotask(() => menuRef.value?.setCheckedKeys(response.data.menuIdList || []))
}

async function saveRole() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const checkedKeys = menuRef.value?.getCheckedKeys?.() || []
  const halfCheckedKeys = menuRef.value?.getHalfCheckedKeys?.() || []
  const response = await roleApi.save({ ...form, menuIdList: [...checkedKeys, ...halfCheckedKeys] })
  ElMessage.success(response.message)
  dialogVisible.value = false
  await loadList()
}

async function deleteRole(row: RoleVO) {
  await ElMessageBox.confirm(`确认删除角色 ${row.roleName} 吗？`, '提示', { type: 'warning' })
  const response = await roleApi.remove(row.roleId)
  ElMessage.success(response.message)
  await loadList()
}

loadList()
loadMenus()
</script>

<template>
  <el-card class="page-card">
    <div class="page-toolbar">
      <div class="page-search"><el-input v-model="searchModel.roleName" placeholder="角色名" clearable style="width: 220px" /><el-button type="primary" @click="loadList">查询</el-button></div>
      <el-button type="primary" @click="openDialog()">新增角色</el-button>
    </div>
  </el-card>
  <el-card class="page-card">
    <el-table :data="list" stripe>
      <el-table-column type="index" label="#" width="70" />
      <el-table-column prop="roleId" label="角色编号" width="120" />
      <el-table-column prop="roleName" label="角色名称" width="160" />
      <el-table-column prop="roleDesc" label="角色描述" min-width="220" />
      <el-table-column label="操作" width="160"><template #default="{ row }"><el-button type="primary" size="small" @click="openDialog(row.roleId)">编辑</el-button><el-button type="danger" size="small" @click="deleteRole(row)">删除</el-button></template></el-table-column>
    </el-table>
    <div style="margin-top:16px;display:flex;justify-content:flex-end;"><el-pagination v-model:current-page="searchModel.pageNo" v-model:page-size="searchModel.pageSize" :page-sizes="[5,10,20,50]" layout="total, sizes, prev, pager, next, jumper" :total="total" @change="loadList" /></div>
  </el-card>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName" /></el-form-item>
      <el-form-item label="角色描述" prop="roleDesc"><el-input v-model="form.roleDesc" /></el-form-item>
      <el-form-item label="权限设置">
        <el-tree ref="menuRef" :data="menus" node-key="menuId" default-expand-all show-checkbox :props="{ children: 'children', label: 'title' }" style="width: 100%;" />
      </el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="saveRole">确定</el-button></template>
  </el-dialog>
</template>
