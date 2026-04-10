# PR11 业务模型重构

## 目标

正式解决 legacy 业务模型的问题，把 Jasmine 从“手工录结果”改成“录业务动作、系统自动推导结果”。

本轮采用的前提是：

- 当前开发环境业务数据可丢弃
- 不再为旧业务表做重兼容
- 直接按新模型重建业务表

## 本轮重构范围

### 1. 花卉主数据重建

`flower` 表升级为真正的主数据：

- `name`
- `unit`
- `sale_price`
- `cost_price`
- `safe_stock`
- `current_stock`
- `status`

这让花卉不再只是“名字 + 单价 + 成本”的薄表。

### 2. 库存改为业务动作驱动

`inventory` 表不再记录“手填余量”，而是记录库存动作：

- `biz_no`
- `flower_id`
- `biz_type`
- `quantity`
- `before_stock`
- `after_stock`
- `unit_cost`
- `total_cost`
- `remark`
- `operator_id`
- `date`

支持的动作类型：

- `PURCHASE_IN`
- `SALE_OUT`
- `LOSS_OUT`
- `RETURN_IN`
- `CHECK_IN`
- `CHECK_OUT`

库存结果由系统维护，不再让页面手工填写 `residue`。
采购入库会记录真实进货单价和成本小计，后续统计不再依赖估算值。

### 3. 销售改为销售单 + 明细

销售模型拆成：

- `sales`
- `sales_item`

其中：

- `sales` 记录销售单头
- `sales_item` 记录销售明细
- `sales_item` 同时记录销售时刻的成本快照

创建销售单时，系统自动：

- 汇总总金额
- 扣减花卉库存
- 写入库存流水
- 冻结销售明细的成本单价与成本金额

### 4. 会员与预约改成真实关系

`appointment` 表改为使用 `vip_id` 关联会员，而不是保存一份松散的会员快照。

页面仍然支持按：

- 会员 ID
- 会员卡号
- 手机号

来定位会员，但底层保存的是稳定关系。

## 数据库策略

本轮使用新的 Flyway 脚本：

- `src/main/resources/db/migration/V4__business_model_rebuild.sql`
- `src/main/resources/db/migration/V5__cost_tracking_and_daily_summary.sql`

策略是：

- 保留系统、权限、认证表
- 直接重建业务表
- 不回改 `V1 ~ V3`

因为开发环境业务数据不重要，本轮不再尝试把旧的库存流水、旧销售金额日志完整迁移到新模型。

## 导库脚本

`Jasmine.sql` 已同步到 PR11 之后的最终开发库结构，适合：

- 手工初始化本地开发库
- 新环境快速导入

## 后端变化

核心后端变化包括：

- 新增业务编号工具
- 新增业务异常
- 新增库存业务类型枚举
- 新增今日经营统计 VO
- 重建 `Flower / Inventory / Sales / SalesItem / Appointment` 实体
- 重建库存、销售、预约的 DTO / VO / Service / Controller
- 销售与库存联动
- 预约与会员联动
- 补上采购成本与销售成本快照
- 补上“今日销售额 / 今日进货成本 / 今日毛利 / 今日净流入”统计能力

## 前端变化

前端涉及的核心页面已按新模型调整：

- `vue-admin-template-4.4.0/src/views/custom/flowerManage.vue`
- `vue-admin-template-4.4.0/src/views/custom/inventoryManage.vue`
- `vue-admin-template-4.4.0/src/views/custom/salesManage.vue`
- `vue-admin-template-4.4.0/src/views/custom/appointment.vue`

页面语义改成更接近门店业务动作：

- 库存登记动作
- 销售单 + 明细
- 预约绑定真实会员
- 花卉按主数据维护
- 销售页展示今日经营统计
- 采购入库录入真实进货单价

## 本地验证

后端验证使用固定 JDK：

`JDK 21.0.10`

已验证：

- `./mvnw.cmd -q test -DskipITs=true`
- `./mvnw.cmd -q verify -DskipUTs=true`
- `npm.cmd run build:prod`

说明：

- 前端整仓 `lint` 仍有大量 legacy 风格噪声，本轮不以清空旧 lint 为目标
- 本轮的验收重点是新业务页面、API、数据库模型是否真正可编译、可联动、可落库

## 风险与后续

### 已接受的取舍

- 旧业务开发数据不保留
- 不做复杂历史兼容迁移
- 不在本轮引入采购系统、财务系统、多门店模型

### 后续建议

- 合并 PR11 后重置本地开发库
- 用新的 `Jasmine.sql` 或 Flyway 重建数据库
- 做一轮真实手工联调：
  - 花卉新增
  - 库存登记
  - 销售建单
  - 库存自动扣减
  - 预约绑定会员