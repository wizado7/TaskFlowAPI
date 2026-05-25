FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY api-gateway/pom.xml /app/api-gateway/pom.xml
COPY api-gateway/src /app/api-gateway/src
RUN mvn -f /app/api-gateway/pom.xml -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --home-dir /app app
COPY --from=build /app/api-gateway/target/api-gateway*.jar /app/api-gateway.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/api-gateway.jar"]
