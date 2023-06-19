<template>
    <div>
        <!--搜索栏-->
        <el-card id="search">
            <el-row>
                <el-col :span="20">
                    <el-input v-model="searchModel.username" placeholder="用户名" clearable></el-input>
                    <el-input v-model="searchModel.phone" placeholder="手机号" clearable></el-input>
                    <el-button @click="getUserList" type="primary" round icon="el-icon-search">查询</el-button>
                </el-col>
                <el-col :span="4" align="right">
                    <!--圆形按钮-->
                    <el-button @click="openEditUI(null)" type="primary" icon="el-icon-plus" circle></el-button>
                </el-col>
            </el-row>
        </el-card>

        <!--查询结果列表-->
        <el-card>
            <el-table :data="userList" stripe style="width: 100%">
                <el-table-column label="#" width="80">
                    <template slot-scope="scope">
                        <!--计算编号-->
                        {{
                            (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1
                        }}
                    </template>
                </el-table-column>
                <el-table-column prop="id" label="用户ID" width="180">
                </el-table-column>
                <el-table-column prop="username" label="用户名" width="180">
                </el-table-column>
                <el-table-column prop="phone" label="手机号" width="180">
                </el-table-column>
                <el-table-column prop="status" label="用户状态" width="180">
                    <template slot-scope="scope">
                        <el-tag v-if="scope.row.status == 1">正常</el-tag>
                        <el-tag v-if="scope.row.status == 0" type="danger">禁用</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="email" label="电子邮件"> </el-table-column>
                <el-table-column label="操作" width="180">
                    <template slot-scope="scope">
                        <el-button @click="openEditUI(scope.row.id)" type="primary" icon="el-icon-edit"
                            size="mini"></el-button>
                        <el-button @click="deleteUser(scope.row)" type="danger" icon="el-icon-delete"
                            size="mini"></el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
        <!--分页组件-->
        <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
            :current-page="searchModel.pageNo" :page-sizes="[10, 25, 50, 100, 250, 500]" :page-size="searchModel.pageSize"
            layout="total, sizes, prev, pager, next, jumper" :total="total">
        </el-pagination>

        <!--用户信息编辑对话框-->
        <el-dialog @close="clearForm" :title="title" :visible.sync="dialogFormVisible">
            <el-form :model="userForm" ref="userFormRef" :rules="rules">
                <el-form-item label="用户名" prop="username" :label-width="formLabelWidth">
                    <el-input v-model="userForm.username" autocomplete="off"></el-input>
                </el-form-item>
                <el-form-item v-if="userForm.id == null || userForm.id == undefined" label="密码" prop="password"
                    :label-width="formLabelWidth">
                    <el-input type="password" v-model="userForm.password" autocomplete="off"></el-input>
                </el-form-item>
                <el-form-item label="手机号" :label-width="formLabelWidth">
                    <el-input v-model="userForm.phone" autocomplete="off"></el-input>
                </el-form-item>
                <el-form-item label="用户状态" :label-width="formLabelWidth">
                    <el-switch v-model="userForm.status" :active-value="1" :inactive-value="0">
                    </el-switch>
                </el-form-item>
                <el-form-item label="电子邮件" prop="email" :label-width="formLabelWidth">
                    <el-input v-model="userForm.email" autocomplete="off"></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button @click="dialogFormVisible = false">取 消</el-button>
                <el-button type="primary" @click="saveUser">确 定</el-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import userApi from "@/api/userManage";
import { call } from "body-parser";
export default {
    data() {
        var checkEmail = (rule, value, callback) => {
            var reg = /^[a-zA-Z0-9]+([-_.][a-zA-Z0-9]+)*@[a-zA-Z0-9]+([-_.][a-zA-Z0-9]+)*\.[a-z]{2,}$/
            if (!reg.test(value)) {
                return callback(new Error('电子邮箱格式错误'));
            }
            callback();
        };
        return {
            formLabelWidth: "130px",
            userForm: {},
            dialogFormVisible: false,
            title: "",
            total: 0,
            searchModel: {
                pageNo: 1,
                pageSize: 10,
            },
            userList: [],
            rules: {
                username: [
                    {
                        required: true,
                        message: '请输入用户名',
                        trigger: 'blur'
                    },
                    {
                        min: 3,
                        max: 50,
                        message: '长度在3到50字符',
                        trigger: 'blur'
                    }
                ],
                password: [
                    {
                        required: true,
                        message: '请输入初始密码',
                        trigger: 'blur'
                    },
                    {
                        min: 6,
                        max: 16,
                        message: '长度在6到16字符',
                        trigger: 'blur'
                    }
                ],
                email: [
                    {
                        required: true,
                        message: '请输入电子邮箱',
                        trigger: 'blur'
                    },
                    {
                        validator: checkEmail,
                        trigger: 'blur'
                    }
                ]
            }
        };
    },
    methods: {
        deleteUser(user) {
            this.$confirm(`您确认删除用户 ${user.username} 吗？`, '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                userApi.deleteUserById(user.id).then(response => {
                    this.$message({
                        type: 'success',
                        message: response.message
                    });
                    this.getUserList();
                });
            }).catch(() => {
                this.$message({
                    type: 'info',
                    message: '已取消删除'
                });
            });
        },
        saveUser() {
            //触发表单
            this.$refs.userFormRef.validate((valid) => {
                if (valid) {
                    //提交请求给后端
                    userApi.saveUser(this.userForm).then(response => {
                        //提交成功提示
                        this.$message({
                            message: response.message,
                            type: 'success'
                        });
                        //关闭对话框
                        this.dialogFormVisible = false;
                        //刷新表格
                        this.getUserList();
                    });
                } else {
                    console.log("提交错误！");
                    return false;
                }
            });
        },
        clearForm() {
            this.userForm = {};
            this.$refs.userFormRef.clearValidate();
        },
        openEditUI(id) {
            if (id == null) {
                this.title = "新增用户";
            }
            else {
                this.title = "修改用户"
                //根据ID查询用户数据
                userApi.getUserById(id).then(response => {
                    this.userForm = response.data;
                });
            }
            this.dialogFormVisible = true;
        },
        handleSizeChange(pageSize) {
            this.searchModel.pageSize = pageSize;
            this.getUserList();
        },
        handleCurrentChange(pageNo) {
            this.searchModel.pageNo = pageNo;
            this.getUserList();
        },
        getUserList() {
            userApi.getUserList(this.searchModel).then((response) => {
                this.userList = response.data.rows;
                this.total = response.data.total;
            });
        },
    },
    created() {
        this.getUserList();
    },
};
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