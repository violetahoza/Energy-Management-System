FROM maven:3.9-eclipse-temurin-21 AS build
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV CATALINA_OPTS="-Duser.timezone=UTC"

COPY ./pom.xml /websocket-service/pom.xml
WORKDIR /websocket-service
RUN mvn dependency:go-offline

COPY ./src/ /websocket-service/src
WORKDIR /websocket-service
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /websocket-service
COPY --from=build /websocket-service/target/websocket-service-0.0.1-SNAPSHOT.jar /websocket-service/websocket-service.jar
EXPOSE 8086
CMD ["java", "-jar", "websocket-service.jar"]