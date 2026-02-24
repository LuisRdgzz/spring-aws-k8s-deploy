FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/car-catalog-api-0.0.1-SNAPSHOT.jar /app/car-catalog-api.jar

ENV DB_HOST=localhost \
    DB_PORT=5432 \
    DB_NAME=car_catalog \
    DB_USERNAME=postgres \
    SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "car-catalog-api.jar"]