FROM maven:3.9-eclipse-temurin-21 AS build
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV CATALINA_OPTS="-Duser.timezone=UTC"

COPY ./pom.xml /authorization-service/pom.xml
WORKDIR /authorization-service
RUN mvn dependency:go-offline

COPY ./src/ /authorization-service/src
WORKDIR /authorization-service
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /authorization-service
COPY --from=build /authorization-service/target/authorization-service-0.0.1-SNAPSHOT.jar /authorization-service/authorization-service.jar
EXPOSE 8083
CMD ["java", "-jar", "authorization-service.jar"]