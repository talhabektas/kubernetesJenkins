FROM openjdk:17-jdk-slim

ARG JAR_FILE=target/*.jar


COPY ${JAR_FILE} application.jar

EXPOSE 8061

ENTRYPOINT ["java", "-jar", "/application.jar"]