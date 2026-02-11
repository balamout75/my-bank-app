FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

USER root
RUN apk add --no-cache curl
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]