FROM maven:3.9-eclipse-temurin-21 AS build
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV CATALINA_OPTS="-Duser.timezone=UTC"

COPY ./pom.xml /monitoring-service/pom.xml
WORKDIR /monitoring-service
RUN mvn dependency:go-offline

COPY ./src/ /monitoring-service/src
WORKDIR /monitoring-service
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /monitoring-service
COPY --from=build /monitoring-service/target/monitoring-service-0.0.1-SNAPSHOT.jar /monitoring-service/monitoring-service.jar
EXPOSE 8084
CMD ["java", "-jar", "monitoring-service.jar"]