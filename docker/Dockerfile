# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn ./.mvn
# Permessi di esecuzione per il wrapper maven e fix line endings (CRLF -> LF)
RUN chmod +x ./mvnw && sed -i 's/\r$//' ./mvnw
RUN ./mvnw package -DskipTests

# Stage 2: Runtime (immagine minimale)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
