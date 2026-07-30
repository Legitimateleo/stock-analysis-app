# --- Build stage ---
FROM public.ecr.aws/docker/library/maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Run stage ---
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/parallax-backend-1.0-SNAPSHOT.jar app.jar

# FINNHUB_API_KEY and FRONTEND_ORIGIN are injected at run time
# (docker-compose env_file, or the cloud provider's secret/env config) —
# never baked into the image.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
