-- 为各微服务创建独立数据库并授权
CREATE DATABASE IF NOT EXISTS jasmine_iam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS jasmine_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS jasmine_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS jasmine_crm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS nacos DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL ON jasmine_iam.* TO 'jasmine_app'@'%';
GRANT ALL ON jasmine_product.* TO 'jasmine_app'@'%';
GRANT ALL ON jasmine_trade.* TO 'jasmine_app'@'%';
GRANT ALL ON jasmine_crm.* TO 'jasmine_app'@'%';
GRANT ALL ON nacos.* TO 'jasmine_app'@'%';
