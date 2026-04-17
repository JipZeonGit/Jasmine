FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 健康检查和简单排障都需要基础 HTTP 工具，这里预装 curl。
RUN apk add --no-cache curl

VOLUME /tmp

COPY target/Jasmine-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 9999

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dfile.encoding=UTF-8","-jar","/app/app.jar"]
