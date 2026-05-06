-- PR10: MySQL 8.4 正式收口
-- 说明：
-- 1. 将历史 utf8/utf8mb3 表统一迁移到 utf8mb4_0900_ai_ci。
-- 2. 清理 display width 等 5.7 时代遗留定义。
-- 3. 补充当前代码路径实际会用到的基础索引与唯一约束。
-- 4. 清理导库脚本遗留的 user_role 脏数据，避免结构继续漂移。

DELETE ur
FROM `user_role` ur
LEFT JOIN `user` u ON ur.`user_id` = u.`id`
WHERE u.`id` IS NULL;

ALTER TABLE `appointment`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `appointment` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `flower`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `flower` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `inventory`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `quantity` int NULL DEFAULT NULL,
  MODIFY COLUMN `residue` int NULL DEFAULT NULL,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `inventory` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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

ALTER TABLE `sales`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `sales` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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

ALTER TABLE `vip`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `vip` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `auth_refresh_token`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `user_id` int NOT NULL,
  MODIFY COLUMN `revoked` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `auth_refresh_token` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'appointment' AND index_name = 'idx_appointment_deleted_date'),
        'DO 1',
        'ALTER TABLE `appointment` ADD KEY `idx_appointment_deleted_date` (`deleted`, `date`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'appointment' AND index_name = 'idx_appointment_vid'),
        'DO 1',
        'ALTER TABLE `appointment` ADD KEY `idx_appointment_vid` (`vid`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'appointment' AND index_name = 'idx_appointment_phone'),
        'DO 1',
        'ALTER TABLE `appointment` ADD KEY `idx_appointment_phone` (`phone`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'flower' AND index_name = 'idx_flower_deleted_name'),
        'DO 1',
        'ALTER TABLE `flower` ADD KEY `idx_flower_deleted_name` (`deleted`, `name`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inventory' AND index_name = 'uk_inventory_num'),
        'DO 1',
        'ALTER TABLE `inventory` ADD UNIQUE KEY `uk_inventory_num` (`num`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inventory' AND index_name = 'idx_inventory_deleted_name'),
        'DO 1',
        'ALTER TABLE `inventory` ADD KEY `idx_inventory_deleted_name` (`deleted`, `name`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inventory' AND index_name = 'idx_inventory_date'),
        'DO 1',
        'ALTER TABLE `inventory` ADD KEY `idx_inventory_date` (`date`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'menu' AND index_name = 'idx_menu_parent_id'),
        'DO 1',
        'ALTER TABLE `menu` ADD KEY `idx_menu_parent_id` (`parent_id`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'menu' AND index_name = 'idx_menu_hidden'),
        'DO 1',
        'ALTER TABLE `menu` ADD KEY `idx_menu_hidden` (`hidden`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'role' AND index_name = 'idx_role_name'),
        'DO 1',
        'ALTER TABLE `role` ADD KEY `idx_role_name` (`role_name`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'role_menu' AND index_name = 'uk_role_menu_role_id_menu_id'),
        'DO 1',
        'ALTER TABLE `role_menu` ADD UNIQUE KEY `uk_role_menu_role_id_menu_id` (`role_id`, `menu_id`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'role_menu' AND index_name = 'idx_role_menu_menu_id'),
        'DO 1',
        'ALTER TABLE `role_menu` ADD KEY `idx_role_menu_menu_id` (`menu_id`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sales' AND index_name = 'idx_sales_deleted_date'),
        'DO 1',
        'ALTER TABLE `sales` ADD KEY `idx_sales_deleted_date` (`deleted`, `date`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'uk_user_username'),
        'DO 1',
        'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_username` (`username`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'idx_user_phone'),
        'DO 1',
        'ALTER TABLE `user` ADD KEY `idx_user_phone` (`phone`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'idx_user_deleted_status'),
        'DO 1',
        'ALTER TABLE `user` ADD KEY `idx_user_deleted_status` (`deleted`, `status`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user_role' AND index_name = 'uk_user_role_user_id_role_id'),
        'DO 1',
        'ALTER TABLE `user_role` ADD UNIQUE KEY `uk_user_role_user_id_role_id` (`user_id`, `role_id`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user_role' AND index_name = 'idx_user_role_role_id'),
        'DO 1',
        'ALTER TABLE `user_role` ADD KEY `idx_user_role_role_id` (`role_id`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'vip' AND index_name = 'uk_vip_vid'),
        'DO 1',
        'ALTER TABLE `vip` ADD UNIQUE KEY `uk_vip_vid` (`vid`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'vip' AND index_name = 'uk_vip_phone'),
        'DO 1',
        'ALTER TABLE `vip` ADD UNIQUE KEY `uk_vip_phone` (`phone`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'vip' AND index_name = 'idx_vip_deleted_name'),
        'DO 1',
        'ALTER TABLE `vip` ADD KEY `idx_vip_deleted_name` (`deleted`, `name`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'auth_refresh_token' AND index_name = 'idx_auth_refresh_token_user_id'),
        'ALTER TABLE `auth_refresh_token` DROP INDEX `idx_auth_refresh_token_user_id`',
        'DO 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'auth_refresh_token' AND index_name = 'idx_auth_refresh_token_expires_at'),
        'ALTER TABLE `auth_refresh_token` DROP INDEX `idx_auth_refresh_token_expires_at`',
        'DO 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'auth_refresh_token' AND index_name = 'idx_auth_refresh_token_user_state'),
        'DO 1',
        'ALTER TABLE `auth_refresh_token` ADD KEY `idx_auth_refresh_token_user_state` (`user_id`, `revoked`, `expires_at`) USING BTREE'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;