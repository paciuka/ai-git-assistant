# =============================================================================
# AI Git Assistant — Multi-Stage Dockerfile
# =============================================================================
# Multi-stage builds are a Docker best practice. They separate the BUILD
# environment (which needs Maven, JDK, and all build tools) from the
# RUNTIME environment (which only needs the JRE and the compiled JAR).
#
# Benefits:
#   - Final image is much smaller (no Maven, no source code, no build cache)
#   - Reduced attack surface (fewer tools = fewer vulnerabilities)
#   - Faster pulls and deploys
# =============================================================================

# ─── Stage 1: BUILD ─────────────────────────────────────────────────────────
# Use the official Maven image with JDK 17 to compile and package the app.
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the POM first (dependency caching optimization).
# Docker caches layers. If pom.xml hasn't changed, Docker reuses the
# cached dependency layer — making subsequent builds much faster.
COPY pom.xml .

# Download all dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Now copy the source code
COPY src ./src

# Build the fat JAR (skipping tests for the Docker build)
RUN mvn package -DskipTests -B

# ─── Stage 2: RUNTIME ───────────────────────────────────────────────────────
# Use a minimal JRE-only image for the final container.
# eclipse-temurin is the official successor to AdoptOpenJDK.
FROM eclipse-temurin:17-jre-alpine

# Install git — our application needs it to run `git diff --staged`
RUN apk add --no-cache git

WORKDIR /repo

# Copy ONLY the compiled fat JAR from the builder stage.
# Everything else (Maven, source code, .m2 cache) is discarded.
COPY --from=builder /app/target/ai-git-assistant-1.0.0.jar /app/ai-git-assistant.jar

# The API key is passed at runtime via environment variable.
# We declare it here as documentation — it does NOT set a default value.
# The container will fail fast with a clear error if it's not provided.
ENV ANTHROPIC_API_KEY=""

# ENTRYPOINT defines the command that runs when the container starts.
# The user's repository will be mounted at /repo (see README for usage).
ENTRYPOINT ["java", "-jar", "/app/ai-git-assistant.jar"]
