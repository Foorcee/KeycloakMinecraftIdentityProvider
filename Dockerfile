FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM quay.io/keycloak/keycloak:26.4

COPY --from=builder /build/target/*.jar /opt/keycloak/providers/
