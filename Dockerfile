FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Seoul

COPY build/libs/note-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.main.log-startup-info=true", "-jar", "/app/app.jar"]
