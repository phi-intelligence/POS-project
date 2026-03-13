FROM gradle:8.7-jdk21 AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8087

ENTRYPOINT ["java","-jar","/app/app.jar"]

