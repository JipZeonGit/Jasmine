# 使用 Eclipse Temurin 的 Java 21 镜像作为基础镜像
FROM eclipse-temurin:21-jre-alpine

# 将本地临时目录挂载到容器中
VOLUME /tmp

# 将打包生成的 jar 包复制到容器内
COPY target/Jasmine-0.0.1-SNAPSHOT.jar app.jar

# 声明服务运行端口
EXPOSE 9999

# 指定容器启动程序及参数
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
