FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY user-service/pom.xml /app/user-service/pom.xml
COPY user-service/src /app/user-service/src
RUN mvn -f /app/user-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --home-dir /app app
COPY --from=build /app/user-service/target/user-service*.jar /app/user-service.jar
RUN mkdir -p /app/uploads/user-avatars && chown -R app:app /app
USER app
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/user-service.jar"]
