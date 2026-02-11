FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY task-service/pom.xml /app/task-service/pom.xml
COPY task-service/src /app/task-service/src
RUN mvn -f /app/task-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/task-service/target/task-service*.jar /app/task-service.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/task-service.jar"]
