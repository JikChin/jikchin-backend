FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

RUN apk add --no-cache curl imagemagick libheif

COPY build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]