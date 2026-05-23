SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `appointment`;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `vip` (`id`, `vid`, `name`, `sex`, `phone`, `deleted`) VALUES
  (1, '10876678474', '管先生', '男', '13677778888', 0),
  (2, '10563933573', '黄女士', '女', '13788889999', 0),
  (3, '10139228001', '叶小姐', '女', '13799990001', 0);

INSERT INTO `appointment` (`id`, `vip_id`, `date`, `content`, `deleted`) VALUES
  (1, 1, '2026-04-12 10:00:00', '红玫瑰花束预订', 0),
  (2, 2, '2026-04-13 16:30:00', '向日葵到店自提', 0);

SET FOREIGN_KEY_CHECKS = 1;
