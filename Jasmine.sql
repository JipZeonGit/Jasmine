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

 Date: 22/08/2023 00:15:25
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 确保数据库存在并切换使用（兼容 Docker 自动初始化和手动导入两种场景）
CREATE DATABASE IF NOT EXISTS `jasmine` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `jasmine`;

-- ----------------------------
-- Table structure for appointment
-- ----------------------------
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `date` datetime NULL DEFAULT NULL,
  `vid` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `sex` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `phone` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `content` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `deleted` int(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of appointment
-- ----------------------------
INSERT INTO `appointment` VALUES (1, '2023-07-28 22:18:36', '10876678474', '管先生', '男', '13677778888', '桃花 五份', 0);
INSERT INTO `appointment` VALUES (2, '2023-08-17 18:27:24', '10563933573', '黄女士', '女', '13788889999', '荷花 两份', 0);
INSERT INTO `appointment` VALUES (3, '2023-07-06 23:17:56', '10876678474', '管先生', '男', '13677778888', '取花', 1);
INSERT INTO `appointment` VALUES (4, '2023-07-06 23:17:59', '10139228001', '叶小姐', '女', '13799990001', '取花', 1);
INSERT INTO `appointment` VALUES (5, '2023-08-09 12:11:19', '10563933573', '黄女士', '女', '13788889999', '送花', 1);
INSERT INTO `appointment` VALUES (6, '2023-08-24 00:00:00', '10563933573', '黄女士', '女', '13788889999', '送花', 1);

-- ----------------------------
-- Table structure for flower
-- ----------------------------
DROP TABLE IF EXISTS `flower`;
CREATE TABLE `flower`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `unitprice` decimal(10, 2) NULL DEFAULT NULL,
  `costs` decimal(10, 2) NULL DEFAULT NULL,
  `deleted` int(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of flower
-- ----------------------------
INSERT INTO `flower` VALUES (1, '紫罗兰', 10.00, 7.50, 0);
INSERT INTO `flower` VALUES (2, '向日葵', 7.50, 5.00, 0);
INSERT INTO `flower` VALUES (3, '红玫瑰', 13.00, 10.00, 0);
INSERT INTO `flower` VALUES (4, '水仙花', 5.00, 4.00, 1);
INSERT INTO `flower` VALUES (5, '白玫瑰', 17.00, 14.00, 1);

-- ----------------------------
-- Table structure for inventory
-- ----------------------------
DROP TABLE IF EXISTS `inventory`;
CREATE TABLE `inventory`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `num` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `quantity` int(20) NULL DEFAULT NULL,
  `date` datetime NULL DEFAULT NULL,
  `residue` int(20) NULL DEFAULT NULL,
  `deleted` int(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of inventory
-- ----------------------------
INSERT INTO `inventory` VALUES (1, '紫罗兰', '2023070812830580', 200, '2023-07-08 22:20:56', 200, 0);
INSERT INTO `inventory` VALUES (2, '四季玫瑰', '2023070878913924', 50, '2023-07-08 22:19:21', 50, 1);
INSERT INTO `inventory` VALUES (3, '白玫瑰', '2023081293409505', 300, '2023-08-12 17:03:56', 300, 0);
INSERT INTO `inventory` VALUES (4, '雏菊', '2023081235853725', 3000, '2023-08-12 13:43:45', 3000, 0);
INSERT INTO `inventory` VALUES (5, '雏菊', '2023081289245739', 3000, '2023-08-12 13:43:45', 3000, 1);
INSERT INTO `inventory` VALUES (6, '山茶花', '2023081215776988', 200, '2023-08-12 13:52:49', 200, 0);
INSERT INTO `inventory` VALUES (7, '胡桃', '2023081270757883', 200, '2023-08-12 22:26:15', 200, 0);
INSERT INTO `inventory` VALUES (8, '荷花', '2023081229342714', -200, '2023-08-12 22:39:33', 10, 0);
INSERT INTO `inventory` VALUES (9, '桃花', '2023081241186918', 300, '2023-08-11 20:50:05', 200, 0);
INSERT INTO `inventory` VALUES (10, '杜鹃花', '2023081324755103', 100, '2023-08-13 22:35:18', 100, 0);
INSERT INTO `inventory` VALUES (11, '郁金香', '2023081349087690', 1000, '2023-08-03 20:02:03', 1800, 0);
INSERT INTO `inventory` VALUES (12, '葵花', '2023081493090861', 100, '2023-08-14 21:00:56', 100, 0);

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
-- Table structure for sales
-- ----------------------------
DROP TABLE IF EXISTS `sales`;
CREATE TABLE `sales`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `date` datetime NULL DEFAULT NULL,
  `money` decimal(15, 2) NULL DEFAULT NULL,
  `deleted` int(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sales
-- ----------------------------
INSERT INTO `sales` VALUES (1, '2023-07-06 21:17:35', 4350.75, 0);
INSERT INTO `sales` VALUES (2, '2023-07-07 21:17:59', 3897.40, 0);
INSERT INTO `sales` VALUES (3, '2023-07-07 21:50:15', 3000.50, 1);
INSERT INTO `sales` VALUES (4, '2023-08-15 04:38:13', 30167.50, 1);
INSERT INTO `sales` VALUES (5, '2023-08-15 04:49:05', 0.00, 1);
INSERT INTO `sales` VALUES (6, '2023-08-14 21:01:57', 218.95, 0);
INSERT INTO `sales` VALUES (7, '2023-08-15 18:25:23', -400.25, 0);

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

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

-- ----------------------------
-- Table structure for vip
-- ----------------------------
DROP TABLE IF EXISTS `vip`;
CREATE TABLE `vip`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `vid` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sex` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `phone` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `deleted` int(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vip
-- ----------------------------
INSERT INTO `vip` VALUES (1, '10876678474', '管先生', '男', '13677778888', 0);
INSERT INTO `vip` VALUES (2, '10563933573', '黄女士', '女', '13788889999', 0);
INSERT INTO `vip` VALUES (3, '10139228001', '叶小姐', '女', '13799990001', 0);
INSERT INTO `vip` VALUES (4, '10373332883', '会先生', '男', '13888889999', 1);
INSERT INTO `vip` VALUES (5, '10957215420', '李先生', '男', '13677778889', 0);

SET FOREIGN_KEY_CHECKS = 1;
