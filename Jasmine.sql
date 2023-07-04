/*
 Navicat Premium Data Transfer

 Source Server         : LOCAL
 Source Server Type    : MySQL
 Source Server Version : 50718
 Source Host           : localhost:3307
 Source Schema         : jasmine

 Target Server Type    : MySQL
 Target Server Version : 50718
 File Encoding         : 65001

 Date: 04/07/2023 23:21:56
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for menu
-- ----------------------------
DROP TABLE IF EXISTS `menu`;
CREATE TABLE `menu`  (
  `menu_id` int(11) NOT NULL AUTO_INCREMENT,
  `component` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `redirect` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `parent_id` int(11) NULL DEFAULT NULL,
  `is_leaf` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `hidden` tinyint(1) NULL DEFAULT NULL,
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of menu
-- ----------------------------
INSERT INTO `menu` VALUES (1, 'Layout', '/system', '/system/user', 'sysManage', '用户管理', 'el-icon-s-help', 0, 'N', 0);
INSERT INTO `menu` VALUES (2, 'system/user', 'user', NULL, 'userList', '用户列表', 'icon-user-1', 1, 'Y', 0);
INSERT INTO `menu` VALUES (3, 'system/role', 'role', NULL, 'roleList', '角色列表', 'icon-user', 1, 'Y', 0);
INSERT INTO `menu` VALUES (4, 'Layout', '/custom', '/custom/appointment', 'cusManage', '门店功能', 'el-icon-s-help', 0, 'N', 0);
INSERT INTO `menu` VALUES (5, 'custom/appointment', 'appointment', NULL, 'appointment', '用户预约', 'tree', 4, 'Y', 0);
INSERT INTO `menu` VALUES (6, 'custom/VIP', 'VIP', NULL, 'VIP', '会员管理', 'tree', 4, 'Y', 0);
INSERT INTO `menu` VALUES (7, 'custom/flowerManage', 'flowerManage', NULL, 'flowerManage', '花卉管理', 'tree', 4, 'Y', 0);
INSERT INTO `menu` VALUES (8, 'custom/salesManage', 'salesManage', NULL, 'salesManage', '销售管理', 'tree', 4, 'Y', 0);
INSERT INTO `menu` VALUES (9, 'custom/inventoryManage', 'inventoryManage', NULL, 'inventoryManage', '库存管理', 'tree', 4, 'Y', 0);

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `role_id` int(11) NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `role_desc` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (1, 'admin', '管理员');
INSERT INTO `role` VALUES (2, 'HR', '人事');
INSERT INTO `role` VALUES (4, 'clerk', '花店员工');
INSERT INTO `role` VALUES (7, 'Boss', '老板');

-- ----------------------------
-- Table structure for role_menu
-- ----------------------------
DROP TABLE IF EXISTS `role_menu`;
CREATE TABLE `role_menu`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `role_id` int(11) NULL DEFAULT NULL,
  `menu_id` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 50 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role_menu
-- ----------------------------
INSERT INTO `role_menu` VALUES (25, 1, 1);
INSERT INTO `role_menu` VALUES (26, 1, 2);
INSERT INTO `role_menu` VALUES (27, 1, 3);
INSERT INTO `role_menu` VALUES (28, 1, 4);
INSERT INTO `role_menu` VALUES (29, 1, 5);
INSERT INTO `role_menu` VALUES (30, 1, 6);
INSERT INTO `role_menu` VALUES (31, 1, 7);
INSERT INTO `role_menu` VALUES (32, 1, 8);
INSERT INTO `role_menu` VALUES (33, 1, 9);
INSERT INTO `role_menu` VALUES (40, 4, 7);
INSERT INTO `role_menu` VALUES (41, 4, 8);
INSERT INTO `role_menu` VALUES (42, 4, 9);
INSERT INTO `role_menu` VALUES (43, 4, 4);
INSERT INTO `role_menu` VALUES (44, 7, 4);
INSERT INTO `role_menu` VALUES (45, 7, 5);
INSERT INTO `role_menu` VALUES (46, 7, 6);
INSERT INTO `role_menu` VALUES (47, 7, 7);
INSERT INTO `role_menu` VALUES (48, 7, 8);
INSERT INTO `role_menu` VALUES (49, 7, 9);

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `email` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `status` int(1) NULL DEFAULT NULL,
  `avatar` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `deleted` int(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$JucudGSGaIlP42TJbwaRe.GHjNjoV8opukOxEYQrWwv291Kuyt3iq', 'admin@test.com', '13677778888', 1, 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif', 0);
INSERT INTO `user` VALUES (2, 'Jasmine', '$2a$10$PY5vfxJvPMQwEEtklOOgI.pGwB6kOeQBTuh/OSKqhgmN4wjbMGqQu', 'Jasmine@test.com', '13777777777', 1, 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif', 0);

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NULL DEFAULT NULL,
  `role_id` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_role
-- ----------------------------
INSERT INTO `user_role` VALUES (1, 1, 1);
INSERT INTO `user_role` VALUES (2, 3, 2);
INSERT INTO `user_role` VALUES (3, 2, 1);
INSERT INTO `user_role` VALUES (4, 5, 4);

SET FOREIGN_KEY_CHECKS = 1;
