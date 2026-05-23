-- Product 基线：花卉主数据
CREATE TABLE IF NOT EXISTS `flower` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `unitprice` decimal(10,2) NULL DEFAULT NULL,
  `costs` decimal(10,2) NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO `flower` VALUES (1, '紫罗兰', 10.00, 7.50, 0);
INSERT IGNORE INTO `flower` VALUES (2, '向日葵', 7.50, 5.00, 0);
INSERT IGNORE INTO `flower` VALUES (3, '红玫瑰', 13.00, 10.00, 0);
INSERT IGNORE INTO `flower` VALUES (4, '水仙花', 5.00, 4.00, 1);
INSERT IGNORE INTO `flower` VALUES (5, '白玫瑰', 17.00, 14.00, 1);
