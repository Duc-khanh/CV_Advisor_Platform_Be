# Bước 1: Dùng Gradle image để build dự án
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/app
WORKDIR /home/app
RUN ./gradlew clean build -x test

# Bước 2: Dùng Eclipse Temurin (JDK 17 Alpines) siêu nhẹ và cực kỳ ổn định
FROM eclipse-temurin:17-jre-alpine
EXPOSE 8080
COPY --from=build /home/app/build/libs/CvAdvisorPlatform-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]