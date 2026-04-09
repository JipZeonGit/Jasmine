<template>
  <div>
    <!-- 搜索栏 -->
    <el-card id="search">
      <el-row>
        <el-col :span="20">
          <el-date-picker
            v-model="searchModel.date"
            type="datetime"
            format="yyyy-MM-dd HH:mm:ss"
            @change="handleSearchDateChange"
            clearable>
          </el-date-picker>
          <el-button @click="getSalesList" type="primary" round icon="el-icon-search">查询</el-button>
        </el-col>
        <el-col :span="4" align="right">
          <!-- 圆形按钮 -->
          <el-button @click="openEditUI(null)" type="primary" icon="el-icon-plus" circle></el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 结果列表 -->
    <el-card>
      <el-table :data="salesList" stripe style="width: 100%">
        <el-table-column type="index" label="#" width="80">
          <template slot-scope="scope">
            {{ (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="销售ID" width="180"></el-table-column>
        <el-table-column prop="date" label="统计时间（年-月-日 时:分:秒）" width="240"></el-table-column>
        <el-table-column prop="money" label="营业额（元）">
          <template slot-scope="scope">
            <strong>
              <el-tag v-if="scope.row.money > 0" style="font-size: 15px;">+&nbsp;<span>{{ scope.row.money }}</span></el-tag>
              <el-tag v-if="scope.row.money < 0" type="danger" style="font-size: 15px;">-&nbsp;<span>{{ Math.abs(scope.row.money) }}</span></el-tag>
            </strong>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template slot-scope="scope">
            <!-- 编辑按钮 -->
            <el-button @click="openEditUI(scope.row.id)" type="primary" icon="el-icon-edit" size="mini"></el-button>
            <!-- 删除按钮 -->
            <el-button @click="deleteSales(scope.row)" type="danger" icon="el-icon-delete" size="mini"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 分页组件 -->
    <el-pagination
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="searchModel.pageNo"
      :page-sizes="[10, 25, 50, 100, 250, 500]"
      :page-size="searchModel.pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total">
    </el-pagination>

    <!-- 销售信息新增/编辑对话框 -->
    <el-dialog @close="clearForm" :title="title" :visible.sync="dialogFormVisible">
      <el-form :model="salesForm" ref="salesFormRef" :rules="rules">
        <el-form-item label="营业额（元）" prop="money" :label-width="formLabelWidth">
          <el-input v-model="salesForm.money" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="统计时间" prop="date" :label-width="formLabelWidth">
          <el-date-picker
            v-model="salesForm.date"
            type="datetime"
            format="yyyy-MM-dd HH:mm:ss"
            @change="handleDateChange">
          </el-date-picker>
        </el-form-item>
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
import { DatePicker } from 'element-ui'
import moment from 'moment'

export default {
  data() {
    return {
      formLabelWidth: '130px',
      salesForm: this.getDefaultSalesForm(),
      dialogFormVisible: false,
      title: '',
      total: 0,
      searchModel: {
        date: '',
        pageNo: 1,
        pageSize: 10
      },
      salesList: [],
      rules: {
        money: [
          { required: true, message: '请输入营业额', trigger: 'blur' },
          { pattern: /^-?\d+(\.\d{1,2})?$/, message: '金额格式不正确', trigger: 'blur' }
        ],
        date: [
          { required: true, message: '请选择统计时间', trigger: 'change' }
        ]
      }
    }
  },
  methods: {
    getDefaultSalesForm() {
      return {
        id: null,
        date: '',
        money: ''
      }
    },
    handleSearchDateChange() {
      if (this.searchModel.date) {
        this.searchModel.date = moment(this.searchModel.date).format('YYYY-MM-DD HH:mm:ss')
      }
    },
    handleDateChange() {
      if (this.salesForm.date) {
        this.salesForm.date = moment(this.salesForm.date).format('YYYY-MM-DD HH:mm:ss')
      }
    },
    deleteSales(sales) {
      this.$confirm(`您确认删除销售记录 ${sales.date} 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        salesApi.deleteSalesById(sales.id).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.getSalesList()
        })
      }).catch(() => {
        this.$message({ type: 'info', message: '已取消删除' })
      })
    },
    saveSales() {
      this.$refs.salesFormRef.validate(valid => {
        if (!valid) {
          return false
        }
        salesApi.saveSales(this.salesForm).then(response => {
          this.$message({ message: response.message, type: 'success' })
          this.dialogFormVisible = false
          this.getSalesList()
        })
      })
    },
    clearForm() {
      this.salesForm = this.getDefaultSalesForm()
      if (this.$refs.salesFormRef) {
        this.$refs.salesFormRef.clearValidate()
      }
    },
    openEditUI(id) {
      if (id === null || id === undefined) {
        // 新增操作
        this.title = '新增销售数据'
        this.salesForm = this.getDefaultSalesForm()
        this.dialogFormVisible = true
        return
      }

      // 编辑操作
      this.title = '修改销售数据'
      salesApi.getSalesById(id).then(response => {
        this.salesForm = Object.assign(this.getDefaultSalesForm(), response.data)
        this.dialogFormVisible = true
      })
    },
    handleSizeChange(pageSize) {
      this.searchModel.pageSize = pageSize
      this.getSalesList()
    },
    handleCurrentChange(pageNo) {
      this.searchModel.pageNo = pageNo
      this.getSalesList()
    },
    getSalesList() {
      salesApi.getSalesList(this.searchModel).then(response => {
        this.salesList = response.data.rows
        this.total = response.data.total
      })
    }
  },
  created() {
    this.getSalesList()
  },
  components: {
    'el-date-picker': DatePicker
  }
}
</script>

<style>
#search .el-input {
  width: 200px;
  margin-right: 20px;
}

#search .el-input__inner {
  border-radius: 20px;
}

.el-dialog .el-input {
  width: 85%;
}
</style>
