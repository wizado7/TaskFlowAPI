FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY task-service/pom.xml /app/task-service/pom.xml
COPY task-service/src /app/task-service/src
RUN mvn -f /app/task-service/pom.xml -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --home-dir /app app
COPY --from=build /app/task-service/target/task-service*.jar /app/task-service.jar
RUN mkdir -p /app/uploads/task-attachments && chown -R app:app /app
USER app
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/task-service.jar"]
