FROM azul/zulu-openjdk-alpine:21-latest
WORKDIR /app

COPY build/libs/*-SNAPSHOT.jar app.jar

RUN apk add --no-cache curl imagemagick libheif

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]