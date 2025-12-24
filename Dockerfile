FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
COPY application.yaml application.yaml
COPY application-common.yaml application-common.yaml
COPY application-note.yaml application-note.yaml
COPY application-gateway.yaml application-gateway.yaml
COPY application-platform.yaml application-platform.yaml
ENV SPRING_CONFIG_ADDITIONAL_LOCATION=/app/
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
