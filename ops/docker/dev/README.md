# 本地开发栈

`microservices` 分支的本地开发约定：仅在 Docker（推荐 WSL2 / Linux 主机）里跑中间件，**业务服务直接在本机 IDE / Maven 中以 dev profile 启动**，省掉每次改代码都要重打镜像的成本。

## 1. 启动中间件

```bash
cp ops/.env.example ops/docker/dev/.env   # 第一次启动时复制并修改密码
cd ops/docker/dev
docker compose up -d
```

启动后会得到：

| 容器 | 暴露端口 | 说明 |
| --- | --- | --- |
| `jasmine-dev-mysql` | 13306 → 3306 | MySQL 8.4 |
| `jasmine-dev-redis` | 6379 → 6379 | Redis 7.2 |
| `jasmine-dev-rabbitmq` | 5673/15673 | AMQP / 管理台 |
| `jasmine-dev-nacos` | 8848/9848 | Nacos 2.4.3，本地默认关闭鉴权 |

健康检查全部通过后即可启动业务服务。

## 2. 导入 Nacos 配置

```bash
cd ops/nacos-config
bash import.sh 127.0.0.1:8848 dev
```

脚本会自动判断 Nacos 是否开启鉴权，dev 默认关闭时直接走匿名路径。

## 3. 在本机以 dev profile 启动业务服务

中间件已在本机 `127.0.0.1` 上暴露端口，对应的 `application-dev.yml` 已默认指向这些地址。
直接传入密码与 vhost 即可：

```powershell
$env:MYSQL_USER='jasmine_app'
$env:MYSQL_PASSWORD='jasmine-dev-pass'
$env:REDIS_PASSWORD='redis-dev-pass'
$env:RABBITMQ_USERNAME='jasmine'
$env:RABBITMQ_PASSWORD='rabbit-dev-pass'
$env:NACOS_ADDR='127.0.0.1:8848'
$env:SPRING_PROFILES_ACTIVE='dev'

# 任选一个服务单独启动
./mvnw -pl jasmine-iam spring-boot:run
```

要联调 Gateway + 多个服务时，同一台机器上分别在不同终端启动即可。

## 4. 关停 / 清理

```bash
cd ops/docker/dev
docker compose down            # 仅停止
docker compose down -v         # 停止并清掉 named volume（mysql/redis/rabbitmq/nacos 全部数据）
```
