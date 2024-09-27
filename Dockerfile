FROM eclipse-temurin:17.0.12_7-jre-alpine AS task
COPY clever-boot/build/libs/ /app/
WORKDIR /app
#SHELL ["/bin/bash", "-c"]
SHELL ["/bin/sh", "-c"]
ENTRYPOINT exec java ${JAVA_MEM_OPTS} ${DATABASE_OPTS} ${JAVA_OPTS_EXT} -jar /app/clever-boot-1.0.1-SNAPSHOT.jar --spring.config.activate.on-profile=${SPRING_PROFILES} --web.port=8080
EXPOSE 8080
