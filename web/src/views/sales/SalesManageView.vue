<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { flowerApi } from '@/api/flower'
import { salesApi } from '@/api/sales'
import { vipApi } from '@/api/vip'
import type { FlowerVO, SalesVO, TodayBusinessSummaryVO, VipVO } from '@/types'

interface SalesFormItem { _key: string; flowerId: number | null; quantity: number; unitPrice: number }

const total = ref(0)
const salesList = ref<SalesVO[]>([])
const flowerOptions = ref<FlowerVO[]>([])
const vipOptions = ref<VipVO[]>([])
const todaySummary = ref<TodayBusinessSummaryVO>({ todaySalesAmount: 0, todayPurchaseCost: 0, todayGrossProfit: 0, todayNetCashflow: 0, todaySalesOrderCount: 0, todayPurchaseCount: 0 })
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const searchModel = reactive({ orderNo: '', dateRange: [] as string[], pageNo: 1, pageSize: 10 })
const form = reactive(getDefaultForm())

function getDefaultItem(): SalesFormItem {
  return { _key: `${Date.now()}-${Math.random()}`, flowerId: null, quantity: 1, unitPrice: 0 }
}

function getDefaultForm() {
  return { id: null as number | null, vipId: null as number | null, date: '', remark: '', items: [getDefaultItem()] as SalesFormItem[] }
}

const salesTotal = computed(() => form.items.reduce((totalValue, item) => totalValue + Number(item.quantity || 0) * Number(item.unitPrice || 0), 0).toFixed(2))

async function loadOptions() {
  const [flowers, vips] = await Promise.all([flowerApi.getAll(), vipApi.getAll()])
  flowerOptions.value = flowers.data.filter((item) => item.status === 1)
  vipOptions.value = vips.data
}

async function loadTodaySummary() {
  const response = await salesApi.getTodaySummary()
  todaySummary.value = response.data
}

async function loadSalesList() {
  const [startTime, endTime] = searchModel.dateRange || []
  const response = await salesApi.list({ ...searchModel, startTime, endTime })
  salesList.value = response.data.rows
  total.value = response.data.total
}

function handleFlowerChange(item: SalesFormItem) {
  const flower = flowerOptions.value.find((option) => option.id === item.flowerId)
  if (flower) item.unitPrice = Number(flower.salePrice)
}

function calcUnitCost(item: SalesFormItem) {
  const flower = flowerOptions.value.find((option) => option.id === item.flowerId)
  return flower ? Number(flower.costPrice).toFixed(2) : '0.00'
}

function calcGrossProfit(item: SalesFormItem) {
  const flower = flowerOptions.value.find((option) => option.id === item.flowerId)
  const unitCost = flower ? Number(flower.costPrice) : 0
  return (Number(item.quantity || 0) * (Number(item.unitPrice || 0) - unitCost)).toFixed(2)
}

function addItem() { form.items.push(getDefaultItem()) }
function removeItem(index: number) { if (form.items.length > 1) form.items.splice(index, 1) }

async function openDialog(id?: number) {
  Object.assign(form, getDefaultForm())
  if (id == null) {
    dialogTitle.value = '新增销售单'
    dialogVisible.value = true
    return
  }
  dialogTitle.value = '修改销售单'
  const response = await salesApi.getById(id)
  Object.assign(form, {
    id: response.data.id,
    vipId: response.data.vipId,
    date: response.data.date,
    remark: response.data.remark || '',
    items: (response.data.items || []).map((item) => ({ _key: `${item.id}-${Math.random()}`, flowerId: item.flowerId, quantity: item.quantity, unitPrice: Number(item.unitPrice) })),
  })
  dialogVisible.value = true
}

async function saveSales() {
  if (!form.items.every((item) => item.flowerId && Number(item.quantity) > 0 && Number(item.unitPrice) > 0)) {
    ElMessage.warning('请完整填写每条销售明细')
    return
  }
  const payload = {
    id: form.id,
    vipId: form.vipId,
    date: form.date,
    remark: form.remark,
    items: form.items.map((item) => ({ flowerId: item.flowerId, quantity: Number(item.quantity), unitPrice: Number(item.unitPrice) })),
  }
  const response = await salesApi.save(payload)
  ElMessage.success(response.message)
  dialogVisible.value = false
  await Promise.all([loadSalesList(), loadOptions(), loadTodaySummary()])
}

async function deleteSales(row: SalesVO) {
  await ElMessageBox.confirm(`确认删除销售单 ${row.orderNo} 吗？`, '提示', { type: 'warning' })
  const response = await salesApi.remove(row.id)
  ElMessage.success(response.message)
  await Promise.all([loadSalesList(), loadOptions(), loadTodaySummary()])
}

loadSalesList()
loadOptions()
loadTodaySummary()
</script>

<template>
  <div class="summary-grid page-card">
    <el-card class="summary-card"><div class="summary-label">今日销售额</div><div class="summary-value">¥ {{ Number(todaySummary.todaySalesAmount || 0).toFixed(2) }}</div></el-card>
    <el-card class="summary-card"><div class="summary-label">今日进货成本</div><div class="summary-value">¥ {{ Number(todaySummary.todayPurchaseCost || 0).toFixed(2) }}</div></el-card>
    <el-card class="summary-card"><div class="summary-label">今日销售毛利</div><div class="summary-value">¥ {{ Number(todaySummary.todayGrossProfit || 0).toFixed(2) }}</div></el-card>
    <el-card class="summary-card"><div class="summary-label">今日净流入</div><div class="summary-value">¥ {{ Number(todaySummary.todayNetCashflow || 0).toFixed(2) }}</div><div class="summary-subtext">销售单 {{ todaySummary.todaySalesOrderCount }} 笔 / 采购 {{ todaySummary.todayPurchaseCount }} 笔</div></el-card>
  </div>

  <el-card class="page-card">
    <div class="page-toolbar">
      <div class="page-search">
        <el-input v-model="searchModel.orderNo" placeholder="销售单号" clearable style="width: 220px" />
        <el-date-picker v-model="searchModel.dateRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" clearable />
        <el-button type="primary" @click="loadSalesList">查询</el-button>
      </div>
      <el-button type="primary" @click="openDialog()">新增销售单</el-button>
    </div>
  </el-card>

  <el-card class="page-card">
    <el-table :data="salesList" stripe>
      <el-table-column type="index" label="#" width="70" />
      <el-table-column prop="id" label="销售ID" width="100" />
      <el-table-column prop="orderNo" label="销售单号" width="200" />
      <el-table-column prop="vipName" label="会员" width="120" />
      <el-table-column prop="vipPhone" label="手机号" width="140" />
      <el-table-column prop="itemCount" label="明细数" width="90" />
      <el-table-column prop="totalAmount" label="销售总额(元)" width="130" />
      <el-table-column prop="date" label="销售时间" width="180" />
      <el-table-column prop="remark" label="备注" min-width="180" />
      <el-table-column label="操作" width="160"><template #default="{ row }"><el-button type="primary" size="small" @click="openDialog(row.id)">编辑</el-button><el-button type="danger" size="small" @click="deleteSales(row)">删除</el-button></template></el-table-column>
    </el-table>
    <div style="margin-top:16px;display:flex;justify-content:flex-end;"><el-pagination v-model:current-page="searchModel.pageNo" v-model:page-size="searchModel.pageSize" :page-sizes="[10,25,50,100]" layout="total, sizes, prev, pager, next, jumper" :total="total" @change="loadSalesList" /></div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="920px">
    <el-form :model="form" label-width="110px">
      <div class="dialog-form-grid">
        <el-form-item label="关联会员"><el-select v-model="form.vipId" clearable filterable><el-option v-for="vip in vipOptions" :key="vip.id" :label="`${vip.name}(${vip.phone})`" :value="vip.id" /></el-select></el-form-item>
        <el-form-item label="销售时间"><el-date-picker v-model="form.date" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item label="备注" class="full-width"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit /></el-form-item>
      </div>
      <div style="display:flex;justify-content:space-between;align-items:center;margin:16px 0;"><strong>销售明细</strong><el-button type="primary" size="small" @click="addItem">新增明细</el-button></div>
      <div v-for="(item, index) in form.items" :key="item._key" style="padding:12px 0;border-top:1px solid #e5e7eb;">
        <el-row :gutter="12">
          <el-col :span="8"><el-select v-model="item.flowerId" filterable placeholder="选择花卉" @change="handleFlowerChange(item)"><el-option v-for="flower in flowerOptions" :key="flower.id" :label="`${flower.name}（当前库存:${flower.currentStock}${flower.unit}）`" :value="flower.id" /></el-select></el-col>
          <el-col :span="4"><el-input-number v-model="item.quantity" :min="1" :controls="false" /></el-col>
          <el-col :span="4"><el-input-number v-model="item.unitPrice" :min="0" :precision="2" :controls="false" /></el-col>
          <el-col :span="4">成本单价: {{ calcUnitCost(item) }}</el-col>
          <el-col :span="2">毛利: {{ calcGrossProfit(item) }}</el-col>
          <el-col :span="2" style="text-align:right;"><el-button type="danger" size="small" @click="removeItem(index)">删除</el-button></el-col>
        </el-row>
      </div>
      <div style="margin-top: 16px; text-align: right;">销售总额：<strong>{{ salesTotal }}</strong> 元</div>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="saveSales">确定</el-button></template>
  </el-dialog>
</template>
