SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `flower`;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `flower` (`id`, `name`, `unit`, `sale_price`, `cost_price`, `safe_stock`, `current_stock`, `status`, `deleted`) VALUES
  (1, '红玫瑰', '枝', 13.00, 10.00, 30, 120, 1, 0),
  (2, '向日葵', '枝', 7.50, 5.00, 20, 80, 1, 0),
  (3, '洋桔梗', '扎', 18.00, 12.00, 15, 45, 1, 0),
  (4, '白玫瑰', '枝', 17.00, 14.00, 20, 60, 1, 0);

SET FOREIGN_KEY_CHECKS = 1;
