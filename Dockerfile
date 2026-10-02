# ================================
# ARMIET CanteenFlow
# Spring Boot + Java 25
# ================================

# ---------- BUILD STAGE ----------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy Maven project files
COPY pom.xml .

# Copy source code
COPY src ./src

# Build Spring Boot application
RUN mvn clean package -DskipTests


# ---------- RUN STAGE ----------
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the generated Spring Boot JAR
COPY --from=build /app/target/*.jar app.jar

# Spring Boot default port
EXPOSE 8080

# Render provides the PORT environment variable.
# Use 8080 locally if PORT is not provided.
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]