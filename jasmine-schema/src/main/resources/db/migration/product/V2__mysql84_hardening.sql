ALTER TABLE `flower`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `flower` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @sql = (SELECT IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'flower' AND index_name = 'idx_flower_deleted_name'), 'DO 1', 'ALTER TABLE `flower` ADD KEY `idx_flower_deleted_name` (`deleted`, `name`) USING BTREE'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
