FROM eclipse-temurin:21-jre
WORKDIR /app

COPY build/libs/note-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dlogging.level.root=DEBUG", "-Dspring.main.log-startup-info=true", "-Ddebug=true", "-jar", "/app/app.jar"]
