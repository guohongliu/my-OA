FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
# 只复制构建定义以充分利用缓存
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
# 预拉取依赖（如网络良好可保留，加速后续构建）
RUN chmod +x gradlew && ./gradlew --no-daemon -q help
# 复制源码后再构建
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]