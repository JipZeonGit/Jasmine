-- MySQL 8.4 hardening for IAM tables
DELETE ur FROM `user_role` ur LEFT JOIN `user` u ON ur.`user_id` = u.`id` WHERE u.`id` IS NULL;

ALTER TABLE `menu`
  MODIFY COLUMN `menu_id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `parent_id` int NULL DEFAULT NULL,
  MODIFY COLUMN `hidden` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `menu` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `role`
  MODIFY COLUMN `role_id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `role_name` varchar(50) NOT NULL,
  MODIFY COLUMN `role_desc` varchar(100) NULL DEFAULT NULL;
ALTER TABLE `role` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `role_menu`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `role_id` int NOT NULL,
  MODIFY COLUMN `menu_id` int NOT NULL;
ALTER TABLE `role_menu` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `user`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `status` tinyint NULL DEFAULT NULL,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `user` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `user_role`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `user_id` int NOT NULL,
  MODIFY COLUMN `role_id` int NOT NULL;
ALTER TABLE `user_role` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `auth_refresh_token`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `user_id` int NOT NULL,
  MODIFY COLUMN `revoked` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `auth_refresh_token` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- IAM indexes
SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'menu' AND index_name = 'idx_menu_parent_id'), 'DO 1', 'ALTER TABLE `menu` ADD KEY `idx_menu_parent_id` (`parent_id`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'menu' AND index_name = 'idx_menu_hidden'), 'DO 1', 'ALTER TABLE `menu` ADD KEY `idx_menu_hidden` (`hidden`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'role' AND index_name = 'idx_role_name'), 'DO 1', 'ALTER TABLE `role` ADD KEY `idx_role_name` (`role_name`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'role_menu' AND index_name = 'uk_role_menu_role_id_menu_id'), 'DO 1', 'ALTER TABLE `role_menu` ADD UNIQUE KEY `uk_role_menu_role_id_menu_id` (`role_id`, `menu_id`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'role_menu' AND index_name = 'idx_role_menu_menu_id'), 'DO 1', 'ALTER TABLE `role_menu` ADD KEY `idx_role_menu_menu_id` (`menu_id`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'uk_user_username'), 'DO 1', 'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_username` (`username`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'idx_user_phone'), 'DO 1', 'ALTER TABLE `user` ADD KEY `idx_user_phone` (`phone`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'idx_user_deleted_status'), 'DO 1', 'ALTER TABLE `user` ADD KEY `idx_user_deleted_status` (`deleted`, `status`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user_role' AND index_name = 'uk_user_role_user_id_role_id'), 'DO 1', 'ALTER TABLE `user_role` ADD UNIQUE KEY `uk_user_role_user_id_role_id` (`user_id`, `role_id`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user_role' AND index_name = 'idx_user_role_role_id'), 'DO 1', 'ALTER TABLE `user_role` ADD KEY `idx_user_role_role_id` (`role_id`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- auth_refresh_token index cleanup / rebuild
SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'auth_refresh_token' AND index_name = 'idx_auth_refresh_token_user_id'), 'ALTER TABLE `auth_refresh_token` DROP INDEX `idx_auth_refresh_token_user_id`', 'DO 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'auth_refresh_token' AND index_name = 'idx_auth_refresh_token_expires_at'), 'ALTER TABLE `auth_refresh_token` DROP INDEX `idx_auth_refresh_token_expires_at`', 'DO 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'auth_refresh_token' AND index_name = 'idx_auth_refresh_token_user_state'), 'DO 1', 'ALTER TABLE `auth_refresh_token` ADD KEY `idx_auth_refresh_token_user_state` (`user_id`, `revoked`, `expires_at`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
