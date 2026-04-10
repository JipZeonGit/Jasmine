<template>
  <div>
    <el-card id="search">
      <el-row>
        <el-col :span="20">
          <el-input v-model="searchModel.name" placeholder="花卉名称" clearable />
          <el-input v-model="searchModel.num" placeholder="业务单号" clearable />
          <el-select v-model="searchModel.bizType" clearable placeholder="业务类型">
            <el-option
              v-for="item in bizTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
          <el-button type="primary" round icon="el-icon-search" @click="getInventoryList">查询</el-button>
        </el-col>
        <el-col :span="4" align="right">
          <el-button type="primary" icon="el-icon-plus" circle @click="openEditUI()" />
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <el-table :data="inventoryList" stripe style="width: 100%">
        <el-table-column type="index" label="#" width="80">
          <template slot-scope="scope">
            {{ (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="流水ID" width="100" />
        <el-table-column prop="bizNo" label="业务单号" width="190" />
        <el-table-column prop="flowerName" label="花名" width="120" />
        <el-table-column prop="bizTypeLabel" label="业务类型" width="120" />
        <el-table-column prop="quantity" label="变动数量" width="100" />
        <el-table-column prop="beforeStock" label="变动前库存" width="110" />
        <el-table-column prop="afterStock" label="变动后库存" width="110" />
        <el-table-column prop="unitCost" label="成本单价" width="110" />
        <el-table-column prop="totalCost" label="成本小计" width="110" />
        <el-table-column prop="date" label="业务时间" width="180" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button type="primary" icon="el-icon-edit" size="mini" @click="openEditUI(scope.row.id)" />
            <el-button type="danger" icon="el-icon-delete" size="mini" @click="deleteInventory(scope.row)" />
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

    <el-dialog :title="title" :visible.sync="dialogFormVisible" @close="clearForm">
      <el-form ref="inventoryFormRef" :model="inventoryForm" :rules="rules">
        <el-form-item label="花卉" prop="flowerId" :label-width="formLabelWidth">
          <el-select v-model="inventoryForm.flowerId" filterable placeholder="请选择花卉">
            <el-option
              v-for="flower in flowerOptions"
              :key="flower.id"
              :label="formatFlowerOptionLabel(flower)"
              :value="flower.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型" prop="bizType" :label-width="formLabelWidth">
          <el-select v-model="inventoryForm.bizType" placeholder="请选择业务类型">
            <el-option
              v-for="item in bizTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="变动数量" prop="quantity" :label-width="formLabelWidth">
          <el-input-number v-model="inventoryForm.quantity" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item
          v-if="requiresUnitCost"
          label="进货单价"
          prop="unitCost"
          :label-width="formLabelWidth"
        >
          <el-input-number v-model="inventoryForm.unitCost" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="业务时间" prop="date" :label-width="formLabelWidth">
          <el-date-picker
            v-model="inventoryForm.date"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            format="yyyy-MM-dd HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark" :label-width="formLabelWidth">
          <el-input v-model="inventoryForm.remark" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取消</el-button>
        <el-button type="primary" @click="saveInventory">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import inventoryApi from '@/api/inventoryManage'
import flowerApi from '@/api/flowerManage'

const BIZ_TYPE_OPTIONS = [
  { label: '采购入库', value: 'PURCHASE_IN' },
  { label: '销售出库', value: 'SALE_OUT' },
  { label: '损耗出库', value: 'LOSS_OUT' },
  { label: '退货入库', value: 'RETURN_IN' },
  { label: '盘点调增', value: 'CHECK_IN' },
  { label: '盘点调减', value: 'CHECK_OUT' }
]

export default {
  data() {
    return {
      formLabelWidth: '130px',
      dialogFormVisible: false,
      title: '',
      total: 0,
      inventoryList: [],
      flowerOptions: [],
      bizTypeOptions: BIZ_TYPE_OPTIONS,
      searchModel: {
        name: '',
        num: '',
        bizType: '',
        dateRange: [],
        pageNo: 1,
        pageSize: 10
      },
      inventoryForm: this.getDefaultInventoryForm(),
      rules: {
        flowerId: [
          { required: true, message: '请选择花卉', trigger: 'change' }
        ],
        bizType: [
          { required: true, message: '请选择业务类型', trigger: 'change' }
        ],
        quantity: [
          { required: true, message: '请输入变动数量', trigger: 'change' }
        ],
        unitCost: [
          {
            validator: (rule, value, callback) => {
              if (this.requiresUnitCost && (!value || Number(value) <= 0)) {
                callback(new Error('采购入库必须填写有效的进货单价'))
                return
              }
              callback()
            },
            trigger: 'change'
          }
        ],
        date: [
          { required: true, message: '请选择业务时间', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    requiresUnitCost() {
      return this.inventoryForm.bizType === 'PURCHASE_IN'
    }
  },
  methods: {
    getDefaultInventoryForm() {
      return {
        id: null,
        flowerId: null,
        bizType: '',
        quantity: 1,
        unitCost: null,
        date: '',
        remark: ''
      }
    },
    loadFlowerOptions() {
      flowerApi.getAllFlowerList().then(response => {
        this.flowerOptions = response.data.filter(item => item.status === 1)
      })
    },
    formatFlowerOptionLabel(flower) {
      return `${flower.name}（当前库存: ${flower.currentStock} ${flower.unit}）`
    },
    getInventoryList() {
      inventoryApi.getInventoryList(this.searchModel).then(response => {
        this.inventoryList = response.data.rows
        this.total = response.data.total
      })
    },
    saveInventory() {
      this.$refs.inventoryFormRef.validate(valid => {
        if (!valid) {
          return false
        }

        const payload = {
          ...this.inventoryForm,
          quantity: Number(this.inventoryForm.quantity),
          unitCost: this.requiresUnitCost ? Number(this.inventoryForm.unitCost) : null
        }

        inventoryApi.saveInventory(payload).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.dialogFormVisible = false
          this.getInventoryList()
          this.loadFlowerOptions()
        })
      })
    },
    deleteInventory(inventory) {
      this.$confirm(`确认删除库存流水 ${inventory.bizNo} 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        inventoryApi.deleteInventoryById(inventory.id).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.getInventoryList()
          this.loadFlowerOptions()
        })
      })
    },
    openEditUI(id) {
      if (id === null || id === undefined) {
        this.title = '登记库存动作'
        this.inventoryForm = this.getDefaultInventoryForm()
        this.dialogFormVisible = true
        return
      }

      this.title = '修改库存动作'
      inventoryApi.getInventoryById(id).then(response => {
        this.inventoryForm = Object.assign(this.getDefaultInventoryForm(), {
          id: response.data.id,
          flowerId: response.data.flowerId,
          bizType: response.data.bizType,
          quantity: response.data.quantity,
          unitCost: response.data.unitCost,
          date: response.data.date,
          remark: response.data.remark
        })
        this.dialogFormVisible = true
      })
    },
    clearForm() {
      this.inventoryForm = this.getDefaultInventoryForm()
      if (this.$refs.inventoryFormRef) {
        this.$refs.inventoryFormRef.clearValidate()
      }
    },
    handleSizeChange(pageSize) {
      this.searchModel.pageSize = pageSize
      this.getInventoryList()
    },
    handleCurrentChange(pageNo) {
      this.searchModel.pageNo = pageNo
      this.getInventoryList()
    }
  },
  created() {
    this.getInventoryList()
    this.loadFlowerOptions()
  }
}
</script>

<style>
#search .el-input,
#search .el-select,
#search .el-date-editor {
  width: 200px;
  margin-right: 20px;
}

#search .el-date-editor.el-range-editor {
  width: 360px;
}

#search .el-input__inner {
  border-radius: 20px;
}

.el-dialog .el-input,
.el-dialog .el-select,
.el-dialog .el-input-number,
.el-dialog .el-date-editor {
  width: 85%;
}
</style>
