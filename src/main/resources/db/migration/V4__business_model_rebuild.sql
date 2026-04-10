-- PR11 业务模型重构
-- 说明：
-- 1. 这轮按“开发环境业务数据可重建”处理，直接重建业务表模型。
-- 2. 系统权限、认证等基础表继续沿用既有结构，业务表改为更贴近门店动作的模型。
-- 3. Flyway 历史版本不回改，通过新增 V4 完成正式收口。

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sales_item`;
DROP TABLE IF EXISTS `inventory`;
DROP TABLE IF EXISTS `sales`;
DROP TABLE IF EXISTS `appointment`;
DROP TABLE IF EXISTS `flower`;
DROP TABLE IF EXISTS `vip`;

CREATE TABLE `vip` (
  `id` int NOT NULL AUTO_INCREMENT,
  `vid` varchar(20) NOT NULL,
  `name` varchar(50) NOT NULL,
  `sex` varchar(2) DEFAULT NULL,
  `phone` varchar(20) NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_vip_vid` (`vid`) USING BTREE,
  UNIQUE KEY `uk_vip_phone` (`phone`) USING BTREE,
  KEY `idx_vip_deleted_name` (`deleted`, `name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

CREATE TABLE `flower` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `unit` varchar(10) NOT NULL DEFAULT '枝',
  `sale_price` decimal(10,2) NOT NULL,
  `cost_price` decimal(10,2) NOT NULL,
  `safe_stock` int NOT NULL DEFAULT 0,
  `current_stock` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_flower_name` (`name`) USING BTREE,
  KEY `idx_flower_deleted_status_name` (`deleted`, `status`, `name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

CREATE TABLE `appointment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `vip_id` int NOT NULL,
  `date` datetime NOT NULL,
  `content` varchar(100) NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_appointment_deleted_date` (`deleted`, `date`) USING BTREE,
  KEY `idx_appointment_vip_id` (`vip_id`) USING BTREE,
  CONSTRAINT `fk_appointment_vip` FOREIGN KEY (`vip_id`) REFERENCES `vip` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

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
  KEY `idx_sales_operator_id` (`operator_id`) USING BTREE,
  CONSTRAINT `fk_sales_vip` FOREIGN KEY (`vip_id`) REFERENCES `vip` (`id`),
  CONSTRAINT `fk_sales_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

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
  CONSTRAINT `fk_sales_item_sales` FOREIGN KEY (`sales_id`) REFERENCES `sales` (`id`),
  CONSTRAINT `fk_sales_item_flower` FOREIGN KEY (`flower_id`) REFERENCES `flower` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

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
  KEY `idx_inventory_operator_id` (`operator_id`) USING BTREE,
  CONSTRAINT `fk_inventory_flower` FOREIGN KEY (`flower_id`) REFERENCES `flower` (`id`),
  CONSTRAINT `fk_inventory_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

INSERT INTO `vip` (`id`, `vid`, `name`, `sex`, `phone`, `deleted`) VALUES
  (1, '10876678474', '管先生', '男', '13677778888', 0),
  (2, '10563933573', '黄女士', '女', '13788889999', 0),
  (3, '10139228001', '叶小姐', '女', '13799990001', 0);

INSERT INTO `flower` (`id`, `name`, `unit`, `sale_price`, `cost_price`, `safe_stock`, `current_stock`, `status`, `deleted`) VALUES
  (1, '红玫瑰', '枝', 13.00, 10.00, 30, 120, 1, 0),
  (2, '向日葵', '枝', 7.50, 5.00, 20, 80, 1, 0),
  (3, '洋桔梗', '扎', 18.00, 12.00, 15, 45, 1, 0),
  (4, '白玫瑰', '枝', 17.00, 14.00, 20, 60, 1, 0);

INSERT INTO `inventory` (`id`, `biz_no`, `flower_id`, `biz_type`, `quantity`, `before_stock`, `after_stock`, `remark`, `operator_id`, `date`, `deleted`) VALUES
  (1, 'INV-INIT-ROSE', 1, 'PURCHASE_IN', 120, 0, 120, '初始化库存', 1, '2026-04-10 09:00:00', 0),
  (2, 'INV-INIT-SUN', 2, 'PURCHASE_IN', 80, 0, 80, '初始化库存', 1, '2026-04-10 09:05:00', 0),
  (3, 'INV-INIT-EUST', 3, 'PURCHASE_IN', 45, 0, 45, '初始化库存', 1, '2026-04-10 09:10:00', 0),
  (4, 'INV-INIT-WROSE', 4, 'PURCHASE_IN', 60, 0, 60, '初始化库存', 1, '2026-04-10 09:15:00', 0);

INSERT INTO `appointment` (`id`, `vip_id`, `date`, `content`, `deleted`) VALUES
  (1, 1, '2026-04-12 10:00:00', '红玫瑰花束预订', 0),
  (2, 2, '2026-04-13 16:30:00', '向日葵到店自提', 0);

SET FOREIGN_KEY_CHECKS = 1;
