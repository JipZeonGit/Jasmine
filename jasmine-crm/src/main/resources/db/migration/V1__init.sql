-- jasmine_crm 数据库初始化脚本
-- 包含表：vip, appointment, site_message, event_outbox

-- ----------------------------
-- Table structure for vip
-- ----------------------------
CREATE TABLE `vip` (
  `id` int NOT NULL AUTO_INCREMENT,
  `vid` varchar(20) NOT NULL,
  `name` varchar(50) NOT NULL,
  `sex` varchar(2) DEFAULT NULL,
  `phone` varchar(20) NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_vip_vid` (`vid`) USING BTREE,
  UNIQUE KEY `uk_vip_phone` (`phone`) USING BTREE,
  KEY `idx_vip_deleted_name` (`deleted`, `name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

-- ----------------------------
-- Table structure for appointment
-- ----------------------------
CREATE TABLE `appointment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `vip_id` int NOT NULL,
  `date` datetime NOT NULL,
  `content` varchar(100) NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_appointment_deleted_date` (`deleted`, `date`) USING BTREE,
  KEY `idx_appointment_vip_id` (`vip_id`) USING BTREE,
  CONSTRAINT `fk_appointment_vip` FOREIGN KEY (`vip_id`) REFERENCES `vip` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

-- ----------------------------
-- Table structure for site_message
-- ----------------------------
CREATE TABLE `site_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型，如 APPOINTMENT_REMINDER',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '业务主键，用于消费端回查和前端跳转',
  `receiver_user_id` int DEFAULT NULL COMMENT '接收人用户 ID',
  `title` varchar(128) NOT NULL COMMENT '站内信标题',
  `content` varchar(512) NOT NULL COMMENT '站内信正文',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_site_message_is_read` (`is_read`),
  KEY `idx_site_message_biz_type` (`biz_type`),
  KEY `idx_site_message_receiver_read_created` (`receiver_user_id`, `is_read`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内通知消息表';

-- ----------------------------
-- Table structure for event_outbox
-- ----------------------------
CREATE TABLE `event_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(64) NOT NULL COMMENT '事件类型，如 appointment.created',
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
-- Default data: vip & appointment
-- ----------------------------
INSERT INTO `vip` (`id`, `vid`, `name`, `sex`, `phone`, `deleted`) VALUES
  (1, '10876678474', '管先生', '男', '13677778888', 0),
  (2, '10563933573', '黄女士', '女', '13788889999', 0),
  (3, '10139228001', '叶小姐', '女', '13799990001', 0);

INSERT INTO `appointment` (`id`, `vip_id`, `date`, `content`, `deleted`) VALUES
  (1, 1, '2026-04-12 10:00:00', '红玫瑰花束预订', 0),
  (2, 2, '2026-04-13 16:30:00', '向日葵到店自提', 0);
