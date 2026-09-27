FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/ ./backend/
RUN mvn -f backend/pom.xml -pl api-gateway -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/backend/api-gateway/target/api-gateway-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]