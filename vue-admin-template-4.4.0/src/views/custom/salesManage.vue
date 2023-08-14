<template>
    <div>
        <!--搜索栏-->
        <el-card id="search">
            <el-row>
                <el-col :span="20">
                    <el-date-picker v-model="searchModel.date" type="datetime" format="yyyy-MM-dd HH:mm:ss"
                        @change="handleSearchDateChange" clearable></el-date-picker>
                    <el-button @click="getSalesList" type="primary" round icon="el-icon-search">查询</el-button>
                </el-col>
                <el-col :span="4" align="right">
                    <!--圆形按钮-->
                    <el-button @click="openEditUI(null)" type="primary" icon="el-icon-plus" circle></el-button>
                </el-col>
            </el-row>
        </el-card>

        <!--结果列表-->
        <el-card>
            <el-table :data="salesList" stripe style="width: 100%">
                <el-table-column type="index" label="#" width="80">
                    <template slot-scope="scope">
                        {{
                            (searchModel.pageNo - 1) * searchModel.pageSize + scope.$index + 1
                        }}
                    </template>
                </el-table-column>
                <el-table-column prop="id" label="销售ID" width="180"></el-table-column>
                <el-table-column prop="date" label="统计时间（年-月-日 时:分:秒）" width="240"></el-table-column>
                <el-table-column prop="money" label="营业额（元）">
                    <template slot-scope="scope">
                        <strong>
                            <el-tag v-if="scope.row.money > 0" style="font-size: 15px;">+&nbsp;<span>{{ scope.row.money }}</span></el-tag>
                            <el-tag v-if="scope.row.money < 0" type="danger" style="font-size: 15px;">-&nbsp;<span>{{ Math.abs(scope.row.money)
                            }}</span></el-tag>
                        </strong>
                    </template>
                </el-table-column>
                <el-table-column label="操作">
                    <template slot-scope="scope">
                        <el-button @click="openEditUI(scope.row.id)" type="primary" icon="el-icon-edit"
                            size="mini"></el-button>
                        <el-button @click="deleteSales(scope.row)" type="danger" icon="el-icon-delete"
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

        <!--销售信息新增对话框-->
        <el-dialog @close="clearForm" :title="title" :visible.sync="dialogFormVisible">
            <el-form :model="salesForm" ref="salesFormRef" :rules="rules">
                <el-form-item label="营业额（元）" prop="money" :label-width="formLabelWidth">
                    <el-input v-model="salesForm.money" autocomplete="off"></el-input>
                </el-form-item>
                <el-form-item label="统计时间" prop="date" :label-width="formLabelWidth">
                    <el-date-picker v-model="salesForm.date" type="datetime" format="yyyy-MM-dd HH:mm:ss"
                        @change="handleDateChange"></el-date-picker>
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button @click="dialogFormVisible = false">取 消</el-button>
                <el-button type="primary" @click="saveSales">确 定</el-button>
            </div>
        </el-dialog>
    </div>
</template>
    
<script>
import salesApi from "@/api/salesManage";
import { DatePicker } from "element-ui";
import moment from "moment";
export default {
    data() {
        return {
            formLabelWidth: "130px",
            salesForm: [],
            dialogFormVisible: false,
            title: "",
            total: 0,
            searchModel: {
                date: "",
                pageNo: 1,
                pageSize: 10,
            },
            salesList: [],
            salesForm: {
                date: "",
            },

            rules: {
                money: [
                    {
                        required: true,
                        message: "请输入营业额",
                        trigger: "blur",
                    },
                    {
                        min: 1,
                        max: 10,
                        pattern: /^-?\d{1,10}(\.\d{1,2})?$/,
                        message: "长度在1到10字符（精确到小数点后2位）",
                        trigger: "blur",
                    },
                ],
            },
        };
    },
    methods: {
        handleSearchDateChange() {
            const formattedDate = moment(this.searchModel.date).format(
                "YYYY-MM-DD HH:mm:ss"
            );
            this.searchModel.date = formattedDate; // 将格式化后的日期时间值重新赋值给 searchModel.date
        },
        handleDateChange() {
            const formattedDate = moment(this.salesForm.date).format(
                "YYYY-MM-DD HH:mm:ss"
            );
            this.salesForm.date = formattedDate; // 将格式化后的日期时间值重新赋值给 salesForm.date
        },
        deleteSales(sales) {
            this.$confirm(`您确认删除销售记录 ${sales.date} 吗？`, "提示", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning",
            })
                .then(() => {
                    salesApi.deleteSalesById(sales.id).then((response) => {
                        this.$message({
                            type: "success",
                            message: response.message,
                        });
                        this.getSalesList();
                    });
                })
                .catch(() => {
                    this.$message({
                        type: "info",
                        message: "已取消删除",
                    });
                });
        },
        saveSales() {
            // 触发表单验证
            this.$refs.salesFormRef.validate((valid) => {
                if (valid) {
                    // 请求提交
                    salesApi.saveSales(this.salesForm).then((response) => {
                        //提交成功提示
                        this.$message({
                            message: response.message,
                            type: "success",
                        });
                        //关闭对话框
                        this.dialogFormVisible = false;
                        //刷新表格
                        this.getSalesList();
                    });
                } else {
                    console.log("提交错误！");
                    return false;
                }
            });
        },
        clearForm() {
            this.salesForm = {};
            this.$refs.salesFormRef.clearValidate();
        },
        openEditUI(id) {
            if (id == null) {
                this.title = "新增花卉";
            } else {
                this.title = "修改花卉";
                // 根据id查询花卉
                salesApi.getSalesById(id).then((response) => {
                    this.salesForm = response.data;
                });
            }
            this.dialogFormVisible = true;
        },
        handleSizeChange(pageSize) {
            this.searchModel.pageSize = pageSize;
            this.getSalesList();
        },
        handleCurrentChange(pageNo) {
            this.searchModel.pageNo = pageNo;
            this.getSalesList();
        },
        getSalesList() {
            salesApi.getSalesList(this.searchModel).then((response) => {
                this.salesList = response.data.rows;
                this.total = response.data.total;
            });
        },
    },
    created() {
        this.getSalesList();
    },
    components: {
        "el-date-picker": DatePicker,
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