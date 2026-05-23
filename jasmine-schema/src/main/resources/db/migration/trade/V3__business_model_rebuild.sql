SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `sales_item`;
DROP TABLE IF EXISTS `inventory`;
DROP TABLE IF EXISTS `sales`;

CREATE TABLE `sales` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL,
  `vip_id` int DEFAULT NULL,
  `date` datetime NOT NULL,
  `total_amount` decimal(12,2) NOT NULL DEFAULT 0,
  `remark` varchar(200) DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_sales_order_no` (`order_no`) USING BTREE,
  KEY `idx_sales_deleted_date` (`deleted`, `date`) USING BTREE,
  KEY `idx_sales_vip_id` (`vip_id`) USING BTREE,
  KEY `idx_sales_operator_id` (`operator_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sales_item` (
  `id` int NOT NULL AUTO_INCREMENT,
  `sales_id` int NOT NULL,
  `flower_id` int NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sales_item_sales_id_deleted` (`sales_id`, `deleted`) USING BTREE,
  KEY `idx_sales_item_flower_id` (`flower_id`) USING BTREE,
  CONSTRAINT `fk_sales_item_sales` FOREIGN KEY (`sales_id`) REFERENCES `sales` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `inventory` (
  `id` int NOT NULL AUTO_INCREMENT,
  `biz_no` varchar(32) NOT NULL,
  `flower_id` int NOT NULL,
  `biz_type` varchar(32) NOT NULL,
  `quantity` int NOT NULL,
  `before_stock` int NOT NULL,
  `after_stock` int NOT NULL,
  `remark` varchar(200) DEFAULT NULL,
  `operator_id` int DEFAULT NULL,
  `date` datetime NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_inventory_deleted_date` (`deleted`, `date`) USING BTREE,
  KEY `idx_inventory_flower_id` (`flower_id`) USING BTREE,
  KEY `idx_inventory_biz_type` (`biz_type`) USING BTREE,
  KEY `idx_inventory_biz_no` (`biz_no`) USING BTREE,
  KEY `idx_inventory_operator_id` (`operator_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `inventory` (`id`, `biz_no`, `flower_id`, `biz_type`, `quantity`, `before_stock`, `after_stock`, `remark`, `operator_id`, `date`, `deleted`) VALUES
  (1, 'INV-INIT-ROSE', 1, 'PURCHASE_IN', 120, 0, 120, '初始化库存', 1, '2026-04-10 09:00:00', 0),
  (2, 'INV-INIT-SUN', 2, 'PURCHASE_IN', 80, 0, 80, '初始化库存', 1, '2026-04-10 09:05:00', 0),
  (3, 'INV-INIT-EUST', 3, 'PURCHASE_IN', 45, 0, 45, '初始化库存', 1, '2026-04-10 09:10:00', 0),
  (4, 'INV-INIT-WROSE', 4, 'PURCHASE_IN', 60, 0, 60, '初始化库存', 1, '2026-04-10 09:15:00', 0);

SET FOREIGN_KEY_CHECKS = 1;
