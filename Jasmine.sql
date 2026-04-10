/*
 Jasmine development bootstrap SQL
 Target: MySQL 8.4
 Notes:
 1. This file matches the PR11 business-model rebuild state.
 2. Development business data is disposable; business tables are rebuilt to the new model.
 3. System/auth tables are kept as the application baseline.
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `jasmine` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `jasmine`;

DROP TABLE IF EXISTS `sales_item`;
DROP TABLE IF EXISTS `inventory`;
DROP TABLE IF EXISTS `sales`;
DROP TABLE IF EXISTS `appointment`;
DROP TABLE IF EXISTS `flower`;
DROP TABLE IF EXISTS `vip`;
DROP TABLE IF EXISTS `auth_refresh_token`;
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `role_menu`;
DROP TABLE IF EXISTS `menu`;
DROP TABLE IF EXISTS `role`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) DEFAULT NULL,
  `email` varchar(50) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `avatar` varchar(200) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_username` (`username`) USING BTREE,
  KEY `idx_user_phone` (`phone`) USING BTREE,
  KEY `idx_user_deleted_status` (`deleted`, `status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

CREATE TABLE `role` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL,
  `role_desc` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`role_id`) USING BTREE,
  KEY `idx_role_name` (`role_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

CREATE TABLE `menu` (
  `menu_id` int NOT NULL AUTO_INCREMENT,
  `component` varchar(100) DEFAULT NULL,
  `path` varchar(100) DEFAULT NULL,
  `redirect` varchar(100) DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `title` varchar(100) DEFAULT NULL,
  `icon` varchar(100) DEFAULT NULL,
  `parent_id` int DEFAULT NULL,
  `is_leaf` varchar(1) DEFAULT NULL,
  `hidden` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`menu_id`) USING BTREE,
  KEY `idx_menu_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_menu_hidden` (`hidden`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

CREATE TABLE `role_menu` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL,
  `menu_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_menu_role_id_menu_id` (`role_id`, `menu_id`) USING BTREE,
  KEY `idx_role_menu_menu_id` (`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

CREATE TABLE `user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_role_user_id_role_id` (`user_id`, `role_id`) USING BTREE,
  KEY `idx_user_role_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

CREATE TABLE `auth_refresh_token` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `token_id` varchar(64) NOT NULL,
  `expires_at` datetime NOT NULL,
  `revoked` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_auth_refresh_token_token_id` (`token_id`) USING BTREE,
  KEY `idx_auth_refresh_token_user_id` (`user_id`) USING BTREE,
  KEY `idx_auth_refresh_token_expires_at` (`expires_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

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
  CONSTRAINT `fk_sales_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_sales_vip` FOREIGN KEY (`vip_id`) REFERENCES `vip` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

CREATE TABLE `sales_item` (
  `id` int NOT NULL AUTO_INCREMENT,
  `sales_id` int NOT NULL,
  `flower_id` int NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `unit_cost` decimal(10,2) DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `cost_amount` decimal(12,2) DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sales_item_sales_id_deleted` (`sales_id`, `deleted`) USING BTREE,
  KEY `idx_sales_item_flower_id` (`flower_id`) USING BTREE,
  CONSTRAINT `fk_sales_item_flower` FOREIGN KEY (`flower_id`) REFERENCES `flower` (`id`),
  CONSTRAINT `fk_sales_item_sales` FOREIGN KEY (`sales_id`) REFERENCES `sales` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

CREATE TABLE `inventory` (
  `id` int NOT NULL AUTO_INCREMENT,
  `biz_no` varchar(32) NOT NULL,
  `flower_id` int NOT NULL,
  `biz_type` varchar(32) NOT NULL,
  `quantity` int NOT NULL,
  `before_stock` int NOT NULL,
  `after_stock` int NOT NULL,
  `unit_cost` decimal(10,2) DEFAULT NULL,
  `total_cost` decimal(12,2) DEFAULT NULL,
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

INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`, `status`, `avatar`, `deleted`) VALUES
  (1, 'admin', '$2a$10$JucudGSGaIlP42TJbwaRe.GHjNjoV8opukOxEYQrWwv291Kuyt3iq', 'admin@test.com', '13677778888', 1, 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif', 0),
  (2, 'Jasmine', '$2a$10$PY5vfxJvPMQwEEtklOOgI.pGwB6kOeQBTuh/OSKqhgmN4wjbMGqQu', 'jasmine@test.com', '13777777777', 1, 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif', 0);

INSERT INTO `role` (`role_id`, `role_name`, `role_desc`) VALUES
  (1, 'admin', '管理员'),
  (2, 'HR', '人事'),
  (4, 'clerk', '花店店员'),
  (7, 'Boss', '老板');

INSERT INTO `menu` (`menu_id`, `component`, `path`, `redirect`, `name`, `title`, `icon`, `parent_id`, `is_leaf`, `hidden`) VALUES
  (1, 'Layout', '/system', '/system/user', 'sysManage', '用户管理', 'el-icon-s-help', 0, 'N', 0),
  (2, 'system/user', 'user', NULL, 'userList', '用户列表', 'icon-user-1', 1, 'Y', 0),
  (3, 'system/role', 'role', NULL, 'roleList', '角色列表', 'icon-user', 1, 'Y', 0),
  (4, 'Layout', '/custom', '/custom/appointment', 'cusManage', '门店功能', 'el-icon-s-help', 0, 'N', 0),
  (5, 'custom/appointment', 'appointment', NULL, 'appointment', '用户预约', 'tree', 4, 'Y', 0),
  (6, 'custom/VIP', 'VIP', NULL, 'VIP', '会员管理', 'tree', 4, 'Y', 0),
  (7, 'custom/flowerManage', 'flowerManage', NULL, 'flowerManage', '花卉管理', 'tree', 4, 'Y', 0),
  (8, 'custom/salesManage', 'salesManage', NULL, 'salesManage', '销售管理', 'tree', 4, 'Y', 0),
  (9, 'custom/inventoryManage', 'inventoryManage', NULL, 'inventoryManage', '库存管理', 'tree', 4, 'Y', 0);

INSERT INTO `role_menu` (`id`, `role_id`, `menu_id`) VALUES
  (25, 1, 1),
  (26, 1, 2),
  (27, 1, 3),
  (28, 1, 4),
  (29, 1, 5),
  (30, 1, 6),
  (31, 1, 7),
  (32, 1, 8),
  (33, 1, 9),
  (40, 4, 7),
  (41, 4, 8),
  (42, 4, 9),
  (43, 4, 4),
  (44, 7, 4),
  (45, 7, 5),
  (46, 7, 6),
  (47, 7, 7),
  (48, 7, 8),
  (49, 7, 9);

INSERT INTO `user_role` (`id`, `user_id`, `role_id`) VALUES
  (1, 1, 1),
  (3, 2, 1);

INSERT INTO `vip` (`id`, `vid`, `name`, `sex`, `phone`, `deleted`) VALUES
  (1, '10876678474', '管先生', '男', '13677778888', 0),
  (2, '10563933573', '黄女士', '女', '13788889999', 0),
  (3, '10139228001', '叶小姐', '女', '13799990001', 0);

INSERT INTO `flower` (`id`, `name`, `unit`, `sale_price`, `cost_price`, `safe_stock`, `current_stock`, `status`, `deleted`) VALUES
  (1, '红玫瑰', '枝', 13.00, 10.00, 30, 120, 1, 0),
  (2, '向日葵', '枝', 7.50, 5.00, 20, 80, 1, 0),
  (3, '洋桔梗', '扎', 18.00, 12.00, 15, 45, 1, 0),
  (4, '白玫瑰', '枝', 17.00, 14.00, 20, 60, 1, 0);

INSERT INTO `inventory` (`id`, `biz_no`, `flower_id`, `biz_type`, `quantity`, `before_stock`, `after_stock`, `unit_cost`, `total_cost`, `remark`, `operator_id`, `date`, `deleted`) VALUES
  (1, 'INV-INIT-ROSE', 1, 'PURCHASE_IN', 120, 0, 120, 10.00, 1200.00, '初始化库存', 1, '2026-04-10 09:00:00', 0),
  (2, 'INV-INIT-SUN', 2, 'PURCHASE_IN', 80, 0, 80, 5.00, 400.00, '初始化库存', 1, '2026-04-10 09:05:00', 0),
  (3, 'INV-INIT-EUST', 3, 'PURCHASE_IN', 45, 0, 45, 12.00, 540.00, '初始化库存', 1, '2026-04-10 09:10:00', 0),
  (4, 'INV-INIT-WROSE', 4, 'PURCHASE_IN', 60, 0, 60, 14.00, 840.00, '初始化库存', 1, '2026-04-10 09:15:00', 0);

INSERT INTO `appointment` (`id`, `vip_id`, `date`, `content`, `deleted`) VALUES
  (1, 1, '2026-04-12 10:00:00', '红玫瑰花束预约', 0),
  (2, 2, '2026-04-13 16:30:00', '向日葵到店自提', 0);

SET FOREIGN_KEY_CHECKS = 1;
