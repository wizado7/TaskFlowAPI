FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY api-gateway/pom.xml /app/api-gateway/pom.xml
COPY api-gateway/src /app/api-gateway/src
RUN mvn -f /app/api-gateway/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/api-gateway/target/api-gateway*.jar /app/api-gateway.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/api-gateway.jar"]
