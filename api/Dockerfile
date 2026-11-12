# Multi-stage build for the Hygiene Tracker API.
# Stage 1 compiles the jar with the full JDK; stage 2 runs it on a JRE only, so the final image
# has no compiler, Gradle or source code. Configuration and secrets come from environment
# variables at run time, never from the image.

# ---- Build -------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Gradle wrapper and build files first, so dependencies are cached until they change.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

# Tests are skipped here: CI runs them in its own job, and they need Docker (Testcontainers),
# which isn't available inside an image build.
COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
    && find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} app.jar \;

# ---- Run ---------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user instead of root.
RUN useradd --system --uid 1001 --no-create-home app
COPY --from=build /workspace/app.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
