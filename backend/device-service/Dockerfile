FROM maven:3.9-eclipse-temurin-21 AS build
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV CATALINA_OPTS="-Duser.timezone=UTC"

COPY ./pom.xml /device-service/pom.xml
WORKDIR /device-service
RUN mvn dependency:go-offline

COPY ./src/ /device-service/src
WORKDIR /device-service
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /device-service
COPY --from=build /device-service/target/device-service-0.0.1-SNAPSHOT.jar /device-service/device-service.jar
EXPOSE 8082
CMD ["java", "-jar", "device-service.jar"]