FROM maven:3.9-eclipse-temurin-21 AS build
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV CATALINA_OPTS="-Duser.timezone=UTC"

COPY ./pom.xml /api-gateway/pom.xml
WORKDIR /api-gateway
RUN mvn dependency:go-offline

COPY ./src/ /api-gateway/src
WORKDIR /api-gateway
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /api-gateway
COPY --from=build /api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar /api-gateway/api-gateway.jar
EXPOSE 8080
CMD ["java", "-jar", "api-gateway.jar"]