<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { flowerApi } from '@/api/flower'
import type { FlowerVO } from '@/types'

const total = ref(0)
const flowerList = ref<FlowerVO[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const searchModel = reactive({ name: '', pageNo: 1, pageSize: 10 })
const form = reactive(getDefaultForm())

const rules = {
  name: [{ required: true, message: '请输入花卉名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择单位', trigger: 'change' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'change' }],
  costPrice: [{ required: true, message: '请输入成本价', trigger: 'change' }],
  safeStock: [{ required: true, message: '请输入安全库存', trigger: 'change' }],
}

function getDefaultForm() {
  return { id: null as number | null, name: '', unit: '枝', salePrice: 0, costPrice: 0, safeStock: 0, status: 1 }
}

async function loadFlowerList() {
  const response = await flowerApi.list(searchModel)
  flowerList.value = response.data.rows
  total.value = response.data.total
}

async function openDialog(id?: number) {
  Object.assign(form, getDefaultForm())
  if (id == null) {
    dialogTitle.value = '新增花卉'
    dialogVisible.value = true
    return
  }
  dialogTitle.value = '修改花卉'
  const response = await flowerApi.getById(id)
  Object.assign(form, response.data || getDefaultForm())
  dialogVisible.value = true
}

async function saveFlower() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const response = await flowerApi.save({ ...form })
  ElMessage.success(response.message)
  dialogVisible.value = false
  await loadFlowerList()
}

async function deleteFlower(row: FlowerVO) {
  await ElMessageBox.confirm(`确认删除花卉 ${row.name} 吗？`, '提示', { type: 'warning' })
  const response = await flowerApi.remove(row.id)
  ElMessage.success(response.message)
  await loadFlowerList()
}

loadFlowerList()
</script>

<template>
  <el-card class="page-card">
    <div class="page-toolbar">
      <div class="page-search">
        <el-input v-model="searchModel.name" placeholder="花卉名称" clearable style="width: 220px" />
        <el-button type="primary" @click="loadFlowerList">查询</el-button>
      </div>
      <el-button type="primary" @click="openDialog()">新增花卉</el-button>
    </div>
  </el-card>

  <el-card class="page-card">
    <el-table :data="flowerList" style="width: 100%">
      <el-table-column type="index" label="#" width="70" />
      <el-table-column prop="id" label="花卉ID" width="100" />
      <el-table-column prop="name" label="花名" width="160" />
      <el-table-column prop="unit" label="单位" width="90" />
      <el-table-column prop="salePrice" label="售价(元)" width="120" />
      <el-table-column prop="costPrice" label="成本价(元)" width="120" />
      <el-table-column prop="safeStock" label="安全库存" width="110" />
      <el-table-column label="当前库存" width="110">
        <template #default="{ row }">
          <span :style="{ color: row.currentStock < row.safeStock ? '#f56c6c' : 'inherit', fontWeight: row.currentStock < row.safeStock ? 'bold' : 'normal' }">
            {{ row.currentStock }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '在售' : '停售' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column min-width="100" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openDialog(row.id)">编辑</el-button>
          <el-button type="danger" size="small" @click="deleteFlower(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-container">
      <el-pagination v-model:current-page="searchModel.pageNo" v-model:page-size="searchModel.pageSize" :page-sizes="[10,25,50,100]" layout="total, sizes, prev, pager, next, jumper" :total="total" @change="loadFlowerList" />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="dialog-form-grid">
      <el-form-item label="花卉名称" prop="name"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="单位" prop="unit">
        <el-select v-model="form.unit"><el-option label="枝" value="枝" /><el-option label="束" value="束" /><el-option label="扎" value="扎" /><el-option label="盆" value="盆" /></el-select>
      </el-form-item>
      <el-form-item label="售价(元)" prop="salePrice"><el-input-number v-model="form.salePrice" :min="0" :precision="2" :controls="false" /></el-form-item>
      <el-form-item label="成本价(元)" prop="costPrice"><el-input-number v-model="form.costPrice" :min="0" :precision="2" :controls="false" /></el-form-item>
      <el-form-item label="安全库存" prop="safeStock"><el-input-number v-model="form.safeStock" :min="0" :controls="false" /></el-form-item>
      <el-form-item label="销售状态" prop="status"><el-radio-group v-model="form.status"><el-radio :value="1">在售</el-radio><el-radio :value="0">停售</el-radio></el-radio-group></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveFlower">确定</el-button>
    </template>
  </el-dialog>
</template>
