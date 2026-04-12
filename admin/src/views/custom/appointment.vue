<template>
  <div>
    <el-card id="search">
      <el-row>
        <el-col :span="20">
          <el-input v-model="searchModel.name" placeholder="会员姓名" clearable />
          <el-input v-model="searchModel.phone" placeholder="手机号" clearable />
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
          <el-button type="primary" round icon="el-icon-search" @click="getAppointmentList">查询</el-button>
        </el-col>
        <el-col :span="4" align="right">
          <el-button type="primary" icon="el-icon-plus" circle @click="openEditUI()" />
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <el-table :data="appointmentList" stripe style="width: 100%">
        <el-table-column type="index" label="#" width="80">
          <template slot-scope="scope">
            {{ (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="预约ID" width="100" />
        <el-table-column prop="vid" label="会员卡号" width="150" />
        <el-table-column prop="name" label="会员姓名" width="120" />
        <el-table-column prop="sex" label="性别" width="80" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="date" label="预约时间" width="180" />
        <el-table-column prop="content" label="预约内容" />
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button type="primary" icon="el-icon-edit" size="mini" @click="openEditUI(scope.row.id)" />
            <el-button type="danger" icon="el-icon-delete" size="mini" @click="deleteAppointment(scope.row)" />
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

    <el-dialog title="新增预约" :visible.sync="addDialogVisible" @close="clearForm">
      <el-form ref="addAppointmentFormRef" :model="addAppointmentForm" :rules="rules">
        <el-form-item label="直接选会员" prop="vipId" :label-width="formLabelWidth">
          <el-select v-model="addAppointmentForm.vipId" clearable filterable placeholder="优先使用真实会员关联">
            <el-option
              v-for="vip in vipOptions"
              :key="vip.id"
              :label="`${vip.name}(${vip.phone})`"
              :value="vip.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone" :label-width="formLabelWidth">
          <el-input v-model="addAppointmentForm.phone" autocomplete="off" />
        </el-form-item>
        <el-form-item label="会员卡号" prop="vid" :label-width="formLabelWidth">
          <el-input v-model="addAppointmentForm.vid" autocomplete="off" />
        </el-form-item>
        <el-form-item label="预约内容" prop="content" :label-width="formLabelWidth">
          <el-input v-model="addAppointmentForm.content" autocomplete="off" />
        </el-form-item>
        <el-form-item label="预约时间" prop="date" :label-width="formLabelWidth">
          <el-date-picker
            v-model="addAppointmentForm.date"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            format="yyyy-MM-dd HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addAppointment">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="修改预约" :visible.sync="editDialogVisible" @close="clearForm">
      <el-form ref="editAppointmentFormRef" :model="editAppointmentForm" :rules="editRules">
        <el-form-item label="预约内容" prop="content" :label-width="formLabelWidth">
          <el-input v-model="editAppointmentForm.content" autocomplete="off" />
        </el-form-item>
        <el-form-item label="预约时间" prop="date" :label-width="formLabelWidth">
          <el-date-picker
            v-model="editAppointmentForm.date"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            format="yyyy-MM-dd HH:mm:ss"
          />
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
import vipApi from '@/api/vipManage'

export default {
  data() {
    return {
      formLabelWidth: '130px',
      total: 0,
      vipOptions: [],
      appointmentList: [],
      addDialogVisible: false,
      editDialogVisible: false,
      searchModel: {
        name: '',
        phone: '',
        dateRange: [],
        pageNo: 1,
        pageSize: 10
      },
      addAppointmentForm: this.getDefaultAddAppointmentForm(),
      editAppointmentForm: this.getDefaultEditAppointmentForm(),
      rules: {
        content: [
          { required: true, message: '请输入预约内容', trigger: 'blur' }
        ],
        date: [
          { required: true, message: '请选择预约时间', trigger: 'change' }
        ],
        phone: [
          { pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        vid: [
          { pattern: /^$|^\d{11}$/, message: '会员卡号格式不正确', trigger: 'blur' }
        ]
      },
      editRules: {
        content: [
          { required: true, message: '请输入预约内容', trigger: 'blur' }
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
        vipId: null,
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
        date: ''
      }
    },
    loadVipOptions() {
      vipApi.getAllVipList().then(response => {
        this.vipOptions = response.data
      })
    },
    getAppointmentList() {
      appointmentApi.getAppointmentList(this.searchModel).then(response => {
        this.appointmentList = response.data.rows
        this.total = response.data.total
      })
    },
    addAppointment() {
      this.$refs.addAppointmentFormRef.validate(valid => {
        if (!valid) {
          return false
        }
        appointmentApi.addAppointment(this.addAppointmentForm).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.addDialogVisible = false
          this.getAppointmentList()
        })
      })
    },
    updateAppointment() {
      this.$refs.editAppointmentFormRef.validate(valid => {
        if (!valid) {
          return false
        }
        appointmentApi.updateAppointment(this.editAppointmentForm).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.editDialogVisible = false
          this.getAppointmentList()
        })
      })
    },
    deleteAppointment(appointment) {
      this.$confirm(`确认删除预约 ${appointment.name} 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        appointmentApi.deleteAppointmentById(appointment.id).then(response => {
          this.$message({ type: 'success', message: response.message })
          this.getAppointmentList()
        })
      })
    },
    openEditUI(id) {
      if (id === null || id === undefined) {
        this.addAppointmentForm = this.getDefaultAddAppointmentForm()
        this.addDialogVisible = true
        return
      }

      appointmentApi.getAppointmentById(id).then(response => {
        this.editAppointmentForm = Object.assign(this.getDefaultEditAppointmentForm(), {
          id: response.data.id,
          content: response.data.content,
          date: response.data.date
        })
        this.editDialogVisible = true
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
    handleSizeChange(pageSize) {
      this.searchModel.pageSize = pageSize
      this.getAppointmentList()
    },
    handleCurrentChange(pageNo) {
      this.searchModel.pageNo = pageNo
      this.getAppointmentList()
    }
  },
  created() {
    this.getAppointmentList()
    this.loadVipOptions()
  }
}
</script>

<style>
#search .el-input,
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
.el-dialog .el-date-editor {
  width: 85%;
}
</style>
