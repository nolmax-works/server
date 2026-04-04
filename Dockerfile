FROM gradle:9.4.1-jdk21 AS builder

WORKDIR /app
COPY gradle ./gradle
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY src ./src
RUN gradle build --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -g 1000 -S nolmax && adduser -u 1000 -S nolmax -G nolmax
RUN chown nolmax:nolmax /app
COPY --from=builder --chown=nolmax:nolmax /app/build/libs/*.jar /app/server.jar
USER nolmax:nolmax
ENTRYPOINT ["java", "-jar", "/app/server.jar"]