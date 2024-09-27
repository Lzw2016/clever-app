dependencies {
    api(project(":clever-web"))
    api(project(":clever-data-jdbc"))
    api(project(":clever-data-redis"))
    api("io.jsonwebtoken:jjwt-api")
    api("io.jsonwebtoken:jjwt-impl")
    api("io.jsonwebtoken:jjwt-jackson")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
