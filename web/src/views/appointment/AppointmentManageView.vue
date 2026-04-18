<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { appointmentApi } from '@/api/appointment'
import { vipApi } from '@/api/vip'
import type { AppointmentVO, VipVO } from '@/types'

const list = ref<AppointmentVO[]>([])
const vipOptions = ref<VipVO[]>([])
const total = ref(0)
const addVisible = ref(false)
const editVisible = ref(false)
const addFormRef = ref()
const editFormRef = ref()
const searchModel = reactive({ name: '', phone: '', dateRange: [] as string[], pageNo: 1, pageSize: 10 })
const addForm = reactive({ vipId: null as number | null, vid: '', phone: '', content: '', date: '' })
const editForm = reactive({ id: null as number | null, content: '', date: '' })

const addRules = {
  content: [{ required: true, message: '请输入预约内容', trigger: 'blur' }],
  date: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
}

const editRules = {
  content: [{ required: true, message: '请输入预约内容', trigger: 'blur' }],
  date: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
}

async function loadVipOptions() {
  const response = await vipApi.getAll()
  vipOptions.value = response.data
}

async function loadList() {
  const [startTime, endTime] = searchModel.dateRange || []
  const response = await appointmentApi.list({ ...searchModel, startTime, endTime })
  list.value = response.data.rows
  total.value = response.data.total
}

async function createAppointment() {
  const valid = await addFormRef.value.validate().catch(() => false)
  if (!valid) return
  const response = await appointmentApi.create({ ...addForm })
  ElMessage.success(response.message)
  addVisible.value = false
  Object.assign(addForm, { vipId: null, vid: '', phone: '', content: '', date: '' })
  await loadList()
}

async function openEdit(id?: number) {
  if (id == null) {
    addVisible.value = true
    return
  }
  const response = await appointmentApi.getById(id)
  Object.assign(editForm, { id: response.data.id, content: response.data.content, date: response.data.date })
  editVisible.value = true
}

async function updateAppointment() {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  const response = await appointmentApi.update({ ...editForm })
  ElMessage.success(response.message)
  editVisible.value = false
  await loadList()
}

async function deleteAppointment(row: AppointmentVO) {
  await ElMessageBox.confirm(`确认删除预约 ${row.name} 吗？`, '提示', { type: 'warning' })
  const response = await appointmentApi.remove(row.id)
  ElMessage.success(response.message)
  await loadList()
}

loadList()
loadVipOptions()
</script>

<template>
  <el-card class="page-card">
    <div class="page-toolbar">
      <div class="page-search">
        <el-input v-model="searchModel.name" placeholder="会员姓名" clearable style="width: 200px" />
        <el-input v-model="searchModel.phone" placeholder="手机号" clearable style="width: 200px" />
        <el-date-picker v-model="searchModel.dateRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" clearable />
        <el-button type="primary" @click="loadList">查询</el-button>
      </div>
      <el-button type="primary" @click="openEdit()">新增预约</el-button>
    </div>
  </el-card>
  <el-card class="page-card">
    <el-table :data="list" stripe>
      <el-table-column type="index" label="#" width="70" />
      <el-table-column prop="id" label="预约ID" width="100" />
      <el-table-column prop="vid" label="会员卡号" width="150" />
      <el-table-column prop="name" label="会员姓名" width="120" />
      <el-table-column prop="sex" label="性别" width="80" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="date" label="预约时间" width="180" />
      <el-table-column prop="content" label="预约内容" min-width="220" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openEdit(row.id)">编辑</el-button>
          <el-button type="danger" size="small" @click="deleteAppointment(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
      <el-pagination v-model:current-page="searchModel.pageNo" v-model:page-size="searchModel.pageSize" :page-sizes="[10,25,50,100]" layout="total, sizes, prev, pager, next, jumper" :total="total" @change="loadList" />
    </div>
  </el-card>

  <el-dialog v-model="addVisible" title="新增预约" width="640px">
    <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="110px">
      <el-form-item label="直接选会员"><el-select v-model="addForm.vipId" clearable filterable><el-option v-for="vip in vipOptions" :key="vip.id" :label="`${vip.name}(${vip.phone})`" :value="vip.id" /></el-select></el-form-item>
      <el-form-item label="手机号"><el-input v-model="addForm.phone" /></el-form-item>
      <el-form-item label="会员卡号"><el-input v-model="addForm.vid" /></el-form-item>
      <el-form-item label="预约内容" prop="content"><el-input v-model="addForm.content" /></el-form-item>
      <el-form-item label="预约时间" prop="date"><el-date-picker v-model="addForm.date" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="addVisible = false">取消</el-button><el-button type="primary" @click="createAppointment">确定</el-button></template>
  </el-dialog>

  <el-dialog v-model="editVisible" title="修改预约" width="560px">
    <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="110px">
      <el-form-item label="预约内容" prop="content"><el-input v-model="editForm.content" /></el-form-item>
      <el-form-item label="预约时间" prop="date"><el-date-picker v-model="editForm.date" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="editVisible = false">取消</el-button><el-button type="primary" @click="updateAppointment">确定</el-button></template>
  </el-dialog>
</template>
