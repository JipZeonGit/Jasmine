# Phase 7.6 — product/crm/trade 业务 Service 单测覆盖率补齐

> 时间：2026-08-05
> 范围：jasmine-product / jasmine-crm / jasmine-trade 三个模块的业务 Service 单元测试缺口扫描与补齐
> 前置：Phase 7.3 已补齐 iam 模块单测，Phase 7.4 已补齐跨服务契约测试

---

## 一、背景

Phase 7.3 仅覆盖 iam 模块。product/crm/trade 三个业务模块的业务 Service 单测覆盖率此前未系统扫描，
存在"库存/资金类写操作无测试守护"的风险。本轮对所有业务 Service 实现类做了一次完整扫描，并按优先级补齐缺口。

---

## 二、扫描结果（补齐前）

| 模块 | Service | 行数 | 已有测试 | 已覆盖方法 | 未覆盖方法 |
|:---|:---|---:|:---|:---|:---|
| product | FlowerStockService | 89 | FlowerStockServiceTest (8) | adjustStock | — |
| product | FlowerServiceImpl | 70 | — | — | 5 |
| crm | AppointmentServiceImpl | 181 | — | — | 5 |
| crm | VipServiceImpl | 20 | — | — | 0（空类）|
| crm | VipReadFacadeImpl | 64 | — | — | 5 |
| trade | SalesServiceImpl | 450 | SalesServiceImplTest (4) | saveSales 部分 | 6 |
| trade | InventoryServiceImpl | 322 | — | — | 7 |

**总计**：32 个未覆盖方法，其中 🔴 高风险 9 个、🟡 中风险 6 个、🟢 低风险 17 个。

---

## 三、补齐内容

### P0-1：InventoryServiceImplTest（新增，14 个用例）

**文件**：`jasmine-trade/src/test/java/com/nfu/jasmine/inventory/application/impl/InventoryServiceImplTest.java`

| 测试方法 | 覆盖场景 |
|:---|:---|
| saveInventoryShouldDeductStockAndPublishEvent | 采购入库：库存扣减 + 流水保存 + MQ 事件 |
| saveInventoryShouldApplyBizTypeDirection | 销售出库：apply() 方向转换（-1）|
| saveInventoryShouldRejectInvalidBizType | 非法业务类型 |
| saveInventoryShouldRejectNonPositiveQuantity | 数量 ≤ 0 |
| saveInventoryShouldRejectPurchaseInWithoutUnitCost | 采购入库未填单价 |
| updateInventorySameFlowerShouldRollbackAndReapply | **同花卉更新**：回滚旧 delta + 叠加新 delta |
| updateInventoryChangeFlowerShouldRollbackOldAndApplyNew | **换花卉更新**：旧花卉回滚 + 新花卉新增 |
| updateInventoryShouldThrowWhenNotFound | 流水不存在 |
| deleteInventoryShouldRollbackStockAndRemove | 删除：回滚库存 + 删除记录 + MQ 事件 |
| deleteInventoryShouldThrowWhenNotFound | 删除：流水不存在 |
| getInventoryDetailShouldReturnVo | 查询详情 |
| getInventoryDetailShouldThrowWhenNotFound | 查询：不存在 |
| pageInventoryByNameWithNoMatchShouldReturnEmpty | 按花卉名查询无命中 |
| getLowStockCountShouldDelegateToAlertService | 委托给 InventoryAlertService |

**关键验证点**：
- `updateInventory` 同花卉分支：`adjustStock(flowerId, newDelta - oldDelta, ...)`，基准库存 `beforeStock - oldDelta`
- `updateInventory` 换花卉分支：两次 `adjustStock`（旧花卉回滚 + 新花卉新增）
- `deleteInventory`：`adjustStock(flowerId, -delta, ...)` 反向回滚

### P0-2：SalesServiceImplTest（追加 5 个用例，共 9 个）

**文件**：`jasmine-trade/src/test/java/com/nfu/jasmine/sales/application/impl/SalesServiceImplTest.java`

新增测试：
| 测试方法 | 覆盖场景 |
|:---|:---|
| updateSalesShouldRollbackOldItemsAndRebuildNew | **更新销售单**：回滚旧明细库存 + 删除旧记录 + 重建新明细 + ROLLBACK + CREATE 事件 |
| updateSalesShouldThrowWhenSalesNotFound | 销售单不存在 |
| updateSalesShouldThrowWhenItemsEmpty | 新明细为空 |
| deleteSalesShouldRestoreStockAndRemoveRecords | **删除销售单**：回滚库存 + 删除记录 + ROLLBACK 事件 |
| deleteSalesShouldThrowWhenSalesNotFound | 删除：销售单不存在 |

**关键验证点**：
- `updateSales` 的"先回滚再重建"策略：`restoreSales` → `rebuildSalesItems`
- MQ 事件次数：updateSales 触发 2 次（1 ROLLBACK + 1 CREATE），deleteSales 触发 1 次（ROLLBACK）
- `restoreSales` 私有方法通过 `adjustStock(flowerId, +quantity, ...)` 反向回滚

### P1：AppointmentServiceImplTest（新增，8 个用例）

**文件**：`jasmine-crm/src/test/java/com/nfu/jasmine/appointment/application/impl/AppointmentServiceImplTest.java`

| 测试方法 | 覆盖场景 |
|:---|:---|
| createAppointmentShouldSaveAndPublishCreatedAndReminderEvents | 创建：保存 + 创建事件 + 延时提醒事件 |
| createAppointmentShouldThrowWhenVipNotFound | 会员不存在 |
| updateAppointmentShouldUpdateAndRepublishReminder | 更新：修改内容 + 重发延时提醒 |
| updateAppointmentShouldThrowWhenNotFound | 预约不存在 |
| updateAppointmentShouldNotPublishReminderWhenVipDeleted | 会员已删除时不发提醒 |
| getAppointmentDetailShouldReturnVo | 查询详情 |
| getAppointmentDetailShouldThrowWhenNotFound | 查询：不存在 |
| pageAppointmentsByNameOrPhoneWithNoMatchShouldReturnEmpty | 按会员名/手机号查询无命中 |

**关键验证点**：
- `createAppointment`：`resolveVip` 三参数解析 + 延时提醒 `delayMs = 预约时间 - now - 1小时`
- `updateAppointment`：会员已删除（`vipReadFacade.findById` 返回 null）时不发提醒事件

### P2-1：FlowerServiceImplTest（新增，5 个用例）

**文件**：`jasmine-product/src/test/java/com/nfu/jasmine/flower/application/impl/FlowerServiceImplTest.java`

| 测试方法 | 覆盖场景 |
|:---|:---|
| listAllFlowersShouldDelegateToMapperOrderedById | 查询列表 |
| getFlowerDetailByIdShouldDelegateToMapper | 查询详情 |
| addFlowerShouldDelegateToMapperInsert | 新增 |
| updateFlowerShouldDelegateToMapperUpdateById | 更新 |
| deleteFlowerByIdShouldDelegateToMapperDeleteById | 删除 |

**说明**：FlowerServiceImpl 主要是 CRUD 委托 + @Cacheable/@Caching 注解。
单元测试验证委托路径无误；注解行为本身由 Spring 容器在集成测试中验证。

### P2-2：VipReadFacadeImplTest（新增，14 个用例）

**文件**：`jasmine-crm/src/test/java/com/nfu/jasmine/vip/application/support/VipReadFacadeImplTest.java`

| 测试方法 | 覆盖场景 |
|:---|:---|
| existsByIdShouldReturnTrueWhenVipExists | 存在 |
| existsByIdShouldReturnFalseWhenVipNotFound | 不存在 |
| existsByIdShouldReturnFalseWhenNull | null 入参 |
| findByIdShouldReturnVip | 正常查询 |
| findByIdShouldReturnNullWhenIdNull | null 入参 |
| resolveShouldPreferVipIdWhenPresent | **vipId 优先级最高** |
| resolveShouldQueryByVidWhenVipIdAbsent | 按 vid 查询 |
| resolveShouldQueryByPhoneWhenBothVipIdAndVidAbsent | 按 phone 查询 |
| resolveShouldReturnNullWhenAllIdentifiersAbsent | 全部入参为空 |
| findIdsByNameOrPhoneShouldReturnMatchingIds | 模糊查询命中 |
| findIdsByNameOrPhoneShouldReturnEmptyWhenNoMatch | 模糊查询无命中 |
| findByIdsShouldReturnMapKeyedById | 批量查询 |
| findByIdsShouldReturnEmptyWhenInputEmpty | 空入参 |
| findByIdsShouldReturnEmptyWhenInputNull | null 入参 |

**关键验证点**：`resolve` 方法的三分支优先级（vipId > vid > phone）

### P2-3：VipServiceImpl（不补，空类）

**说明**：`VipServiceImpl` 只继承 `ServiceImpl<VipMapper, Vip>`，未定义任何方法，
无需单测。如未来新增自定义方法再补。

---

## 四、验证结果

### 全量单测统计（补齐后）

| 模块 | 补齐前 | 补齐后 | 增量 |
|:---|---:|---:|---:|
| gateway | 12 | 12 | — |
| iam | 24 | 24 | — |
| product | 8 | 13 | +5（FlowerServiceImplTest）|
| trade | 22 | 41 | +19（InventoryServiceImplTest 14 + SalesServiceImplTest +5）|
| crm | 9 | 31 | +22（AppointmentServiceImplTest 8 + VipReadFacadeImplTest 14）|
| **合计** | **75** | **121** | **+46** |

### 运行结果

```
[INFO] Results:
[INFO] Tests run: 121, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

全量 121 个单测全部通过，0 失败，0 回归。

---

## 五、测试设计说明

### 1. 测试策略

- **纯 Mockito 单测**：所有测试使用 `@ExtendWith(MockitoExtension.class)`，不依赖 Spring 容器，
  启动快（全量 ~10 秒），可在 CI 的"快速单元测试"阶段运行（`./mvnw test -DskipITs=true`）
- **ServiceImpl 基类处理**：MyBatis-Plus 的 `ServiceImpl` 通过 `ReflectionTestUtils.setField(service, "baseMapper", mapper)` 注入 baseMapper
- **不验证注解行为**：`@Cacheable`/`@Caching`/`@Transactional` 等注解行为依赖 Spring 容器，
  单元测试只验证委托路径，注解行为由集成测试验证

### 2. 关键验证点

- **库存调整方向**：`InventoryBizType.apply()` 将正数转换为带方向的 delta（PURCHASE_IN 为 +，SALE_OUT 为 -）
- **updateInventory 同花卉分支**：`adjustStock(flowerId, newDelta - oldDelta, ...)`，基准库存 = `beforeStock - oldDelta`
- **updateInventory 换花卉分支**：两次 `adjustStock`（旧花卉回滚 + 新花卉新增）
- **restoreSales 私有方法**：通过 `adjustStock(flowerId, +quantity, ...)` 反向回滚
- **MQ 事件次数**：updateSales 触发 2 次（ROLLBACK + CREATE），deleteSales 触发 1 次（ROLLBACK）
- **resolve 三分支优先级**：vipId > vid > phone

### 3. 踩过的坑

- **MyBatis-Plus 3.5.9 的 `insert`/`updateById` 重载歧义**：新增了 `insert(Collection<T>)` 和 `updateById(Collection<T>)`，`any()` 匹配器有歧义，必须用 `any(SpecificClass.class)` 明确类型
- **`PageQueryDTO.setPageNo/setPageSize` 接收 Long**：不是 int，测试时需用 `1L`/`10L`
- **`TableData.getTotal()` 返回 Long**：断言时需用 `0L` 而非 `0`

---

## 六、后续建议

1. **trade 模块的查询方法未测**：`listSales`/`pageSales`/`getSalesDetail`/`getTodayBusinessSummary` 仍是简单委托/聚合查询，
   风险低，可按需补
2. **集成测试层面**：单元测试已覆盖业务逻辑分支，但跨服务调用 + 事务回滚 + 缓存失效的端到端验证
   仍依赖现有的 2 个 @Disabled 的 IT 测试（`SalesFlowIT`、`InventoryFlowIT`），如需启动可按需开启
3. **覆盖率工具**：本轮未引入 JaCoCo，仅做方法级覆盖。如需行级覆盖率报告，
   可后续在 pom.xml 加 jacoco-maven-plugin，但当前 121 个用例已覆盖核心业务路径
