# PR8：接口层契约收口

## 本轮目标

PR8 聚焦接口层规范化，不改数据库结构，也不重做业务模型。本轮重点是把主要控制器的输入输出边界收口成更稳定的 DTO / VO 形式，并补齐参数校验与轻量测试。

## 主要改动

### 1. 主要控制器请求改走 DTO

本轮已覆盖以下主要控制器：

- `UserController`
- `RoleController`
- `MenuController`
- `FlowerController`
- `InventoryController`
- `SalesController`
- `VipController`
- `AppointmentController`

其中新增、修改、分页查询等接口统一改为使用 DTO，避免继续默认直接收数据库实体对象。

### 2. 主要控制器响应改走 VO

本轮为用户、角色、菜单、花卉、库存、销售、会员、预约补齐了对应 VO。控制器对外返回的数据结构不再默认直接暴露 Entity，接口边界更清晰，也更利于后续业务重构。

### 3. 统一分页返回结构

列表接口统一改为返回 `TableData<T>`：

- `total`
- `rows`

不再继续在控制器里手工拼装 `Map<String, Object>`。

### 4. 补齐参数校验

新增 DTO 上统一补充了基础校验规则，例如：

- `@Valid`
- `@NotBlank`
- `@NotNull`
- `@Min`
- `@Max`

分页参数、关键新增/修改请求都开始按统一规则校验，参数错误时交给全局异常处理返回统一中文提示。

### 5. 收口 appointment 的接口边界

预约模块这轮额外处理了一个历史遗留问题：

- 控制器不再直接负责会员存在性查询
- `IAppointmentService` / `AppointmentServiceImpl` 不再混入 Web 层注解
- 预约创建逻辑由 service 返回是否创建成功，控制器只负责请求校验与结果转换

## 测试补充

本轮新增了两类轻量测试：

- DTO 校验测试
- 预约控制器边界测试

目的是确保：

- 分页和新增请求的约束规则真正生效
- `appointment` 这种需要控制器兜底的边界逻辑不会回退

## 当前效果

PR8 完成后，接口层的职责边界更明确：

- DTO：前端传什么
- VO：后端回什么
- Entity：数据库怎么存

这样后续继续推进日志、指标、数据库升级和业务模型重构时，接口层不会再和数据库表结构强耦合。
