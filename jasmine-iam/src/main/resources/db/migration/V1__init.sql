-- jasmine_iam 数据库初始化脚本
-- 包含表：user, role, user_role, menu, role_menu, auth_refresh_token

-- ----------------------------
-- Table structure for user
-- ----------------------------
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NULL DEFAULT NULL,
  `email` varchar(50) NULL DEFAULT NULL,
  `phone` varchar(20) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL,
  `avatar` varchar(200) NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_username` (`username`) USING BTREE,
  KEY `idx_user_phone` (`phone`) USING BTREE,
  KEY `idx_user_deleted_status` (`deleted`, `status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

-- ----------------------------
-- Table structure for role
-- ----------------------------
CREATE TABLE `role` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL,
  `role_desc` varchar(100) NULL DEFAULT NULL,
  PRIMARY KEY (`role_id`) USING BTREE,
  KEY `idx_role_name` (`role_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
CREATE TABLE `user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_role_user_id_role_id` (`user_id`, `role_id`) USING BTREE,
  KEY `idx_user_role_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

-- ----------------------------
-- Table structure for menu
-- ----------------------------
CREATE TABLE `menu` (
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
  PRIMARY KEY (`menu_id`) USING BTREE,
  KEY `idx_menu_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_menu_hidden` (`hidden`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

-- ----------------------------
-- Table structure for role_menu
-- ----------------------------
CREATE TABLE `role_menu` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL,
  `menu_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_menu_role_id_menu_id` (`role_id`, `menu_id`) USING BTREE,
  KEY `idx_role_menu_menu_id` (`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=Dynamic;

-- ----------------------------
-- Table structure for auth_refresh_token
-- ----------------------------
CREATE TABLE `auth_refresh_token` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `token_id` varchar(64) NOT NULL,
  `expires_at` datetime NOT NULL,
  `revoked` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_refresh_token_token_id` (`token_id`),
  KEY `idx_auth_refresh_token_user_state` (`user_id`, `revoked`, `expires_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Default data: admin user
-- ----------------------------
INSERT INTO `user` (`id`, `username`, `password`, `email`, `phone`, `status`, `avatar`, `deleted`) VALUES
  (1, 'admin', '$2a$10$JucudGSGaIlP42TJbwaRe.GHjNjoV8opukOxEYQrWwv291Kuyt3iq', 'admin@test.com', '13677778888', 1, 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif', 0),
  (2, 'Jasmine', '$2a$10$PY5vfxJvPMQwEEtklOOgI.pGwB6kOeQBTuh/OSKqhgmN4wjbMGqQu', 'Jasmine@test.com', '13777777777', 1, 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif', 0);

INSERT INTO `role` (`role_id`, `role_name`, `role_desc`) VALUES
  (1, 'admin', '管理员'),
  (2, 'HR', '人事'),
  (4, 'clerk', '花店员工'),
  (7, 'Boss', '老板');

INSERT INTO `user_role` (`id`, `user_id`, `role_id`) VALUES
  (1, 1, 1),
  (3, 2, 1);

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
