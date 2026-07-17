FROM maven:3.9-eclipse-temurin-21 as build
WORKDIR /app
COPY pom.xml .
COPY . .
RUN mvn -pl mock-game-server clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/mock-game-server/target/mock-game-server-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 9090
CMD ["java", "-jar", "app.jar"]
