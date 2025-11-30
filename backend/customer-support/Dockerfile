FROM maven:3.9-eclipse-temurin-21 AS build
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV CATALINA_OPTS="-Duser.timezone=UTC"

COPY ./pom.xml /customer-support/pom.xml
WORKDIR /customer-support
RUN mvn dependency:go-offline

COPY ./src/ /customer-support/src
WORKDIR /customer-support
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /customer-support
COPY --from=build /customer-support/target/customer-support-0.0.1-SNAPSHOT.jar /customer-support/customer-support.jar
EXPOSE 8085
CMD ["java", "-jar", "customer-support.jar"]