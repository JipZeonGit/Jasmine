-- Trade 成本追踪：给 inventory/sales_item 增加成本字段
SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory' AND column_name = 'unit_cost'), 'SELECT 1', 'ALTER TABLE `inventory` ADD COLUMN `unit_cost` decimal(10,2) DEFAULT NULL AFTER `after_stock`'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory' AND column_name = 'total_cost'), 'SELECT 1', 'ALTER TABLE `inventory` ADD COLUMN `total_cost` decimal(12,2) DEFAULT NULL AFTER `unit_cost`'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sales_item' AND column_name = 'unit_cost'), 'SELECT 1', 'ALTER TABLE `sales_item` ADD COLUMN `unit_cost` decimal(10,2) DEFAULT NULL AFTER `unit_price`'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sales_item' AND column_name = 'cost_amount'), 'SELECT 1', 'ALTER TABLE `sales_item` ADD COLUMN `cost_amount` decimal(12,2) DEFAULT NULL AFTER `amount`'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
