FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY client-service/pom.xml /app/client-service/pom.xml
COPY client-service/src /app/client-service/src
RUN mvn -f /app/client-service/pom.xml -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --home-dir /app app
COPY --from=build /app/client-service/target/client-service*.jar /app/client-service.jar
RUN mkdir -p /app/uploads/client-attachments && chown -R app:app /app
USER app
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/client-service.jar"]
