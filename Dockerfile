# -------------------------------------------------------------
# Stage 1: Build application with Maven and Eclipse Temurin JDK 25
# -------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure maven wrapper is executable
RUN chmod +x mvnw

# Download dependencies offline (cached layer unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy application source code
COPY src/ ./src/

# Package the Spring Boot JAR (skip tests during container build)
RUN ./mvnw clean package -DskipTests -B

# -------------------------------------------------------------
# Stage 2: Minimal runtime image with Eclipse Temurin JRE 25
# -------------------------------------------------------------
FROM eclipse-temurin:25-jre AS runner
WORKDIR /app

# Create a non-root user for enhanced container security
RUN (userdel -r ubuntu 2>/dev/null || true) && \
    (useradd -m appuser || true) && \
    chown -R appuser:appuser /app
USER appuser

# Copy the generated executable JAR from builder stage
COPY --from=builder --chown=appuser:appuser /app/target/*.jar app.jar

# Render assigns port dynamically via $PORT environment variable (defaults to 10000)
ENV PORT=10000
ENV SESSION_COOKIE_SAMESITE=none
ENV SESSION_COOKIE_SECURE=true
EXPOSE 10000

# Run Spring Boot with container-aware RAM settings
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
