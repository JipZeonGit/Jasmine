<template>
    <div>
        <!--搜索栏-->
        <el-card id="search">
            <el-row>
                <el-col :span="20">
                    <el-input v-model="searchModel.vid" placeholder="会员卡号" clearable></el-input>
                    <el-input v-model="searchModel.name" placeholder="姓名" clearable></el-input>
                    <el-input v-model="searchModel.phone" placeholder="手机号" clearable></el-input>
                    <el-button @click="getVipList" type="primary" round icon="el-icon-search">查询</el-button>
                </el-col>
                <el-col :span="4" align="right">
                    <!--圆形按钮-->
                    <el-button @click="openEditUI(null)" type="primary" icon="el-icon-plus" circle></el-button>
                </el-col>
            </el-row>
        </el-card>

        <!--结果列表-->
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
                        <el-button @click="openEditUI(scope.row.id)" type="primary" icon="el-icon-edit"
                            size="mini"></el-button>
                        <el-button @click="deleteVip(scope.row)" type="danger" icon="el-icon-delete"
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

        <!--会员信息新增对话框-->
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
                <el-button @click="dialogFormVisible = false">取 消</el-button>
                <el-button type="primary" @click="saveVip">确 定</el-button>
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
            vipForm: [],
            dialogFormVisible: false,
            title: "",
            total: 0,
            searchModel: {
                pageNo: 1,
                pageSize: 10
            },
            vipList: [],
            vipForm: {
                sex: '' // 用于存储选择的性别
            },

            rules: {
                name: [
                    {
                        required: true,
                        message: '请输入姓名',
                        trigger: 'blur'
                    },
                    {
                        min: 1,
                        max: 10,
                        message: '长度在1到10字符',
                        trigger: 'blur'
                    }
                ],
            }
        }
    },
    methods: {
        deleteVip(vip) {
            this.$confirm(`您确认删除会员 ${vip.name} 吗？`, '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                vipApi.deleteVipById(vip.id).then(response => {
                    this.$message({
                        type: 'success',
                        message: response.message
                    });
                    this.getVipList();
                });
            }).catch(() => {
                this.$message({
                    type: 'info',
                    message: '已取消删除'
                });
            });
        },
        saveVip() {
            // 触发表单验证
            this.$refs.vipFormRef.validate((valid) => {
                if (valid) {
                    // 请求提交
                    vipApi.saveVip(this.vipForm).then(response => {
                        //提交成功提示
                        this.$message({
                            message: response.message,
                            type: 'success'
                        });
                        //关闭对话框
                        this.dialogFormVisible = false;
                        //刷新表格
                        this.getVipList();
                    });
                } else {
                    console.log("提交错误！");
                    return false;
                }
            });
        },
        clearForm() {
            this.vipForm = {};
            this.$refs.vipFormRef.clearValidate();
        },
        openEditUI(id) {
            if (id == null) {
                this.title = '新增会员';
            } else {
                this.title = '修改会员';
                // 根据id查询会员
                vipApi.getVipById(id).then(response => {
                    this.vipForm = response.data;
                });
            }
            this.dialogFormVisible = true;
        },
        handleSizeChange(pageSize) {
            this.searchModel.pageSize = pageSize;
            this.getVipList();
        },
        handleCurrentChange(pageNo) {
            this.searchModel.pageNo = pageNo;
            this.getVipList();
        },
        getVipList() {
            vipApi.getVipList(this.searchModel).then(response => {
                this.vipList = response.data.rows;
                this.total = response.data.total;
            });
        }
    },
    created() {
        this.getVipList();
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