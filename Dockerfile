FROM adoptopenjdk/openjdk11:alpine-slim
ARG JAR_FILE=build/libs/journal-1.0.0-SNAPSHOT.jar
WORKDIR /opt/service
COPY ${JAR_FILE} journal-1.0.0-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","journal-1.0.0-SNAPSHOT.jar"]