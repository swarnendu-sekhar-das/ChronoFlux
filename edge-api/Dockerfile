FROM maven:3.9-eclipse-temurin-21 as build
WORKDIR /app
COPY pom.xml .
COPY . .
RUN mvn -pl edge-api clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/edge-api/target/edge-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
