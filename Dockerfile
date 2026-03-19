# Multi-stage Dockerfile for Scala/SBT project

# Build stage
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.10_7_1.10.1_3.4.2 AS build

WORKDIR /app

# Copy project configuration
COPY project/build.properties project/
COPY build.sbt .

# Pre-fetch dependencies
RUN sbt update

# Copy source code and resources
COPY src ./src

# Compile the project
RUN sbt compile

# Run stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# We'll use sbt to run it as well for simplicity since we don't have sbt-assembly/sbt-native-packager configured
# However, for a more "standard" approach, we can use sbt to build and then copy dependencies.
# Given the "no changes" constraint, we'll keep sbt in the final image or use a slightly different approach.

# Let's try to make it work with just the JRE.
# To do that we would need a Fat JAR. Since we can't add plugins easily, we'll use a slightly larger image with sbt for now.
# This ensures it works without any changes to your project.

FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.10_7_1.10.1_3.4.2

WORKDIR /app

COPY --from=build /app /app
COPY --from=build /root/.sbt /root/.sbt
COPY --from=build /root/.cache /root/.cache

# Expose port
EXPOSE 8080

# Run the application
# We use shell form to allow environment variable expansion from docker-compose
ENTRYPOINT sbt -Ddb.url=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME} \
               -Ddb.user=${DB_USER} \
               -Ddb.pass=${DB_PASS} \
               run
