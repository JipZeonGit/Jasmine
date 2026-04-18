<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { vipApi } from '@/api/vip'
import type { VipVO } from '@/types'

const vipList = ref<VipVO[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const searchModel = reactive({ vid: '', name: '', phone: '', pageNo: 1, pageSize: 10 })
const form = reactive(getDefaultForm())

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
}

function getDefaultForm() {
  return { id: null as number | null, vid: '', name: '', sex: '', phone: '' }
}

async function loadVipList() {
  const response = await vipApi.list(searchModel)
  vipList.value = response.data.rows
  total.value = response.data.total
}

async function openDialog(id?: number) {
  Object.assign(form, getDefaultForm())
  if (id == null) {
    dialogTitle.value = '新增会员'
    dialogVisible.value = true
    return
  }
  dialogTitle.value = '修改会员'
  const response = await vipApi.getById(id)
  Object.assign(form, response.data)
  dialogVisible.value = true
}

async function saveVip() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const response = await vipApi.save({ ...form })
  ElMessage.success(response.message)
  dialogVisible.value = false
  await loadVipList()
}

async function deleteVip(row: VipVO) {
  await ElMessageBox.confirm(`确认删除会员 ${row.name} 吗？`, '提示', { type: 'warning' })
  const response = await vipApi.remove(row.id)
  ElMessage.success(response.message)
  await loadVipList()
}

loadVipList()
</script>

<template>
  <el-card class="page-card">
    <div class="page-toolbar">
      <div class="page-search">
        <el-input v-model="searchModel.vid" placeholder="会员卡号" clearable style="width: 200px" />
        <el-input v-model="searchModel.name" placeholder="姓名" clearable style="width: 200px" />
        <el-input v-model="searchModel.phone" placeholder="手机号" clearable style="width: 200px" />
        <el-button type="primary" @click="loadVipList">查询</el-button>
      </div>
      <el-button type="primary" @click="openDialog()">新增会员</el-button>
    </div>
  </el-card>
  <el-card class="page-card">
    <el-table :data="vipList" stripe>
      <el-table-column type="index" label="#" width="70" />
      <el-table-column prop="id" label="会员ID" width="100" />
      <el-table-column prop="vid" label="会员卡号" width="180" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="sex" label="性别" width="80" />
      <el-table-column prop="phone" label="手机号" width="160" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openDialog(row.id)">编辑</el-button>
          <el-button type="danger" size="small" @click="deleteVip(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
      <el-pagination v-model:current-page="searchModel.pageNo" v-model:page-size="searchModel.pageSize" :page-sizes="[10,25,50,100]" layout="total, sizes, prev, pager, next, jumper" :total="total" @change="loadVipList" />
    </div>
  </el-card>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="性别" prop="sex"><el-radio-group v-model="form.sex"><el-radio value="男">男</el-radio><el-radio value="女">女</el-radio></el-radio-group></el-form-item>
      <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="saveVip">确定</el-button></template>
  </el-dialog>
</template>
