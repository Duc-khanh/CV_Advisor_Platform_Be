# Bước 1: Dùng Gradle image để build file .jar từ code nguồn
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/app
WORKDIR /home/app
RUN ./gradlew clean build -x test

# Bước 2: Dùng JDK image siêu nhẹ để chạy file .jar sau khi build thành công
FROM openjdk:17-jdk-slim
EXPOSE 8080
COPY --from=build /home/app/build/libs/CvAdvisorPlatform-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]