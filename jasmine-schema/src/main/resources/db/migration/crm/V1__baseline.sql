-- CRM 基线：会员与预约
CREATE TABLE IF NOT EXISTS `vip` (
  `id` int NOT NULL AUTO_INCREMENT,
  `vid` varchar(20) NULL DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `sex` varchar(2) NULL DEFAULT NULL,
  `phone` varchar(50) NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `appointment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `date` datetime NULL DEFAULT NULL,
  `vid` varchar(20) NULL DEFAULT NULL,
  `name` varchar(50) NULL DEFAULT NULL,
  `sex` varchar(2) NULL DEFAULT NULL,
  `phone` varchar(50) NULL DEFAULT NULL,
  `content` varchar(50) NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO `vip` VALUES (1, '10876678474', '管先生', '男', '13677778888', 0);
INSERT IGNORE INTO `vip` VALUES (2, '10563933573', '黄女士', '女', '13788889999', 0);
INSERT IGNORE INTO `vip` VALUES (3, '10139228001', '叶小姐', '女', '13799990001', 0);
INSERT IGNORE INTO `vip` VALUES (4, '10373332883', '会先生', '男', '13888889999', 1);
INSERT IGNORE INTO `vip` VALUES (5, '10957215420', '李先生', '男', '13677778889', 0);

INSERT IGNORE INTO `appointment` VALUES (1, '2023-07-28 22:18:36', '10876678474', '管先生', '男', '13677778888', '桃花 五份', 0);
INSERT IGNORE INTO `appointment` VALUES (2, '2023-08-17 18:27:24', '10563933573', '黄女士', '女', '13788889999', '荷花 两份', 0);
INSERT IGNORE INTO `appointment` VALUES (3, '2023-07-06 23:17:56', '10876678474', '管先生', '男', '13677778888', '取花', 1);
INSERT IGNORE INTO `appointment` VALUES (4, '2023-07-06 23:17:59', '10139228001', '叶小姐', '女', '13799990001', '取花', 1);
INSERT IGNORE INTO `appointment` VALUES (5, '2023-08-09 12:11:19', '10563933573', '黄女士', '女', '13788889999', '送花', 1);
INSERT IGNORE INTO `appointment` VALUES (6, '2023-08-24 00:00:00', '10563933573', '黄女士', '女', '13788889999', '送花', 1);
