-- PR18：新增本地消息表（Outbox）与库存预警读模型表

-- 本地消息表：主事务内写入，独立 Relay 异步扫描并发 MQ，保证"主业务成功则事件不丢"
CREATE TABLE `event_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(64) NOT NULL COMMENT '事件类型，如 appointment.created / sales.created / inventory.changed',
  `exchange` varchar(128) NOT NULL COMMENT '目标交换机',
  `routing_key` varchar(128) NOT NULL COMMENT '目标路由键',
  `payload` text NOT NULL COMMENT '消息体 JSON',
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

-- 库存预警读模型表：由库存事件消费者驱动 upsert，支撑低库存查询
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
