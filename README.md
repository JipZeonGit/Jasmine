# Jasmine
一个基于 SpringBoot 和 VUE 的 花店管理系统

## 技术选型
|         技术         |          类型          |   版本    |
|:------------------:|:--------------------:|:-------:|
| Java Development Kit |          开发环境          | 1.8.0_472 |
|     SpringBoot     |          框架          |  2.7.8  |
|    MyBatis-Plus    |        持久层框架         |  3.5.2  |
|  Spring Security   |         安全模块         |  2.7.8  |
|        JWT         |    Json Web Token    |  0.9.1  |
|       Lombok       |          工具          | 1.18.24 |
|      Swagger       |       API 文档工具       |  3.0.0  |
|     FreeMarker     |         模板引擎         | 2.3.32  |
|     Fast Json      |      高性能 Json 库      |  2.0.7  |
|      Node.js       |  JavaScript 服务器运行环境  | 22.22.1 |
| vue-admin-template | VUE 和 ElementUI 集成环境 |  4.4.0  |


## 数据库
|技术|        类型         |   版本   |
|:--:|:-----------------:|:------:|
|MySQL|      关系型数据库       | 5.7.44 |
|Redis| 高性能 Key-Value 数据库 |  7.2   |


## 额外说明
项目在本地部署请根据实际环境进行修改 application.yml 的 后端端口号 、 MySQL 、 Redis 的数据库链接以及用户名和密码等

## 项目启动（Windows本地调试）
1. 后端（9999 端口）
```bash
cd Jasmine
./mvnw spring-boot:run
```
2. 前端（8888 端口）
```bash
cd Jasmine/vue-admin-template-4.4.0
npm run dev
```

## 项目启动（Docker容器部署）
本项目支持通过 Docker Compose 一键启动 MySQL 数据库、Redis 缓存、Spring Boot 后端和 Vue 前端，彻底解决环境兼容与可移植性问题。

### 1. 准备工作
确保部署的主机或电脑已经安装了 [Docker](https://www.docker.com/) 和 [Docker Compose](https://docs.docker.com/compose/)。
在构建镜像前，需确保**后端项目已成功打包成 jar 文件**。请在项目根目录终端下执行以下命令打包：
```bash
# 进入根目录
cd Jasmine
./mvnw clean package -DskipTests
```
> 注：项目根目录下的 `Jasmine.sql` 为初始化数据库脚本，MySQL 容器首次启动时会自动导入。

### 2. 环境变量配置（.env 文件）
本项目采用 **`.env` 文件**管理所有敏感配置（数据库密码等），`.env` 文件已被 `.gitignore` 排除，**不会被提交到 Git 仓库**，从源头杜绝密码泄漏。

**首次部署时，请按以下步骤配置：**
```bash
# 复制示例模板为实际配置文件
cp .env.example .env

# 编辑 .env 文件，修改为您的实际密码
vim .env   # 或使用任意文本编辑器
```

`.env` 文件中包含以下配置项：

| 变量名 | 说明 | 示例值 |
|:---|:---|:---|
| `MYSQL_ROOT_PASSWORD` | MySQL root 根密码（仅运维管理使用，应用不会用到） | `YourRootPass!` |
| `MYSQL_DATABASE` | 初始化创建的数据库名 | `jasmine` |
| `MYSQL_USER` | 应用专用的数据库连接用户（**非 root**） | `jasmine_app` |
| `MYSQL_PASSWORD` | 应用专用用户的密码 | `YourAppPass!` |
| `REDIS_PASSWORD` | Redis 认证密码 | `YourRedisPass!` |
| `CORS_ALLOWED_ORIGINS` | （可选）CORS 跨域白名单，多个地址用逗号分隔 | `http://localhost` |

`docker-compose.yml` 通过 `${变量名}` 语法引用 `.env` 文件中的值，Docker Compose 启动时会自动读取并注入。

### 3. 数据库权限安全模型
为了遵循生产环境安全最佳实践，本项目的 Docker 配置采用了**最小权限原则**：

| 用户 | 用途 | 权限范围 |
|:---|:---|:---|
| `root` | 仅限运维人员紧急维护或手动管理 | MySQL 全局最高权限 |
| `jasmine_app`（可自定义） | Spring Boot 后端应用连接数据库 | 仅限 `jasmine` 库的增删改查 |

MySQL 官方 Docker 镜像在首次启动时，会自动根据 `.env` 中的 `MYSQL_USER` 和 `MYSQL_PASSWORD` 创建该普通用户，并授予其对 `MYSQL_DATABASE` 指定数据库的全部表级操作权限（等同于 `GRANT ALL ON jasmine.* TO 'jasmine_app'`）。

后端通过该受限用户连接数据库，即便遭遇 SQL 注入攻击，攻击者也**无法访问其他数据库、无法执行系统级危险操作**（如 `DROP DATABASE`、`SHUTDOWN` 等）。

### 4. 一键编译与启动
在项目根目录（即包含 `docker-compose.yml` 的 `Jasmine/` 目录下）执行：
```bash
docker-compose up -d --build
```
这条指令会自动拉取所需镜像（Node 22.22.1、Nginx、MySQL 5.7.44、Redis 7.2 等），分阶段编译前端 Vue 代码，并启动所有容器实例放到后台运行。

### 5. 访问系统
在控制台显示各容器状态为 `Started` 并且等待约 1~2 分钟让组件初始化后：
- **前端系统界面**：打开浏览器，直接访问宿主机 IP（如果是本地运行即访问 `http://localhost`），因为前端 Nginx 容器已映射到了主机的 `80` 端口。
- **后端 API 服务**：运行并映射在主机的 `9999` 端口。

### 6. 停止与卸载
若需停止并清理环境，在项目根目录运行：
```bash
docker-compose down
```
这条命令会停止对应容器栈、并移除所有容器及默认生成的虚拟网络环境。

> 注意：`docker-compose down` 默认**不会删除**数据持久化卷（`mysql-data`、`redis-data`）。如需彻底清除所有数据（包括数据库内容），请使用：
> ```bash
> docker-compose down -v
> ```
