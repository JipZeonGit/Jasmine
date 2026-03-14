# ========================================
# 第一阶段：构建打包 (Build Stage)
# ========================================
FROM node:22.22.1 AS build-stage

WORKDIR /app

# 如果在国内服务器构建可取消注释以加速（GitHub Actions 服务器在海外，使用淘宝源反而更慢）
# RUN npm config set registry https://registry.npmmirror.com

# 拷贝依赖配置并安装
COPY package*.json ./
RUN npm install

# 拷贝所有源代码并执行打包
COPY . .
RUN npm run build:prod

# ========================================
# 第二阶段：生产环境部署 (Production Stage)
# ========================================
FROM nginx:stable-alpine AS production-stage

# 将构建好的前端产物复制到 nginx 默认发布目录下
COPY --from=build-stage /app/dist /usr/share/nginx/html

# 覆盖掉 nginx 的默认配置，加载包含反向代理规则的配置文件
COPY default.conf /etc/nginx/conf.d/default.conf

# 声明暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
