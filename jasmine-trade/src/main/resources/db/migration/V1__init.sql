-- jasmine_trade 数据库初始化脚本
-- 包含表：sales, sales_item, inventory, inventory_alert, event_outbox

-- ----------------------------
-- Table structure for sales
-- ----------------------------
CREATE TABLE `sales` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL,
  `vip_id` int DEFAULT NULL,
  `date` datetime NOT NULL,
  `total_amount` decimal(12,2) NOT NULL DEFAULT 0,
  `remark` varchar(200) DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_sales_order_no` (`order_no`) USING BTREE,
  KEY `idx_sales_deleted_date` (`deleted`, `date`) USING BTREE,
  KEY `idx_sales_vip_id` (`vip_id`) USING BTREE,
  KEY `idx_sales_operator_id` (`operator_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

-- ----------------------------
-- Table structure for sales_item
-- ----------------------------
CREATE TABLE `sales_item` (
  `id` int NOT NULL AUTO_INCREMENT,
  `sales_id` int NOT NULL,
  `flower_id` int NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `unit_cost` decimal(10,2) DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `cost_amount` decimal(12,2) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sales_item_sales_id_deleted` (`sales_id`, `deleted`) USING BTREE,
  KEY `idx_sales_item_flower_id` (`flower_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

-- ----------------------------
-- Table structure for inventory
-- ----------------------------
CREATE TABLE `inventory` (
  `id` int NOT NULL AUTO_INCREMENT,
  `biz_no` varchar(32) NOT NULL,
  `flower_id` int NOT NULL,
  `biz_type` varchar(32) NOT NULL,
  `quantity` int NOT NULL,
  `before_stock` int NOT NULL,
  `after_stock` int NOT NULL,
  `unit_cost` decimal(10,2) DEFAULT NULL,
  `total_cost` decimal(12,2) DEFAULT NULL,
  `remark` varchar(200) DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `date` datetime NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_inventory_deleted_date` (`deleted`, `date`) USING BTREE,
  KEY `idx_inventory_flower_id` (`flower_id`) USING BTREE,
  KEY `idx_inventory_biz_type` (`biz_type`) USING BTREE,
  KEY `idx_inventory_biz_no` (`biz_no`) USING BTREE,
  KEY `idx_inventory_operator_id` (`operator_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

-- ----------------------------
-- Table structure for inventory_alert
-- ----------------------------
CREATE TABLE `inventory_alert` (
  `id` int NOT NULL AUTO_INCREMENT,
  `flower_id` int NOT NULL COMMENT '花卉主数据 ID',
  `flower_name_snapshot` varchar(64) NOT NULL COMMENT '花卉名称快照，避免后续改名导致预警信息不明确',
  `safe_stock` int NOT NULL COMMENT '安全库存阈值',
  `current_stock` int NOT NULL COMMENT '当前库存',
  `alert_status` varchar(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / LOW_STOCK',
  `last_trigger_time` datetime DEFAULT NULL COMMENT '最近一次触发低库存的时间',
  `last_recover_time` datetime DEFAULT NULL COMMENT '最近一次恢复正常的时间',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inventory_alert_flower_id` (`flower_id`),
  KEY `idx_inventory_alert_status` (`alert_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存预警读模型';

-- ----------------------------
-- Table structure for event_outbox
-- ----------------------------
CREATE TABLE `event_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(64) NOT NULL COMMENT '事件类型，如 sales.created / inventory.changed',
  `exchange` varchar(128) NOT NULL COMMENT '目标交换机',
  `routing_key` varchar(128) NOT NULL COMMENT '目标路由键',
  `payload` text NOT NULL COMMENT '消息体 JSON',
  `delay_ms` bigint DEFAULT NULL COMMENT '延迟投递毫秒数，NULL 表示即时投递',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SENT / FAILED',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次允许重试时间',
  `last_error` varchar(512) DEFAULT NULL COMMENT '最近一次发送失败原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '写入时间',
  `sent_at` datetime DEFAULT NULL COMMENT '发送成功时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_outbox_status_next_retry` (`status`, `next_retry_time`),
  KEY `idx_event_outbox_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='本地消息表（Outbox）';

-- ----------------------------
-- Default data: inventory
-- ----------------------------
INSERT INTO `inventory` (`id`, `biz_no`, `flower_id`, `biz_type`, `quantity`, `before_stock`, `after_stock`, `remark`, `operator_id`, `date`, `deleted`) VALUES
  (1, 'INV-INIT-ROSE', 1, 'PURCHASE_IN', 120, 0, 120, '初始化库存', 1, '2026-04-10 09:00:00', 0),
  (2, 'INV-INIT-SUN', 2, 'PURCHASE_IN', 80, 0, 80, '初始化库存', 1, '2026-04-10 09:05:00', 0),
  (3, 'INV-INIT-EUST', 3, 'PURCHASE_IN', 45, 0, 45, '初始化库存', 1, '2026-04-10 09:10:00', 0),
  (4, 'INV-INIT-WROSE', 4, 'PURCHASE_IN', 60, 0, 60, '初始化库存', 1, '2026-04-10 09:15:00', 0);
