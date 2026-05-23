CREATE TABLE IF NOT EXISTS `site_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '业务主键',
  `title` varchar(128) NOT NULL COMMENT '站内信标题',
  `content` varchar(512) NOT NULL COMMENT '站内信正文',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_site_message_is_read` (`is_read`),
  KEY `idx_site_message_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内通知消息表';

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'event_outbox' AND column_name = 'delay_ms'), 'SELECT 1', 'ALTER TABLE `event_outbox` ADD COLUMN `delay_ms` bigint DEFAULT NULL AFTER `payload`'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
