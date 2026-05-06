package com.nfu.jasmine.infra.mq.message;

/**
 * 库存变更事件的来源与动作常量。
 * <p>
 * changeSource 标识谁触发的库存变更，changeAction 标识当前变更的操作类型，
 * 后面做库存预警或审计时就不需要靠 bizNo 猜来源。
 */
public final class InventoryChangeSource {
    private InventoryChangeSource() {
    }

    // 手工库存动作（采购入库、损耗出库、退货入库等）
    public static final String MANUAL_INVENTORY = "MANUAL_INVENTORY";
    // 销售单触发的库存变更
    public static final String SALES_ORDER = "SALES_ORDER";

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_ROLLBACK = "ROLLBACK";
}
