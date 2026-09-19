FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Seoul

COPY search-content/build/libs/search-content.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
