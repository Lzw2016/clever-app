FROM eclipse-temurin:17.0.12_7-jre-alpine AS task
COPY clever-boot/build/libs/ /app/
WORKDIR /app
SHELL ["/bin/bash", "-c"]
ENTRYPOINT exec java ${JAVA_MEM_OPTS} ${DATABASE_OPTS} ${JAVA_OPTS_EXT} ${JAVA_ADD_OPENS} -jar /app/clever-boot-1.0.1-SNAPSHOT.jar --spring.profiles.active=${ACTIVE_PROFILES} --web.port=8080
EXPOSE 8080


FROM container-registry.oracle.com/graalvm/jdk:17 AS task_graalvm
COPY clever-boot/build/libs/ /app/
WORKDIR /app
ENV LANG=C.UTF-8
SHELL ["/bin/sh", "-c"]
ENTRYPOINT exec java ${JAVA_MEM_OPTS} ${DATABASE_OPTS} ${JAVA_OPTS_EXT} ${JAVA_ADD_OPENS} -jar /app/clever-boot-1.0.1-SNAPSHOT.jar --spring.profiles.active=${ACTIVE_PROFILES} --web.port=8080
EXPOSE 8080
