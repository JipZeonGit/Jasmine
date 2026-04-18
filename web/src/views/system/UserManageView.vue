<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { roleApi } from '@/api/role'
import { userManageApi } from '@/api/userManage'
import type { RoleVO, UserVO } from '@/types'

const list = ref<UserVO[]>([])
const roles = ref<RoleVO[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const searchModel = reactive({ username: '', phone: '', pageNo: 1, pageSize: 10 })
const form = reactive(getDefaultForm())

function getDefaultForm() {
  return { id: null as number | null, username: '', password: '', phone: '', status: 1, email: '', roleIdList: [] as number[] }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  email: [{ required: true, message: '请输入电子邮箱', trigger: 'blur' }],
}

async function loadRoles() {
  const response = await roleApi.getAll()
  roles.value = response.data
}

async function loadList() {
  const response = await userManageApi.list(searchModel)
  list.value = response.data.rows
  total.value = response.data.total
}

async function openDialog(id?: number) {
  Object.assign(form, getDefaultForm())
  dialogTitle.value = id == null ? '新增用户' : '修改用户'
  dialogVisible.value = true
  if (id == null) return
  const response = await userManageApi.getById(id)
  Object.assign(form, response.data || getDefaultForm())
}

async function saveUser() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload = { ...form }
  if (payload.id != null) delete payload.password
  const response = await userManageApi.save(payload)
  ElMessage.success(response.message)
  dialogVisible.value = false
  await loadList()
}

async function deleteUser(row: UserVO) {
  await ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, '提示', { type: 'warning' })
  const response = await userManageApi.remove(row.id)
  ElMessage.success(response.message)
  await loadList()
}

loadRoles()
loadList()
</script>

<template>
  <el-card class="page-card">
    <div class="page-toolbar">
      <div class="page-search"><el-input v-model="searchModel.username" placeholder="用户名" clearable style="width: 200px" /><el-input v-model="searchModel.phone" placeholder="手机号" clearable style="width: 200px" /><el-button type="primary" @click="loadList">查询</el-button></div>
      <el-button type="primary" @click="openDialog()">新增用户</el-button>
    </div>
  </el-card>
  <el-card class="page-card">
    <el-table :data="list" stripe>
      <el-table-column type="index" label="#" width="70" />
      <el-table-column prop="id" label="用户ID" width="100" />
      <el-table-column prop="username" label="用户名" width="160" />
      <el-table-column prop="phone" label="手机号" width="160" />
      <el-table-column label="用户状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag></template></el-table-column>
      <el-table-column prop="email" label="电子邮件" min-width="220" />
      <el-table-column label="操作" width="160"><template #default="{ row }"><el-button type="primary" size="small" @click="openDialog(row.id)">编辑</el-button><el-button type="danger" size="small" @click="deleteUser(row)">删除</el-button></template></el-table-column>
    </el-table>
    <div style="margin-top:16px;display:flex;justify-content:flex-end;"><el-pagination v-model:current-page="searchModel.pageNo" v-model:page-size="searchModel.pageSize" :page-sizes="[10,25,50,100]" layout="total, sizes, prev, pager, next, jumper" :total="total" @change="loadList" /></div>
  </el-card>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="用户名" prop="username"><el-input v-model="form.username" /></el-form-item>
      <el-form-item v-if="form.id == null" label="密码" prop="password"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="用户状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      <el-form-item label="用户角色"><el-checkbox-group v-model="form.roleIdList"><el-checkbox v-for="role in roles" :key="role.roleId" :value="role.roleId">{{ role.roleDesc }}</el-checkbox></el-checkbox-group></el-form-item>
      <el-form-item label="电子邮件" prop="email"><el-input v-model="form.email" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="saveUser">确定</el-button></template>
  </el-dialog>
</template>
