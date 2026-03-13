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
*(注：本项目的根目录有初始的数据库SQL文件。如果您有其他初始的数据库SQL文件，可重命名为 `jasmine.sql` 并放置在根目录，MySQL容器首次启动时会自动执行。)*

### 2. 环境变量配置
为了保证项目在本地开发与 Docker 部署之间平滑切换且不互相冲突，后端的 `application.yml` 配置了默认环境变量解析。当通过 Docker 启动时，可在 `docker-compose.yml` 的 `backend` 服务中注入以下变量进行配置覆写（编排文件中默认已配好，即开即用）：
- `MYSQL_HOST` / `MYSQL_PORT`：数据库连接地址与端口（默认使用容器名 `mysql`，端口 `3306`）
- `MYSQL_USER` / `MYSQL_PASSWORD`：数据库账号密码（默认 `root` / `123456`）
- `REDIS_HOST` / `REDIS_PORT`：Redis 连接地址与端口（默认使用容器名 `redis`，端口 `6379`）
- `REDIS_PASSWORD`：Redis 认证密码（默认 `123456`）

*如果您的目标环境使用了外置的独立数据库，也可直接在上述配置文件里修改。*

### 3. 一键编译与启动
在带有 `docker-compose.yml` 的根目录（即 `Jasmine/` 目录下）执行该命令：
```bash
docker-compose up -d --build
```
这条指令会自动拉取所需镜像（Node 22.22.1、Nginx、MySQL 5.7.44、Redis 7.2 等），分阶段编译前端 Vue 代码，并启动所有容器实例放到后台运行。

### 4. 访问系统
在控制台显示各容器状态为 `Started` 并且等待约 1~2 分钟让组件初始化后：
- **前端系统界面**：打开浏览器，直接访问宿主机 IP（如果是本地运行即访问 `http://localhost`），因为前端 Nginx 容器已映射到了主机的 `80` 端口。
- **后端 API 服务**：运行并映射在主机的 `9999` 端口。

### 5. 停止与卸载
若需停止测试清理环境，在项目根目录运行：
```bash
docker-compose down
```
这条命令会停止对应容器栈、并移除所有容器及默认生成的虚拟网络环境。
