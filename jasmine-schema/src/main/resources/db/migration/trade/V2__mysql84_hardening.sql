ALTER TABLE `inventory`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `quantity` int NULL DEFAULT NULL,
  MODIFY COLUMN `residue` int NULL DEFAULT NULL,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `inventory` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `sales`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `sales` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inventory' AND index_name = 'uk_inventory_num'), 'DO 1', 'ALTER TABLE `inventory` ADD UNIQUE KEY `uk_inventory_num` (`num`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inventory' AND index_name = 'idx_inventory_deleted_name'), 'DO 1', 'ALTER TABLE `inventory` ADD KEY `idx_inventory_deleted_name` (`deleted`, `name`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inventory' AND index_name = 'idx_inventory_date'), 'DO 1', 'ALTER TABLE `inventory` ADD KEY `idx_inventory_date` (`date`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sales' AND index_name = 'idx_sales_deleted_date'), 'DO 1', 'ALTER TABLE `sales` ADD KEY `idx_sales_deleted_date` (`deleted`, `date`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
