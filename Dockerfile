FROM maven:3.9.16-amazoncorretto-25 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jdk-alpine AS jlink-builder
WORKDIR /jlink
COPY --from=builder /build/target/events-manager.jar events-manager.jar

RUN jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    --multi-release 25 \
    events-manager.jar > modules.txt \
 && jlink \
    --add-modules $(cat modules.txt) \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /custom-jre

FROM alpine:3.21
LABEL org.opencontainers.image.authors="willianfaes"
ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jlink-builder /custom-jre $JAVA_HOME
WORKDIR /app
COPY --from=builder /build/target/*.jar events-manager.jar
ENTRYPOINT ["java", "-jar", "events-manager.jar"]