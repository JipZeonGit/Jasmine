<template>
    <div>
        <!--搜索栏-->
        <el-card id="search">
            <el-row>
                <el-col :span="20">
                    <el-input v-model="searchModel.name" placeholder="花名" clearable></el-input>
                    <el-button @click="getFlowerList" type="primary" round icon="el-icon-search">查询</el-button>
                </el-col>
                <el-col :span="4" align="right">
                    <!--圆形按钮-->
                    <el-button @click="openEditUI(null)" type="primary" icon="el-icon-plus" circle></el-button>
                </el-col>
            </el-row>
        </el-card>

        <!--结果列表-->
        <el-card>
            <el-table :data="flowerList" stripe style="width: 100%">
                <el-table-column type="index" label="#" width="80">
                    <template slot-scope="scope">
                        {{ (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1 }}
                    </template>
                </el-table-column>
                <el-table-column prop="id" label="花卉ID" width="180"></el-table-column>
                <el-table-column prop="name" label="花名" width="180"></el-table-column>
                <el-table-column prop="unitprice" label="单价（元）" width="180"></el-table-column>
                <el-table-column prop="costs" label="成本（元）"></el-table-column>
                <el-table-column label="操作">
                    <template slot-scope="scope">
                        <el-button @click="openEditUI(scope.row.id)" type="primary" icon="el-icon-edit"
                            size="mini"></el-button>
                        <el-button @click="deleteFlower(scope.row)" type="danger" icon="el-icon-delete"
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

        <!--花卉信息新增对话框-->
        <el-dialog @close="clearForm" :title="title" :visible.sync="dialogFormVisible">
            <el-form :model="flowerForm" ref="flowerFormRef" :rules="rules">
                <el-form-item label="花名" prop="name" :label-width="formLabelWidth">
                    <el-input v-model="flowerForm.name" autocomplete="off"></el-input>
                </el-form-item>
                <el-form-item label="单价（元）" prop="unitprice" :label-width="formLabelWidth">
                    <el-input v-model="flowerForm.unitprice" autocomplete="off"></el-input>
                </el-form-item>
                <el-form-item label="成本（元）" prop="costs" :label-width="formLabelWidth">
                    <el-input v-model="flowerForm.costs" autocomplete="off"></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button @click="dialogFormVisible = false">取 消</el-button>
                <el-button type="primary" @click="saveFlower">确 定</el-button>
            </div>
        </el-dialog>

    </div>
</template>
    
<script>
import flowerApi from '@/api/flowerManage'
export default {
    data() {
        var checkMoney = (rule,value,callback) => {
            var reg = /^\d+(\.\d{1,2})?$/
            if(!reg.test(value)){
                return callback(new Error('价格格式错误'));
            }
            callback();
        };
        return {
            formLabelWidth: '130px',
            flowerForm: [],
            dialogFormVisible: false,
            title: "",
            total: 0,
            searchModel: {
                pageNo: 1,
                pageSize: 10
            },
            flowerList: [],

            rules: {
                name: [
                    {
                        required: true,
                        message: '请输入花名',
                        trigger: 'blur'
                    },
                    {
                        min: 2,
                        max: 50,
                        message: '长度在2到50字符',
                        trigger: 'blur'
                    }
                ],
                unitprice: [
                    {
                        required: true,
                        message: '请输入单价',
                        trigger: 'blur'
                    },
                    {
                        min: 1,
                        max: 10,
                        validator: checkMoney,
                        message: '长度在1到10字符（精确到小数点后2位）',
                        trigger: 'blur'
                    }
                ],
                costs: [
                    {
                        required: true,
                        message: '请输入单价',
                        trigger: 'blur'
                    },
                    {
                        min: 1,
                        max: 10,
                        validator: checkMoney,
                        message: '长度在1到10字符（精确到小数点后2位）',
                        trigger: 'blur'
                    }
                ]
            }
        }
    },
    methods: {
        deleteFlower(flower) {
            this.$confirm(`您确认删除花卉 ${flower.name} 吗？`, '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                flowerApi.deleteFlowerById(flower.id).then(response => {
                    this.$message({
                        type: 'success',
                        message: response.message
                    });
                    this.getFlowerList();
                });
            }).catch(() => {
                this.$message({
                    type: 'info',
                    message: '已取消删除'
                });
            });
        },
        saveFlower() {
            // 触发表单验证
            this.$refs.flowerFormRef.validate((valid) => {
                if (valid) {
                    // 请求提交
                    flowerApi.saveFlower(this.flowerForm).then(response => {
                        //提交成功提示
                        this.$message({
                            message: response.message,
                            type: 'success'
                        });
                        //关闭对话框
                        this.dialogFormVisible = false;
                        //刷新表格
                        this.getFlowerList();
                    });
                } else {
                    console.log("提交错误！");
                    return false;
                }
            });
        },
        clearForm() {
            this.flowerForm = {};
            this.$refs.flowerFormRef.clearValidate();
        },
        openEditUI(id) {
            if(id == null){
                this.title = '新增花卉';
            }else{
                this.title = '修改花卉';
                // 根据id查询花卉
                flowerApi.getFlowerById(id).then(response => {
                    this.flowerForm = response.data;
                });
            }
            this.dialogFormVisible = true;
        },
        handleSizeChange(pageSize) {
            this.searchModel.pageSize = pageSize;
            this.getFlowerList();
        },
        handleCurrentChange(pageNo) {
            this.searchModel.pageNo = pageNo;
            this.getFlowerList();
        },
        getFlowerList() {
            flowerApi.getFlowerList(this.searchModel).then(response => {
                this.flowerList = response.data.rows;
                this.total = response.data.total;
            });
        }
    },
    created() {
        this.getFlowerList();
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