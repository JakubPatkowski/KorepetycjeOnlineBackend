# ==========================================
# Stage 1: Build
# ==========================================
FROM maven:3.9-eclipse-temurin-22-alpine AS builder

WORKDIR /app

# Copy Maven files (for dependency caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (--enable-preview is configured in pom.xml)
RUN mvn clean package -DskipTests -B

# ==========================================
# Stage 2: Runtime
# ==========================================
FROM eclipse-temurin:22-jre-alpine

WORKDIR /app

# Create logs directory BEFORE switching to non-root user
RUN mkdir -p /app/logs

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Give spring user ownership of app directory (including logs)
RUN chown -R spring:spring /app

USER spring:spring

# Copy JAR from build stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run with preview features enabled
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]