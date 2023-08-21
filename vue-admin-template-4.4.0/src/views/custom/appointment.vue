<template>
    <div>
        <!--搜索栏-->
        <el-card id="search">
            <el-row>
                <el-col :span="20">
                    <el-input v-model="searchModel.name" placeholder="姓名" clearable></el-input>
                    <el-input v-model="searchModel.phone" placeholder="手机号" clearable></el-input>
                    <el-date-picker v-model="searchModel.date" type="datetime" format="yyyy-MM-dd HH:mm:ss"
                        @change="handleSearchDateChange" clearable></el-date-picker>
                    <el-button @click="getAppointmentList" type="primary" round icon="el-icon-search">查询</el-button>
                </el-col>
                <el-col :span="4" align="right">
                    <!--圆形按钮-->
                    <el-button @click="openEditUI(null)" type="primary" icon="el-icon-plus" circle></el-button>
                </el-col>
            </el-row>
        </el-card>

        <!--结果列表-->
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
                        <!--编辑按钮-->
                        <el-button @click="openEditUI(scope.row.id)" type="primary" icon="el-icon-edit"
                            size="mini"></el-button>
                        <!--删除按钮-->
                        <el-button @click="deleteAppointment(scope.row)" type="danger" icon="el-icon-delete"
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

        <!--预约信息新增对话框-->
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
                    <el-date-picker v-model="addAppointmentForm.date" type="datetime" format="yyyy-MM-dd HH:mm:ss"
                        @change="handleAddDateChange"></el-date-picker>
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button @click="addDialogVisible = false">取 消</el-button>
                <el-button type="primary" @click="addAppointment">确 定</el-button>
            </div>
        </el-dialog>

        <!--预约信息编辑对话框-->
        <el-dialog @close="clearForm" title="修改预约" :visible.sync="editDialogVisible">
            <el-form :model="editAppointmentForm" ref="editAppointmentFormRef" :rules="rules">
                <el-form-item label="内容" prop="content" :label-width="formLabelWidth">
                    <el-input v-model="editAppointmentForm.content" autocomplete="off"></el-input>
                </el-form-item>
                <el-form-item label="预约时间" prop="date" :label-width="formLabelWidth">
                    <el-date-picker v-model="editAppointmentForm.date" type="datetime" format="yyyy-MM-dd HH:mm:ss"
                        @change="handleDateChange"></el-date-picker>
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button @click="editDialogVisible = false">取 消</el-button>
                <el-button type="primary" @click="updateAppointment">确 定</el-button>
            </div>
        </el-dialog>

    </div>
</template>
    
<script>
import appointmentApi from '@/api/appointmentManage'
import { DatePicker } from "element-ui";
import moment from "moment";
export default {
    data() {
        return {
            formLabelWidth: '130px',
            addAppointmentForm: {},
            editAppointmentForm: {},
            addDialogVisible: false,
            editDialogVisible: false,
            title: "",
            total: 0,
            searchModel: {
                pageNo: 1,
                pageSize: 10
            },
            appointmentList: [],

            rules: {
                content: [
                    {
                        required: true,
                        message: '请输入内容',
                        trigger: 'blur'
                    },
                    {
                        min: 2,
                        max: 50,
                        message: '长度在2到50字符',
                        trigger: 'blur'
                    }
                ],
                phone: [
                    { required: false, message: '请输入手机号或会员卡号', trigger: 'blur' },
                    { pattern: /^1[3456789]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
                    { trigger: 'blur' }
                ],
                vid: [
                    { required: false, message: '请输入手机号或会员卡号', trigger: 'blur' },
                    { pattern: /^\d{11}$/, message: '会员卡号格式不正确', trigger: 'blur' },
                    { trigger: 'blur' }
                ],
            }
        }
    },
    methods: {
        handleSearchDateChange() {
            const formattedDate = moment(this.searchModel.date).format(
                "YYYY-MM-DD HH:mm:ss"
            );
            this.searchModel.date = formattedDate; // 将格式化后的日期时间值重新赋值给 searchModel.date
        },
        handleDateChange() {
            const formattedDate = moment(this.editAppointmentForm.date).format(
                "YYYY-MM-DD HH:mm:ss"
            );
            this.editAppointmentForm.date = formattedDate; // 将格式化后的日期时间值重新赋值给 editAppointmentForm.date
        },
        handleAddDateChange() {
            const formattedDate = moment(this.addAppointmentForm.date).format(
                "YYYY-MM-DD HH:mm:ss"
            );
            this.addAppointmentForm.date = formattedDate; // 将格式化后的日期时间值重新赋值给 editAppointmentForm.date
        },
        deleteAppointment(appointment) {
            this.$confirm(`您确认删除预约 ${appointment.name} 吗？`, '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                appointmentApi.deleteAppointmentById(appointment.id).then(response => {
                    this.$message({
                        type: 'success',
                        message: response.message
                    });
                    this.getAppointmentList();
                });
            }).catch(() => {
                this.$message({
                    type: 'info',
                    message: '已取消删除'
                });
            });
        },
        updateAppointment() {
            // 触发表单验证
            this.$refs.editAppointmentFormRef.validate((valid) => {
                if (valid) {
                    // 请求提交
                    appointmentApi.updateAppointment(this.editAppointmentForm).then(response => {
                        // 提交成功提示
                        this.$message({
                            message: response.message,
                            type: 'success'
                        });
                        // 关闭对话框
                        this.editDialogVisible = false;
                        // 刷新表格
                        this.getAppointmentList();
                    });
                } else {
                    console.log("提交错误！");
                    return false;
                }
            });
        },
        addAppointment() {
            const vid = this.addAppointmentForm.vid;
            const phone = this.addAppointmentForm.phone;
            const date = this.addAppointmentForm.date;
            const content = this.addAppointmentForm.content;

            // 触发表单验证
            this.$refs.addAppointmentFormRef.validate((valid) => {
                if (valid) {
                    // 请求提交
                    appointmentApi.addAppointment(vid, phone, date, content).then(response => {
                        // 提交成功提示
                        this.$message({
                            message: response.message,
                            type: 'success'
                        });
                        // 关闭对话框
                        this.addDialogVisible = false;
                        // 刷新表格
                        this.getAppointmentList();
                    });
                } else {
                    console.log("提交错误！");
                    return false;
                }
            })
        },

        clearForm() {
            this.addAppointmentForm = {};
            this.editAppointmentForm = {};
            this.$refs.addAppointmentFormRef.clearValidate();
            this.$refs.editAppointmentFormRef.clearValidate();
        },
        openEditUI(id) {
            if (id === null || id === undefined) {
                // 新增操作
                this.addDialogVisible = true;
            } else {
                // 编辑操作
                appointmentApi.getAppointmentById(id).then(response => {
                    this.editAppointmentForm = response.data;
                });
                this.editDialogVisible = true;
            }
        },
        handleSizeChange(pageSize) {
            this.searchModel.pageSize = pageSize;
            this.getAppointmentList();
        },
        handleCurrentChange(pageNo) {
            this.searchModel.pageNo = pageNo;
            this.getAppointmentList();
        },
        getAppointmentList() {
            appointmentApi.getAppointmentList(this.searchModel).then(response => {
                this.appointmentList = response.data.rows;
                this.total = response.data.total;
            });
        }
    },
    created() {
        this.getAppointmentList();
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