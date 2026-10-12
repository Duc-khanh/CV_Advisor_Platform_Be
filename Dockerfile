# Bước 1: Dùng Gradle image để build dự án
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/app
WORKDIR /home/app
RUN chmod +x ./gradlew
RUN ./gradlew clean build

# Bước 2: Dùng Eclipse Temurin (JDK 17 Alpines) siêu nhẹ và cực kỳ ổn định
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
EXPOSE 8080
COPY --from=build /home/app/build/libs/CvAdvisorPlatform-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /home/app/config ./config
COPY --from=build /home/app/.prompts ./.prompts
COPY --from=build /home/app/SYSTEM_PROMPT.md ./SYSTEM_PROMPT.md
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
