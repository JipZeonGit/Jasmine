<template>
  <div>
    <!-- 搜索栏 -->
    <el-card id="search">
      <el-row>
        <el-col :span="20">
          <el-input v-model="searchModel.name" placeholder="花名" clearable></el-input>
          <el-input v-model="searchModel.num" placeholder="单号" clearable></el-input>
          <el-button @click="getInventoryList" type="primary" round icon="el-icon-search">查询</el-button>
        </el-col>
        <el-col :span="4" align="right">
          <!-- 圆形按钮 -->
          <el-button @click="openEditUI(null)" type="primary" icon="el-icon-plus" circle></el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 结果列表 -->
    <el-card>
      <el-table :data="inventoryList" stripe style="width: 100%">
        <el-table-column type="index" label="#" width="80">
          <template slot-scope="scope">
            {{ (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="仓库ID" width="180"></el-table-column>
        <el-table-column prop="name" label="花名" width="180"></el-table-column>
        <el-table-column prop="num" label="单号" width="240"></el-table-column>
        <el-table-column prop="quantity" label="出/入库数量" width="180">
          <template slot-scope="scope">
            <strong>
              <el-tag v-if="scope.row.quantity > 0" style="font-size: 15px;">+&nbsp;<span>{{ scope.row.quantity }}</span></el-tag>
              <el-tag v-if="scope.row.quantity < 0" type="danger" style="font-size: 15px;">-&nbsp;<span>{{ Math.abs(scope.row.quantity) }}</span></el-tag>
            </strong>
          </template>
        </el-table-column>
        <el-table-column prop="date" label="出/入库时间" width="240"></el-table-column>
        <el-table-column prop="residue" label="余量"></el-table-column>
        <el-table-column label="操作">
          <template slot-scope="scope">
            <!-- 编辑按钮 -->
            <el-button @click="openEditUI(scope.row.id)" type="primary" icon="el-icon-edit" size="mini"></el-button>
            <!-- 删除按钮 -->
            <el-button @click="deleteInventory(scope.row)" type="danger" icon="el-icon-delete" size="mini"></el-button>
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

    <!-- 仓库信息新增/编辑对话框 -->
    <el-dialog @close="clearForm" :title="title" :visible.sync="dialogFormVisible">
      <el-form :model="inventoryForm" ref="inventoryFormRef" :rules="rules">
        <el-form-item label="花名" prop="name" :label-width="formLabelWidth">
          <el-input v-model="inventoryForm.name" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="出/入库数量" prop="quantity" :label-width="formLabelWidth">
          <el-input v-model="inventoryForm.quantity" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="出/入库时间" prop="date" :label-width="formLabelWidth">
          <el-date-picker
            v-model="inventoryForm.date"
            type="datetime"
            format="yyyy-MM-dd HH:mm:ss"
            @change="handleDateChange">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="余量" prop="residue" :label-width="formLabelWidth">
          <el-input v-model="inventoryForm.residue" autocomplete="off"></el-input>
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
import { DatePicker } from 'element-ui'
import moment from 'moment'

export default {
  data() {
    return {
      formLabelWidth: '130px',
      inventoryForm: this.getDefaultInventoryForm(),
      dialogFormVisible: false,
      title: '',
      total: 0,
      searchModel: {
        name: '',
        num: '',
        pageNo: 1,
        pageSize: 10
      },
      inventoryList: [],
      rules: {
        name: [
          { required: true, message: '请输入花名', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        quantity: [
          { required: true, message: '请输入出/入库数量', trigger: 'blur' },
          { pattern: /^-?[1-9]\d*$/, message: '数量必须是非零整数', trigger: 'blur' }
        ],
        date: [
          { required: true, message: '请选择出/入库时间', trigger: 'change' }
        ],
        residue: [
          { required: true, message: '请输入余量', trigger: 'blur' },
          { pattern: /^\d+$/, message: '余量必须是大于或等于 0 的整数', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    getDefaultInventoryForm() {
      return {
        id: null,
        name: '',
        num: '',
        quantity: '',
        date: '',
        residue: ''
      }
    },
    handleDateChange() {
      if (this.inventoryForm.date) {
        this.inventoryForm.date = moment(this.inventoryForm.date).format('YYYY-MM-DD HH:mm:ss')
      }
    },
    deleteInventory(inventory) {
      this.$confirm(`您确认删除仓库数据 ${inventory.name} 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        inventoryApi.deleteInventoryById(inventory.id).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.getInventoryList()
        })
      }).catch(() => {
        this.$message({ type: 'info', message: '已取消删除' })
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
          residue: Number(this.inventoryForm.residue)
        }
        inventoryApi.saveInventory(payload).then(response => {
          this.$message({ message: response.message, type: 'success' })
          this.dialogFormVisible = false
          this.getInventoryList()
        })
      })
    },
    clearForm() {
      this.inventoryForm = this.getDefaultInventoryForm()
      if (this.$refs.inventoryFormRef) {
        this.$refs.inventoryFormRef.clearValidate()
      }
    },
    openEditUI(id) {
      if (id === null || id === undefined) {
        // 新增操作
        this.title = '新增仓库数据'
        this.inventoryForm = this.getDefaultInventoryForm()
        this.dialogFormVisible = true
        return
      }

      // 编辑操作
      this.title = '修改仓库数据'
      inventoryApi.getInventoryById(id).then(response => {
        this.inventoryForm = Object.assign(this.getDefaultInventoryForm(), response.data)
        this.dialogFormVisible = true
      })
    },
    handleSizeChange(pageSize) {
      this.searchModel.pageSize = pageSize
      this.getInventoryList()
    },
    handleCurrentChange(pageNo) {
      this.searchModel.pageNo = pageNo
      this.getInventoryList()
    },
    getInventoryList() {
      inventoryApi.getInventoryList(this.searchModel).then(response => {
        this.inventoryList = response.data.rows
        this.total = response.data.total
      })
    }
  },
  created() {
    this.getInventoryList()
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
