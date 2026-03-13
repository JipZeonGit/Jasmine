# 使用 Eclipse Temurin 的 Java 8 镜像作为基础镜像（openjdk 官方镜像已下架）
FROM eclipse-temurin:8-jre-alpine

# 将本地文件夹挂载到当前容器
VOLUME /tmp

# 将打包生成的 jar 包复制到容器内
# 假设你在 pom.xml 中配置的打包名称是 Jasmine-0.0.1-SNAPSHOT.jar
COPY target/Jasmine-0.0.1-SNAPSHOT.jar app.jar

# 声明服务运行在 9999 端口
EXPOSE 9999

# 指定容器启动程序及参数
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
