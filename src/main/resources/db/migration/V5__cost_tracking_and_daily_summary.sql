SET @inventory_unit_cost_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'inventory'
        AND column_name = 'unit_cost'
    ),
    'SELECT 1',
    'ALTER TABLE `inventory` ADD COLUMN `unit_cost` decimal(10,2) DEFAULT NULL AFTER `after_stock`'
  )
);
PREPARE inventory_unit_cost_stmt FROM @inventory_unit_cost_sql;
EXECUTE inventory_unit_cost_stmt;
DEALLOCATE PREPARE inventory_unit_cost_stmt;

SET @inventory_total_cost_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'inventory'
        AND column_name = 'total_cost'
    ),
    'SELECT 1',
    'ALTER TABLE `inventory` ADD COLUMN `total_cost` decimal(12,2) DEFAULT NULL AFTER `unit_cost`'
  )
);
PREPARE inventory_total_cost_stmt FROM @inventory_total_cost_sql;
EXECUTE inventory_total_cost_stmt;
DEALLOCATE PREPARE inventory_total_cost_stmt;

SET @sales_item_unit_cost_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'sales_item'
        AND column_name = 'unit_cost'
    ),
    'SELECT 1',
    'ALTER TABLE `sales_item` ADD COLUMN `unit_cost` decimal(10,2) DEFAULT NULL AFTER `unit_price`'
  )
);
PREPARE sales_item_unit_cost_stmt FROM @sales_item_unit_cost_sql;
EXECUTE sales_item_unit_cost_stmt;
DEALLOCATE PREPARE sales_item_unit_cost_stmt;

SET @sales_item_cost_amount_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'sales_item'
        AND column_name = 'cost_amount'
    ),
    'SELECT 1',
    'ALTER TABLE `sales_item` ADD COLUMN `cost_amount` decimal(12,2) DEFAULT NULL AFTER `amount`'
  )
);
PREPARE sales_item_cost_amount_stmt FROM @sales_item_cost_amount_sql;
EXECUTE sales_item_cost_amount_stmt;
DEALLOCATE PREPARE sales_item_cost_amount_stmt;

UPDATE `inventory` i
JOIN `flower` f ON f.`id` = i.`flower_id`
SET
  i.`unit_cost` = CASE
    WHEN i.`unit_cost` IS NULL AND i.`biz_type` = 'PURCHASE_IN' THEN f.`cost_price`
    ELSE i.`unit_cost`
  END,
  i.`total_cost` = CASE
    WHEN i.`total_cost` IS NULL AND i.`biz_type` = 'PURCHASE_IN' THEN f.`cost_price` * i.`quantity`
    ELSE i.`total_cost`
  END
WHERE i.`biz_type` = 'PURCHASE_IN';

UPDATE `sales_item` si
JOIN `flower` f ON f.`id` = si.`flower_id`
SET
  si.`unit_cost` = COALESCE(si.`unit_cost`, f.`cost_price`),
  si.`cost_amount` = COALESCE(si.`cost_amount`, COALESCE(si.`unit_cost`, f.`cost_price`) * si.`quantity`);

UPDATE `inventory` i
JOIN `flower` f ON f.`id` = i.`flower_id`
SET
  i.`unit_cost` = COALESCE(i.`unit_cost`, f.`cost_price`),
  i.`total_cost` = COALESCE(i.`total_cost`, COALESCE(i.`unit_cost`, f.`cost_price`) * i.`quantity`)
WHERE i.`biz_type` = 'SALE_OUT';
