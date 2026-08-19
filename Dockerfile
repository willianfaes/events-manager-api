FROM maven:3.9.16-amazoncorretto-25 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
LABEL org.opencontainers.image.authors="willianfaes"
WORKDIR /app
COPY --from=builder /build/target/*.jar events-manager.jar
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "events-manager.jar"]