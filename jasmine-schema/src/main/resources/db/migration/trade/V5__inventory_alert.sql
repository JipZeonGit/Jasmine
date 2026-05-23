CREATE TABLE IF NOT EXISTS `inventory_alert` (
  `id` int NOT NULL AUTO_INCREMENT,
  `flower_id` int NOT NULL COMMENT '花卉主数据 ID',
  `flower_name_snapshot` varchar(64) NOT NULL COMMENT '花卉名称快照',
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
