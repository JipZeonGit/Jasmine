ALTER TABLE `appointment`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `appointment` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `vip`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `vip` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'appointment' AND index_name = 'idx_appointment_deleted_date'), 'DO 1', 'ALTER TABLE `appointment` ADD KEY `idx_appointment_deleted_date` (`deleted`, `date`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'appointment' AND index_name = 'idx_appointment_vid'), 'DO 1', 'ALTER TABLE `appointment` ADD KEY `idx_appointment_vid` (`vid`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'appointment' AND index_name = 'idx_appointment_phone'), 'DO 1', 'ALTER TABLE `appointment` ADD KEY `idx_appointment_phone` (`phone`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'vip' AND index_name = 'uk_vip_vid'), 'DO 1', 'ALTER TABLE `vip` ADD UNIQUE KEY `uk_vip_vid` (`vid`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'vip' AND index_name = 'uk_vip_phone'), 'DO 1', 'ALTER TABLE `vip` ADD UNIQUE KEY `uk_vip_phone` (`phone`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'vip' AND index_name = 'idx_vip_deleted_name'), 'DO 1', 'ALTER TABLE `vip` ADD KEY `idx_vip_deleted_name` (`deleted`, `name`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
