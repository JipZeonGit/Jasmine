-- IAM 基线：用户、角色、权限相关表
CREATE TABLE IF NOT EXISTS `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NULL DEFAULT NULL,
  `email` varchar(50) NULL DEFAULT NULL,
  `phone` varchar(20) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL,
  `avatar` varchar(200) NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `role` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL,
  `role_desc` varchar(100) NULL DEFAULT NULL,
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `menu` (
  `menu_id` int NOT NULL AUTO_INCREMENT,
  `component` varchar(100) NULL DEFAULT NULL,
  `path` varchar(100) NULL DEFAULT NULL,
  `redirect` varchar(100) NULL DEFAULT NULL,
  `name` varchar(100) NULL DEFAULT NULL,
  `title` varchar(100) NULL DEFAULT NULL,
  `icon` varchar(100) NULL DEFAULT NULL,
  `parent_id` int NULL DEFAULT NULL,
  `is_leaf` varchar(1) NULL DEFAULT NULL,
  `hidden` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `role_menu` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL,
  `menu_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 初始数据
INSERT IGNORE INTO `user` VALUES (1, 'admin', '$2a$10$8soCrECtCjr6MAr5hdzdhut9qeTdD036ClJ6zbi/AjINBvztCIpKG', 'admin@test.com', '13677778888', 1, 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif', 0);
INSERT IGNORE INTO `user` VALUES (2, 'Jasmine', '$2a$10$PY5vfxJvPMQwEEtklOOgI.pGwB6kOeQBTuh/OSKqhgmN4wjbMGqQu', 'Jasmine@test.com', '13777777777', 1, 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif', 0);

INSERT IGNORE INTO `role` VALUES (1, 'admin', '管理员');
INSERT IGNORE INTO `role` VALUES (2, 'HR', '人事');
INSERT IGNORE INTO `role` VALUES (4, 'clerk', '花店员工');
INSERT IGNORE INTO `role` VALUES (7, 'Boss', '老板');

INSERT IGNORE INTO `menu` VALUES (1, 'Layout', '/system', '/system/user', 'sysManage', '用户管理', 'el-icon-s-help', 0, 'N', 0);
INSERT IGNORE INTO `menu` VALUES (2, 'system/user', 'user', NULL, 'userList', '用户列表', 'icon-user-1', 1, 'Y', 0);
INSERT IGNORE INTO `menu` VALUES (3, 'system/role', 'role', NULL, 'roleList', '角色列表', 'icon-user', 1, 'Y', 0);
INSERT IGNORE INTO `menu` VALUES (4, 'Layout', '/custom', '/custom/appointment', 'cusManage', '门店功能', 'el-icon-s-help', 0, 'N', 0);
INSERT IGNORE INTO `menu` VALUES (5, 'custom/appointment', 'appointment', NULL, 'appointment', '用户预约', 'tree', 4, 'Y', 0);
INSERT IGNORE INTO `menu` VALUES (6, 'custom/VIP', 'VIP', NULL, 'VIP', '会员管理', 'tree', 4, 'Y', 0);
INSERT IGNORE INTO `menu` VALUES (7, 'custom/flowerManage', 'flowerManage', NULL, 'flowerManage', '花卉管理', 'tree', 4, 'Y', 0);
INSERT IGNORE INTO `menu` VALUES (8, 'custom/salesManage', 'salesManage', NULL, 'salesManage', '销售管理', 'tree', 4, 'Y', 0);
INSERT IGNORE INTO `menu` VALUES (9, 'custom/inventoryManage', 'inventoryManage', NULL, 'inventoryManage', '库存管理', 'tree', 4, 'Y', 0);

INSERT IGNORE INTO `user_role` VALUES (1, 1, 1);
INSERT IGNORE INTO `user_role` VALUES (3, 2, 1);

INSERT IGNORE INTO `role_menu` VALUES (25, 1, 1),(26, 1, 2),(27, 1, 3),(28, 1, 4),(29, 1, 5),(30, 1, 6),(31, 1, 7),(32, 1, 8),(33, 1, 9);
INSERT IGNORE INTO `role_menu` VALUES (40, 4, 7),(41, 4, 8),(42, 4, 9),(43, 4, 4),(44, 7, 4),(45, 7, 5),(46, 7, 6),(47, 7, 7),(48, 7, 8),(49, 7, 9);
