CREATE TABLE IF NOT EXISTS `event_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
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
