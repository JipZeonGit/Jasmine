<template>
  <div class="app-container">
    <el-card class="box-card" shadow="never">
      <div slot="header" class="clearfix">
        <span>个人信息</span>
      </div>
      <div class="user-profile">
        <div class="user-info">
          <p><strong>用户名：</strong> {{ name }}</p>
          <p><strong>手机号：</strong> {{ phone || '暂无手机号' }}</p>
          <p><strong>电子邮件：</strong> {{ email || '暂无邮箱' }}</p>
          <p><strong>用户状态：</strong> 
            <el-tag :type="status === 1 ? 'success' : 'danger'">
              {{ status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </p>
          <p><strong>用户角色：</strong>
            <el-tag v-for="role in roles" :key="role" style="margin-right: 5px;">
              {{ role }}
            </el-tag>
          </p>
        </div>
      </div>
    </el-card>

    <el-card class="box-card form-card" shadow="never">
      <div slot="header" class="clearfix">
        <span>修改密码</span>
      </div>
      <el-form ref="passwordForm" :model="passwordForm" :rules="passwordRules" label-width="100px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submitForm('passwordForm')">提交</el-button>
          <el-button @click="resetForm('passwordForm')">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { changePassword } from '@/api/user'

export default {
  name: 'Profile',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的新密码不一致!'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入旧密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters([
      'name',
      'phone',
      'email',
      'status',
      'roles'
    ])
  },
  methods: {
    submitForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.loading = true
          changePassword({
            username: this.name,
            oldPassword: this.passwordForm.oldPassword,
            newPassword: this.passwordForm.newPassword
          }).then(response => {
            this.loading = false
            this.$message({
              message: '密码修改成功，请重新登录！',
              type: 'success'
            })
            // 修改密码成功后自动登出
            this.logout()
          }).catch(error => {
            this.loading = false
          })
        } else {
          return false
        }
      })
    },
    resetForm(formName) {
      this.$refs[formName].resetFields()
    },
    async logout() {
      await this.$store.dispatch('user/logout')
      this.$router.push(`/login`)
    }
  }
}
</script>

<style scoped>
.box-card {
  margin-bottom: 20px;
}
.user-profile {
  font-size: 14px;
}
.user-info p {
  line-height: 2;
  border-bottom: 1px solid #f0f2f5;
  padding-bottom: 10px;
}
.form-card {
  max-width: 600px;
}
</style>
