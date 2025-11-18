FROM gradle:8.7.0-jdk21 AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle -g /home/gradle/.gradle clean bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]