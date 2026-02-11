FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

ARG MODULE=accounts-service

COPY . .

RUN mvn -B -DskipTests -pl ${MODULE} -am clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl \
 && addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ARG MODULE=accounts-service
COPY --from=builder /workspace/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]