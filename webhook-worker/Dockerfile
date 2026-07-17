FROM maven:3.9-eclipse-temurin-21 as build
WORKDIR /app
COPY pom.xml .
COPY . .
RUN mvn -pl webhook-worker clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/webhook-worker/target/webhook-worker-0.0.1-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]
