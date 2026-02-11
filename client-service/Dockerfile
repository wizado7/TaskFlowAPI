FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY client-service/pom.xml /app/client-service/pom.xml
COPY client-service/src /app/client-service/src
RUN mvn -f /app/client-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/client-service/target/client-service*.jar /app/client-service.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/client-service.jar"]
