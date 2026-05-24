-- Trade 也需要本地消息表（销售/库存事件写入 Outbox）
CREATE TABLE IF NOT EXISTS event_outbox (
  id bigint NOT NULL AUTO_INCREMENT,
  event_type varchar(64) NOT NULL,
  exchange varchar(128) NOT NULL,
  routing_key varchar(128) NOT NULL,
  payload text NOT NULL,
  delay_ms bigint DEFAULT NULL,
  status varchar(16) NOT NULL DEFAULT 'PENDING',
  retry_count int NOT NULL DEFAULT 0,
  next_retry_time datetime DEFAULT NULL,
  last_error varchar(512) DEFAULT NULL,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at datetime DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_event_outbox_status_next_retry (status, next_retry_time),
  KEY idx_event_outbox_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
