<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { flowerApi } from '@/api/flower'
import { inventoryApi } from '@/api/inventory'
import type { FlowerVO, InventoryVO } from '@/types'

const list = ref<InventoryVO[]>([])
const flowerOptions = ref<FlowerVO[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const bizTypeOptions = [
  { label: '采购入库', value: 'PURCHASE_IN' },
  { label: '销售出库', value: 'SALE_OUT' },
  { label: '损耗出库', value: 'LOSS_OUT' },
  { label: '退货入库', value: 'RETURN_IN' },
  { label: '盘点调增', value: 'CHECK_IN' },
  { label: '盘点调减', value: 'CHECK_OUT' },
]
const searchModel = reactive({ name: '', num: '', bizType: '', dateRange: [] as string[], pageNo: 1, pageSize: 10 })
const form = reactive(getDefaultForm())

const requiresUnitCost = computed(() => form.bizType === 'PURCHASE_IN')

function getDefaultForm() {
  return { id: null as number | null, flowerId: null as number | null, bizType: '', quantity: 1, unitCost: null as number | null, date: '', remark: '' }
}

const rules = {
  flowerId: [{ required: true, message: '请选择花卉', trigger: 'change' }],
  bizType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入变动数量', trigger: 'change' }],
  date: [{ required: true, message: '请选择业务时间', trigger: 'change' }],
}

async function loadFlowerOptions() {
  const response = await flowerApi.getAll()
  flowerOptions.value = response.data.filter((item) => item.status === 1)
}

async function loadList() {
  const [startTime, endTime] = searchModel.dateRange || []
  const response = await inventoryApi.list({ ...searchModel, startTime, endTime })
  list.value = response.data.rows
  total.value = response.data.total
}

async function openDialog(id?: number) {
  Object.assign(form, getDefaultForm())
  if (id == null) {
    dialogTitle.value = '登记库存动作'
    dialogVisible.value = true
    return
  }
  dialogTitle.value = '修改库存动作'
  const response = await inventoryApi.getById(id)
  Object.assign(form, response.data)
  dialogVisible.value = true
}

async function saveInventory() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const payload = { ...form, quantity: Number(form.quantity), unitCost: requiresUnitCost.value ? Number(form.unitCost) : null }
  const response = await inventoryApi.save(payload)
  ElMessage.success(response.message)
  dialogVisible.value = false
  await loadList()
  await loadFlowerOptions()
}

async function deleteInventory(row: InventoryVO) {
  await ElMessageBox.confirm(`确认删除库存流水 ${row.bizNo} 吗？`, '提示', { type: 'warning' })
  const response = await inventoryApi.remove(row.id)
  ElMessage.success(response.message)
  await loadList()
  await loadFlowerOptions()
}

loadList()
loadFlowerOptions()
</script>

<template>
  <el-card class="page-card">
    <div class="page-toolbar">
      <div class="page-search">
        <el-input v-model="searchModel.name" placeholder="花卉名称" clearable style="width: 130px" />
        <el-input v-model="searchModel.num" placeholder="业务单号" clearable style="width: 160px" />
        <el-select v-model="searchModel.bizType" clearable placeholder="业务类型" style="width: 120px">
          <el-option v-for="item in bizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker v-model="searchModel.dateRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD" range-separator="至" start-placeholder="开始" end-placeholder="结束" clearable style="width: 260px" />
        <el-button type="primary" @click="loadList">查询</el-button>
      </div>
      <el-button type="primary" @click="openDialog()">新增库存动作</el-button>
    </div>
  </el-card>
  <el-card class="page-card">
    <el-table :data="list" style="width: 100%">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="bizNo" label="业务单号" width="220" show-overflow-tooltip />
      <el-table-column prop="flowerName" label="花名" width="80" show-overflow-tooltip />
      <el-table-column prop="bizTypeLabel" label="业务类型" width="90" />
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="beforeStock" label="变动前" width="90" />
      <el-table-column prop="afterStock" label="变动后" width="90" />
      <el-table-column prop="unitCost" label="单价" width="80" />
      <el-table-column prop="totalCost" label="小计" width="80" />
      <el-table-column prop="date" label="时间" width="160" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openDialog(row.id)">编辑</el-button>
          <el-button type="danger" size="small" @click="deleteInventory(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-container">
      <el-pagination v-model:current-page="searchModel.pageNo" v-model:page-size="searchModel.pageSize" :page-sizes="[10,25,50,100]" layout="total, sizes, prev, pager, next, jumper" :total="total" @change="loadList" />
    </div>
  </el-card>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="dialog-form-grid">
      <el-form-item label="花卉" prop="flowerId"><el-select v-model="form.flowerId" filterable><el-option v-for="flower in flowerOptions" :key="flower.id" :label="`${flower.name} （当前库存： ${flower.currentStock} ${flower.unit}）`" :value="flower.id" /></el-select></el-form-item>
      <el-form-item label="业务类型" prop="bizType"><el-select v-model="form.bizType"><el-option v-for="item in bizTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="变动数量" prop="quantity"><el-input-number v-model="form.quantity" :min="1" :controls="false" /></el-form-item>
      <el-form-item v-if="requiresUnitCost" label="进货单价"><el-input-number v-model="form.unitCost" :min="0" :precision="2" :controls="false" /></el-form-item>
      <el-form-item label="业务时间" prop="date"><el-date-picker v-model="form.date" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
      <el-form-item label="备注" class="full-width"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="200" show-word-limit /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="saveInventory">确定</el-button></template>
  </el-dialog>
</template>
