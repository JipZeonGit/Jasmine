<template>
  <div>
    <el-card id="search">
      <el-row>
        <el-col :span="20">
          <el-input v-model="searchModel.name" placeholder="花卉名称" clearable />
          <el-button type="primary" round icon="el-icon-search" @click="getFlowerList">查询</el-button>
        </el-col>
        <el-col :span="4" align="right">
          <el-button type="primary" icon="el-icon-plus" circle @click="openEditUI()" />
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <el-table :data="flowerList" stripe style="width: 100%">
        <el-table-column type="index" label="#" width="80">
          <template slot-scope="scope">
            {{ (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="花卉ID" width="100" />
        <el-table-column prop="name" label="花名" width="160" />
        <el-table-column prop="unit" label="单位" width="100" />
        <el-table-column prop="salePrice" label="售价(元)" width="120" />
        <el-table-column prop="costPrice" label="成本价(元)" width="120" />
        <el-table-column prop="safeStock" label="安全库存" width="110" />
        <el-table-column prop="currentStock" label="当前库存" width="110" />
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '在售' : '停售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button type="primary" icon="el-icon-edit" size="mini" @click="openEditUI(scope.row.id)" />
            <el-button type="danger" icon="el-icon-delete" size="mini" @click="deleteFlower(scope.row)" />
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
      <el-form ref="flowerFormRef" :model="flowerForm" :rules="rules">
        <el-form-item label="花卉名称" prop="name" :label-width="formLabelWidth">
          <el-input v-model="flowerForm.name" autocomplete="off" />
        </el-form-item>
        <el-form-item label="单位" prop="unit" :label-width="formLabelWidth">
          <el-select v-model="flowerForm.unit" placeholder="请选择单位">
            <el-option label="枝" value="枝" />
            <el-option label="束" value="束" />
            <el-option label="扎" value="扎" />
            <el-option label="盆" value="盆" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价(元)" prop="salePrice" :label-width="formLabelWidth">
          <el-input-number v-model="flowerForm.salePrice" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="成本价(元)" prop="costPrice" :label-width="formLabelWidth">
          <el-input-number v-model="flowerForm.costPrice" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="安全库存" prop="safeStock" :label-width="formLabelWidth">
          <el-input-number v-model="flowerForm.safeStock" :min="0" :controls="false" />
        </el-form-item>
        <el-form-item label="销售状态" prop="status" :label-width="formLabelWidth">
          <el-radio-group v-model="flowerForm.status">
            <el-radio :label="1">在售</el-radio>
            <el-radio :label="0">停售</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFlower">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import flowerApi from '@/api/flowerManage'

export default {
  data() {
    return {
      formLabelWidth: '130px',
      dialogFormVisible: false,
      title: '',
      total: 0,
      flowerList: [],
      searchModel: {
        name: '',
        pageNo: 1,
        pageSize: 10
      },
      flowerForm: this.getDefaultFlowerForm(),
      rules: {
        name: [
          { required: true, message: '请输入花卉名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        unit: [
          { required: true, message: '请选择单位', trigger: 'change' }
        ],
        salePrice: [
          { required: true, message: '请输入售价', trigger: 'change' }
        ],
        costPrice: [
          { required: true, message: '请输入成本价', trigger: 'change' }
        ],
        safeStock: [
          { required: true, message: '请输入安全库存', trigger: 'change' }
        ],
        status: [
          { required: true, message: '请选择销售状态', trigger: 'change' }
        ]
      }
    }
  },
  methods: {
    getDefaultFlowerForm() {
      return {
        id: null,
        name: '',
        unit: '枝',
        salePrice: 0,
        costPrice: 0,
        safeStock: 0,
        status: 1
      }
    },
    getFlowerList() {
      flowerApi.getFlowerList(this.searchModel).then(response => {
        this.flowerList = response.data.rows
        this.total = response.data.total
      })
    },
    saveFlower() {
      this.$refs.flowerFormRef.validate(valid => {
        if (!valid) {
          return false
        }

        flowerApi.saveFlower(this.flowerForm).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.dialogFormVisible = false
          this.getFlowerList()
        })
      })
    },
    deleteFlower(flower) {
      this.$confirm(`确认删除花卉 ${flower.name} 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        flowerApi.deleteFlowerById(flower.id).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.getFlowerList()
        })
      })
    },
    openEditUI(id) {
      if (id === null || id === undefined) {
        this.title = '新增花卉'
        this.flowerForm = this.getDefaultFlowerForm()
        this.dialogFormVisible = true
        return
      }

      this.title = '修改花卉'
      flowerApi.getFlowerById(id).then(response => {
        this.flowerForm = Object.assign(this.getDefaultFlowerForm(), response.data)
        this.dialogFormVisible = true
      })
    },
    clearForm() {
      this.flowerForm = this.getDefaultFlowerForm()
      if (this.$refs.flowerFormRef) {
        this.$refs.flowerFormRef.clearValidate()
      }
    },
    handleSizeChange(pageSize) {
      this.searchModel.pageSize = pageSize
      this.getFlowerList()
    },
    handleCurrentChange(pageNo) {
      this.searchModel.pageNo = pageNo
      this.getFlowerList()
    }
  },
  created() {
    this.getFlowerList()
  }
}
</script>

<style>
#search .el-input {
  width: 220px;
  margin-right: 20px;
}

#search .el-input__inner {
  border-radius: 20px;
}

.el-dialog .el-input,
.el-dialog .el-select,
.el-dialog .el-input-number {
  width: 85%;
}
</style>
