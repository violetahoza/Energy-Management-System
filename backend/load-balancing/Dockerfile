FROM maven:3.9-eclipse-temurin-21 AS build
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV CATALINA_OPTS="-Duser.timezone=UTC"

COPY ./pom.xml /load-balancing/pom.xml
WORKDIR /load-balancing
RUN mvn dependency:go-offline

COPY ./src/ /load-balancing/src
WORKDIR /load-balancing
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /load-balancing
COPY --from=build /load-balancing/target/load-balancing-0.0.1-SNAPSHOT.jar /load-balancing/load-balancing.jar
EXPOSE 8086
CMD ["java", "-jar", "load-balancing.jar"]