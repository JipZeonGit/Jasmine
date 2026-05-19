-- 站内信增加接收人维度，避免多账号之间共享未读状态
ALTER TABLE `site_message`
    ADD COLUMN `receiver_user_id` int DEFAULT NULL COMMENT '接收人用户 ID' AFTER `biz_id`;

ALTER TABLE `site_message`
    ADD KEY `idx_site_message_receiver_read_created` (`receiver_user_id`, `is_read`, `created_at`);
