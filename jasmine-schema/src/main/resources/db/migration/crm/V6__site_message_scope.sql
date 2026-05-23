SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'site_message' AND column_name = 'receiver_user_id'), 'SELECT 1', 'ALTER TABLE `site_message` ADD COLUMN `receiver_user_id` int DEFAULT NULL AFTER `biz_id`'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'site_message' AND index_name = 'idx_site_message_receiver_read_created'), 'DO 1', 'ALTER TABLE `site_message` ADD KEY `idx_site_message_receiver_read_created` (`receiver_user_id`, `is_read`, `created_at`)'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
