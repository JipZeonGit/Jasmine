<template>
  <div>
    <el-row :gutter="16" class="summary-row">
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">今日销售额</div>
          <div class="summary-value">¥ {{ formatMoney(todaySummary.todaySalesAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">今日进货成本</div>
          <div class="summary-value">¥ {{ formatMoney(todaySummary.todayPurchaseCost) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">今日销售毛利</div>
          <div class="summary-value">¥ {{ formatMoney(todaySummary.todayGrossProfit) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">今日净流入</div>
          <div class="summary-value">¥ {{ formatMoney(todaySummary.todayNetCashflow) }}</div>
        </el-card>
        <div class="summary-subtext">
          销售单 {{ todaySummary.todaySalesOrderCount }} 笔 / 采购 {{ todaySummary.todayPurchaseCount }} 笔
        </div>
      </el-col>
    </el-row>

    <el-card id="search">
      <el-row>
        <el-col :span="20">
          <el-input v-model="searchModel.orderNo" placeholder="销售单号" clearable />
          <el-date-picker
            v-model="searchModel.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            format="yyyy-MM-dd HH:mm:ss"
            clearable
          />
          <el-button type="primary" round icon="el-icon-search" @click="getSalesList">查询</el-button>
        </el-col>
        <el-col :span="4" align="right">
          <el-button type="primary" icon="el-icon-plus" circle @click="openEditUI()" />
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <el-table :data="salesList" stripe style="width: 100%">
        <el-table-column type="index" label="#" width="80">
          <template slot-scope="scope">
            {{ (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="销售ID" width="100" />
        <el-table-column prop="orderNo" label="销售单号" width="190" />
        <el-table-column prop="vipName" label="会员" width="120" />
        <el-table-column prop="vipPhone" label="手机号" width="140" />
        <el-table-column prop="itemCount" label="明细数" width="90" />
        <el-table-column prop="totalAmount" label="销售总额(元)" width="130" />
        <el-table-column prop="date" label="销售时间" width="180" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button type="primary" icon="el-icon-edit" size="mini" @click="openEditUI(scope.row.id)" />
            <el-button type="danger" icon="el-icon-delete" size="mini" @click="deleteSales(scope.row)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-pagination
      :current-page="searchModel.pageNo"
      :page-size="searchModel.pageSize"
      :page-sizes="[10, 25, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <el-dialog :title="title" :visible.sync="dialogFormVisible" width="920px" @close="clearForm">
      <el-form ref="salesFormRef" :model="salesForm" :rules="rules">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联会员" prop="vipId" :label-width="formLabelWidth">
              <el-select v-model="salesForm.vipId" clearable filterable placeholder="可选">
                <el-option
                  v-for="vip in vipOptions"
                  :key="vip.id"
                  :label="`${vip.name}(${vip.phone})`"
                  :value="vip.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售时间" prop="date" :label-width="formLabelWidth">
              <el-date-picker
                v-model="salesForm.date"
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss"
                format="yyyy-MM-dd HH:mm:ss"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remark" :label-width="formLabelWidth">
          <el-input v-model="salesForm.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>

        <div class="item-toolbar">
          <span>销售明细</span>
          <el-button type="primary" size="mini" icon="el-icon-plus" @click="addItem">新增明细</el-button>
        </div>

        <div v-for="(item, index) in salesForm.items" :key="item._key" class="sales-item-row">
          <el-row :gutter="12">
            <el-col :span="8">
              <el-select v-model="item.flowerId" filterable placeholder="选择花卉" @change="handleFlowerChange(item)">
                <el-option
                  v-for="flower in flowerOptions"
                  :key="flower.id"
                  :label="formatFlowerOptionLabel(flower)"
                  :value="flower.id"
                />
              </el-select>
            </el-col>
            <el-col :span="3">
              <el-input-number v-model="item.quantity" :min="1" :controls="false" />
            </el-col>
            <el-col :span="4">
              <el-input-number v-model="item.unitPrice" :min="0" :precision="2" :controls="false" />
            </el-col>
            <el-col :span="4" class="item-meta">
              成本单价: {{ calcUnitCost(item) }}
            </el-col>
            <el-col :span="3" class="item-meta">
              毛利: {{ calcGrossProfit(item) }}
            </el-col>
            <el-col :span="2" align="right">
              <el-button type="danger" icon="el-icon-delete" size="mini" @click="removeItem(index)" />
            </el-col>
          </el-row>
        </div>

        <div class="total-row">
          销售总额: <strong>{{ salesTotal }}</strong> 元
        </div>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSales">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import salesApi from '@/api/salesManage'
import flowerApi from '@/api/flowerManage'
import vipApi from '@/api/vipManage'

export default {
  data() {
    return {
      formLabelWidth: '120px',
      dialogFormVisible: false,
      title: '',
      total: 0,
      salesList: [],
      flowerOptions: [],
      vipOptions: [],
      todaySummary: {
        todaySalesAmount: 0,
        todayPurchaseCost: 0,
        todayGrossProfit: 0,
        todayNetCashflow: 0,
        todaySalesOrderCount: 0,
        todayPurchaseCount: 0
      },
      searchModel: {
        orderNo: '',
        dateRange: [],
        pageNo: 1,
        pageSize: 10
      },
      salesForm: this.getDefaultSalesForm(),
      rules: {
        date: [
          { required: true, message: '请选择销售时间', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    salesTotal() {
      return this.salesForm.items.reduce((total, item) => {
        return total + Number(item.quantity || 0) * Number(item.unitPrice || 0)
      }, 0).toFixed(2)
    }
  },
  methods: {
    getDefaultItem() {
      return {
        _key: `${Date.now()}-${Math.random()}`,
        flowerId: null,
        quantity: 1,
        unitPrice: 0
      }
    },
    getDefaultSalesForm() {
      return {
        id: null,
        vipId: null,
        date: '',
        remark: '',
        items: [this.getDefaultItem()]
      }
    },
    loadOptions() {
      flowerApi.getAllFlowerList().then(response => {
        this.flowerOptions = response.data.filter(item => item.status === 1)
      })
      vipApi.getAllVipList().then(response => {
        this.vipOptions = response.data
      })
    },
    loadTodaySummary() {
      salesApi.getTodaySummary().then(response => {
        this.todaySummary = response.data
      })
    },
    getSalesList() {
      salesApi.getSalesList(this.searchModel).then(response => {
        this.salesList = response.data.rows
        this.total = response.data.total
      })
    },
    formatFlowerOptionLabel(flower) {
      return `${flower.name}（当前库存: ${flower.currentStock} ${flower.unit}）`
    },
    handleFlowerChange(item) {
      const flower = this.flowerOptions.find(option => option.id === item.flowerId)
      // 重新选择花卉时，销售单价应同步回该花卉的默认售价，避免沿用上一行的旧价格。
      if (flower) {
        item.unitPrice = Number(flower.salePrice)
      }
    },
    calcUnitCost(item) {
      const flower = this.flowerOptions.find(option => option.id === item.flowerId)
      return flower ? Number(flower.costPrice).toFixed(2) : '0.00'
    },
    calcGrossProfit(item) {
      const flower = this.flowerOptions.find(option => option.id === item.flowerId)
      const unitCost = flower ? Number(flower.costPrice) : 0
      return (Number(item.quantity || 0) * (Number(item.unitPrice || 0) - unitCost)).toFixed(2)
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    },
    addItem() {
      this.salesForm.items.push(this.getDefaultItem())
    },
    removeItem(index) {
      if (this.salesForm.items.length === 1) {
        this.$message.warning('至少保留一条销售明细')
        return
      }
      this.salesForm.items.splice(index, 1)
    },
    validateItems() {
      const valid = this.salesForm.items.every(item => item.flowerId && Number(item.quantity) > 0 && Number(item.unitPrice) > 0)
      if (!valid) {
        this.$message.warning('请完整填写每条销售明细')
      }
      return valid
    },
    saveSales() {
      this.$refs.salesFormRef.validate(valid => {
        if (!valid || !this.validateItems()) {
          return false
        }

        const payload = {
          id: this.salesForm.id,
          vipId: this.salesForm.vipId,
          date: this.salesForm.date,
          remark: this.salesForm.remark,
          items: this.salesForm.items.map(item => ({
            flowerId: item.flowerId,
            quantity: Number(item.quantity),
            unitPrice: Number(item.unitPrice)
          }))
        }

        salesApi.saveSales(payload).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.dialogFormVisible = false
          this.getSalesList()
          this.loadOptions()
          this.loadTodaySummary()
        })
      })
    },
    deleteSales(sales) {
      this.$confirm(`确认删除销售单 ${sales.orderNo} 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        salesApi.deleteSalesById(sales.id).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.getSalesList()
          this.loadOptions()
          this.loadTodaySummary()
        })
      })
    },
    openEditUI(id) {
      if (id === null || id === undefined) {
        this.title = '新增销售单'
        this.salesForm = this.getDefaultSalesForm()
        this.dialogFormVisible = true
        return
      }

      this.title = '修改销售单'
      salesApi.getSalesById(id).then(response => {
        const data = response.data
        this.salesForm = {
          id: data.id,
          vipId: data.vipId,
          date: data.date,
          remark: data.remark || '',
          items: (data.items || []).map(item => ({
            _key: `${item.id}-${Math.random()}`,
            flowerId: item.flowerId,
            quantity: item.quantity,
            unitPrice: Number(item.unitPrice)
          }))
        }

        if (this.salesForm.items.length === 0) {
          this.salesForm.items = [this.getDefaultItem()]
        }
        this.dialogFormVisible = true
      })
    },
    clearForm() {
      this.salesForm = this.getDefaultSalesForm()
      if (this.$refs.salesFormRef) {
        this.$refs.salesFormRef.clearValidate()
      }
    },
    handleSizeChange(pageSize) {
      this.searchModel.pageSize = pageSize
      this.getSalesList()
    },
    handleCurrentChange(pageNo) {
      this.searchModel.pageNo = pageNo
      this.getSalesList()
    }
  },
  created() {
    this.getSalesList()
    this.loadOptions()
    this.loadTodaySummary()
  }
}
</script>

<style>
.summary-row {
  margin-bottom: 16px;
}

.summary-card {
  min-height: 110px;
}

.summary-label {
  color: #909399;
  font-size: 14px;
}

.summary-value {
  margin-top: 14px;
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.summary-subtext {
  margin-top: 8px;
  color: #606266;
  font-size: 12px;
  text-align: right;
}

#search .el-input,
#search .el-date-editor {
  width: 220px;
  margin-right: 20px;
}

#search .el-date-editor.el-range-editor {
  width: 360px;
}

#search .el-input__inner {
  border-radius: 20px;
}

.el-dialog .el-select,
.el-dialog .el-input,
.el-dialog .el-date-editor,
.el-dialog .el-input-number {
  width: 100%;
}

.item-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0 12px;
  font-weight: 600;
}

.sales-item-row {
  margin-bottom: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.item-meta {
  line-height: 32px;
  color: #606266;
}

.total-row {
  text-align: right;
  margin-top: 12px;
  font-size: 16px;
}
</style>
