FROM openjdk:8u342-oracle AS task
COPY clever-boot/build/libs/ /app/
WORKDIR /app
ENTRYPOINT ["java", "${JAVA_MEM_OPTS}", "${DATABASE_OPTS}", "${JAVA_OPTS_EXT}", "-jar", "/app/clever-boot-0.0.1-SNAPSHOT.jar", "--clever.profiles.active=${APP_PROFILES}", "--web.port=8080"]
EXPOSE 8080
