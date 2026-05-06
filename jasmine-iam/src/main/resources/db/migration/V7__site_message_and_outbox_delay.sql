-- PR19：站内信表 + Outbox 延时头支持

-- 站内信表：消费端解析延时唤醒消息后落库，供前端全局消息中心展示
CREATE TABLE `site_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型，如 APPOINTMENT_REMINDER',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '业务主键，用于消费端回查和前端跳转',
  `title` varchar(128) NOT NULL COMMENT '站内信标题',
  `content` varchar(512) NOT NULL COMMENT '站内信正文',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_site_message_is_read` (`is_read`),
  KEY `idx_site_message_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内通知消息表';

-- Outbox 表扩展：新增 delay_ms 列，支持延时消息场景
ALTER TABLE `event_outbox` ADD COLUMN `delay_ms` bigint DEFAULT NULL COMMENT '延迟投递毫秒数，NULL 表示即时投递' AFTER `payload`;
