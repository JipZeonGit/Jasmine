<template>
  <div>
    <!-- 搜索栏 -->
    <el-card id="search">
      <el-row>
        <el-col :span="20">
          <el-input v-model="searchModel.name" placeholder="姓名" clearable></el-input>
          <el-input v-model="searchModel.phone" placeholder="手机号" clearable></el-input>
          <el-date-picker
            v-model="searchModel.date"
            type="datetime"
            format="yyyy-MM-dd HH:mm:ss"
            @change="handleSearchDateChange"
            clearable>
          </el-date-picker>
          <el-button @click="getAppointmentList" type="primary" round icon="el-icon-search">查询</el-button>
        </el-col>
        <el-col :span="4" align="right">
          <!-- 圆形按钮 -->
          <el-button @click="openEditUI(null)" type="primary" icon="el-icon-plus" circle></el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 结果列表 -->
    <el-card>
      <el-table :data="appointmentList" stripe style="width: 100%">
        <el-table-column type="index" label="#" width="80">
          <template slot-scope="scope">
            {{ (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="预约ID" width="100"></el-table-column>
        <el-table-column prop="vid" label="会员卡号" width="180"></el-table-column>
        <el-table-column prop="name" label="姓名" width="100"></el-table-column>
        <el-table-column prop="sex" label="性别" width="100"></el-table-column>
        <el-table-column prop="phone" label="手机号" width="180"></el-table-column>
        <el-table-column prop="date" label="预约时间" width="180"></el-table-column>
        <el-table-column prop="content" label="内容" width="180"></el-table-column>
        <el-table-column label="操作">
          <template slot-scope="scope">
            <!-- 编辑按钮 -->
            <el-button @click="openEditUI(scope.row.id)" type="primary" icon="el-icon-edit" size="mini"></el-button>
            <!-- 删除按钮 -->
            <el-button @click="deleteAppointment(scope.row)" type="danger" icon="el-icon-delete" size="mini"></el-button>
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

    <!-- 预约新增对话框 -->
    <el-dialog @close="clearForm" title="新增预约" :visible.sync="addDialogVisible">
      <el-form :model="addAppointmentForm" ref="addAppointmentFormRef" :rules="rules">
        <el-form-item label="手机号" prop="phone" :label-width="formLabelWidth">
          <el-input v-model="addAppointmentForm.phone" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="会员卡号" prop="vid" :label-width="formLabelWidth">
          <el-input v-model="addAppointmentForm.vid" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="内容" prop="content" :label-width="formLabelWidth">
          <el-input v-model="addAppointmentForm.content" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="预约时间" prop="date" :label-width="formLabelWidth">
          <el-date-picker
            v-model="addAppointmentForm.date"
            type="datetime"
            format="yyyy-MM-dd HH:mm:ss"
            @change="handleAddDateChange">
          </el-date-picker>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addAppointment">确定</el-button>
      </div>
    </el-dialog>

    <!-- 预约编辑对话框 -->
    <el-dialog @close="clearForm" title="修改预约" :visible.sync="editDialogVisible">
      <el-form :model="editAppointmentForm" ref="editAppointmentFormRef" :rules="rules">
        <el-form-item label="内容" prop="content" :label-width="formLabelWidth">
          <el-input v-model="editAppointmentForm.content" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="预约时间" prop="date" :label-width="formLabelWidth">
          <el-date-picker
            v-model="editAppointmentForm.date"
            type="datetime"
            format="yyyy-MM-dd HH:mm:ss"
            @change="handleDateChange">
          </el-date-picker>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateAppointment">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import appointmentApi from '@/api/appointmentManage'
import { DatePicker } from 'element-ui'
import moment from 'moment'

export default {
  data() {
    return {
      formLabelWidth: '130px',
      addAppointmentForm: this.getDefaultAddAppointmentForm(),
      editAppointmentForm: this.getDefaultEditAppointmentForm(),
      addDialogVisible: false,
      editDialogVisible: false,
      total: 0,
      searchModel: {
        name: '',
        phone: '',
        date: '',
        pageNo: 1,
        pageSize: 10
      },
      appointmentList: [],
      rules: {
        content: [
          { required: true, message: '请输入内容', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        phone: [
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        vid: [
          { pattern: /^\d{11}$/, message: '会员卡号格式不正确', trigger: 'blur' }
        ],
        date: [
          { required: true, message: '请选择预约时间', trigger: 'change' }
        ]
      }
    }
  },
  methods: {
    getDefaultAddAppointmentForm() {
      return {
        vid: '',
        phone: '',
        content: '',
        date: ''
      }
    },
    getDefaultEditAppointmentForm() {
      return {
        id: null,
        content: '',
        date: '',
        vid: '',
        name: '',
        sex: '',
        phone: ''
      }
    },
    handleSearchDateChange() {
      if (this.searchModel.date) {
        this.searchModel.date = moment(this.searchModel.date).format('YYYY-MM-DD HH:mm:ss')
      }
    },
    handleDateChange() {
      if (this.editAppointmentForm.date) {
        this.editAppointmentForm.date = moment(this.editAppointmentForm.date).format('YYYY-MM-DD HH:mm:ss')
      }
    },
    handleAddDateChange() {
      if (this.addAppointmentForm.date) {
        this.addAppointmentForm.date = moment(this.addAppointmentForm.date).format('YYYY-MM-DD HH:mm:ss')
      }
    },
    deleteAppointment(appointment) {
      this.$confirm(`您确认删除预约 ${appointment.name} 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        appointmentApi.deleteAppointmentById(appointment.id).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.getAppointmentList()
        })
      }).catch(() => {
        this.$message({ type: 'info', message: '已取消删除' })
      })
    },
    updateAppointment() {
      this.$refs.editAppointmentFormRef.validate(valid => {
        if (!valid) {
          return false
        }
        appointmentApi.updateAppointment(this.editAppointmentForm).then(response => {
          this.$message({ message: response.message, type: 'success' })
          this.editDialogVisible = false
          this.getAppointmentList()
        })
      })
    },
    addAppointment() {
      this.$refs.addAppointmentFormRef.validate(valid => {
        if (!valid) {
          return false
        }
        appointmentApi.addAppointment(this.addAppointmentForm).then(response => {
          this.$message({ message: response.message, type: 'success' })
          this.addDialogVisible = false
          this.getAppointmentList()
        })
      })
    },
    clearForm() {
      this.addAppointmentForm = this.getDefaultAddAppointmentForm()
      this.editAppointmentForm = this.getDefaultEditAppointmentForm()
      if (this.$refs.addAppointmentFormRef) {
        this.$refs.addAppointmentFormRef.clearValidate()
      }
      if (this.$refs.editAppointmentFormRef) {
        this.$refs.editAppointmentFormRef.clearValidate()
      }
    },
    openEditUI(id) {
      if (id === null || id === undefined) {
        // 新增操作
        this.addAppointmentForm = this.getDefaultAddAppointmentForm()
        this.addDialogVisible = true
        return
      }

      // 编辑操作
      appointmentApi.getAppointmentById(id).then(response => {
        this.editAppointmentForm = Object.assign(this.getDefaultEditAppointmentForm(), response.data)
        this.editDialogVisible = true
      })
    },
    handleSizeChange(pageSize) {
      this.searchModel.pageSize = pageSize
      this.getAppointmentList()
    },
    handleCurrentChange(pageNo) {
      this.searchModel.pageNo = pageNo
      this.getAppointmentList()
    },
    getAppointmentList() {
      appointmentApi.getAppointmentList(this.searchModel).then(response => {
        this.appointmentList = response.data.rows
        this.total = response.data.total
      })
    }
  },
  created() {
    this.getAppointmentList()
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
