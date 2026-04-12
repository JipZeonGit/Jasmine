<template>
  <div>
    <!-- 搜索栏 -->
    <el-card id="search">
      <el-row>
        <el-col :span="20">
          <el-input v-model="searchModel.vid" placeholder="会员卡号" clearable></el-input>
          <el-input v-model="searchModel.name" placeholder="姓名" clearable></el-input>
          <el-input v-model="searchModel.phone" placeholder="手机号" clearable></el-input>
          <el-button @click="getVipList" type="primary" round icon="el-icon-search">查询</el-button>
        </el-col>
        <el-col :span="4" align="right">
          <!-- 圆形按钮 -->
          <el-button @click="openEditUI(null)" type="primary" icon="el-icon-plus" circle></el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 结果列表 -->
    <el-card>
      <el-table :data="vipList" stripe style="width: 100%">
        <el-table-column type="index" label="#" width="80">
          <template slot-scope="scope">
            {{ (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="会员ID" width="180"></el-table-column>
        <el-table-column prop="vid" label="会员卡号" width="240"></el-table-column>
        <el-table-column prop="name" label="姓名" width="180"></el-table-column>
        <el-table-column prop="sex" label="性别" width="180"></el-table-column>
        <el-table-column prop="phone" label="手机号" width="180"></el-table-column>
        <el-table-column label="操作">
          <template slot-scope="scope">
            <!-- 编辑按钮 -->
            <el-button @click="openEditUI(scope.row.id)" type="primary" icon="el-icon-edit" size="mini"></el-button>
            <!-- 删除按钮 -->
            <el-button @click="deleteVip(scope.row)" type="danger" icon="el-icon-delete" size="mini"></el-button>
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

    <!-- 会员信息新增/编辑对话框 -->
    <el-dialog @close="clearForm" :title="title" :visible.sync="dialogFormVisible">
      <el-form :model="vipForm" ref="vipFormRef" :rules="rules">
        <el-form-item label="姓名" prop="name" :label-width="formLabelWidth">
          <el-input v-model="vipForm.name" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="性别" prop="sex" :label-width="formLabelWidth">
          <el-radio v-model="vipForm.sex" label="男">男</el-radio>
          <el-radio v-model="vipForm.sex" label="女">女</el-radio>
        </el-form-item>
        <el-form-item label="手机号" prop="phone" :label-width="formLabelWidth">
          <el-input v-model="vipForm.phone" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取消</el-button>
        <el-button type="primary" @click="saveVip">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import vipApi from '@/api/vipManage'

export default {
  data() {
    return {
      formLabelWidth: '130px',
      vipForm: this.getDefaultVipForm(),
      dialogFormVisible: false,
      title: '',
      total: 0,
      searchModel: {
        vid: '',
        name: '',
        phone: '',
        pageNo: 1,
        pageSize: 10
      },
      vipList: [],
      rules: {
        name: [
          { required: true, message: '请输入姓名', trigger: 'blur' },
          { min: 1, max: 10, message: '长度在 1 到 10 个字符', trigger: 'blur' }
        ],
        sex: [
          { required: true, message: '请选择性别', trigger: 'change' }
        ],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    getDefaultVipForm() {
      return {
        id: null,
        vid: '',
        name: '',
        sex: '',
        phone: ''
      }
    },
    deleteVip(vip) {
      this.$confirm(`您确认删除会员 ${vip.name} 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        vipApi.deleteVipById(vip.id).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.getVipList()
        })
      }).catch(() => {
        this.$message({ type: 'info', message: '已取消删除' })
      })
    },
    saveVip() {
      this.$refs.vipFormRef.validate(valid => {
        if (!valid) {
          return false
        }
        vipApi.saveVip(this.vipForm).then(response => {
          this.$message({ message: response.message, type: 'success' })
          this.dialogFormVisible = false
          this.getVipList()
        })
      })
    },
    clearForm() {
      this.vipForm = this.getDefaultVipForm()
      if (this.$refs.vipFormRef) {
        this.$refs.vipFormRef.clearValidate()
      }
    },
    openEditUI(id) {
      if (id === null || id === undefined) {
        // 新增操作
        this.title = '新增会员'
        this.vipForm = this.getDefaultVipForm()
        this.dialogFormVisible = true
        return
      }

      // 编辑操作
      this.title = '修改会员'
      vipApi.getVipById(id).then(response => {
        this.vipForm = Object.assign(this.getDefaultVipForm(), response.data)
        this.dialogFormVisible = true
      })
    },
    handleSizeChange(pageSize) {
      this.searchModel.pageSize = pageSize
      this.getVipList()
    },
    handleCurrentChange(pageNo) {
      this.searchModel.pageNo = pageNo
      this.getVipList()
    },
    getVipList() {
      vipApi.getVipList(this.searchModel).then(response => {
        this.vipList = response.data.rows
        this.total = response.data.total
      })
    }
  },
  created() {
    this.getVipList()
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
