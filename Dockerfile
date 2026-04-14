# Build stage
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.10_7_1.10.1_3.4.2 AS build

WORKDIR /app

# Copy project configuration
COPY project/*.sbt project/
COPY project/build.properties project/
COPY build.sbt .

# Pre-fetch dependencies
RUN sbt update

# Copy source code and resources
COPY src ./src

# Build the assembly fat jar (skipping tests for build efficiency)
RUN sbt "set test in assembly := {}" assembly

# Run stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the assembly jar from build stage
COPY --from=build /app/target/scala-3.3.3/wifi-cdmx-ap-assembly-0.1.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
# We use shell form to allow environment variable expansion
ENTRYPOINT java -Ddb.url=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME} \
                -Ddb.user=${DB_USER} \
                -Ddb.pass=${DB_PASS} \
                -Ddb.driver=org.postgresql.Driver \
                -jar app.jar
