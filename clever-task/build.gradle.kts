dependencies {
    api(project(":clever-data-jdbc"))
    compileOnly(project(":clever-web"))
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.postgresql:postgresql")
    testImplementation("com.mysql:mysql-connector-j")
    testImplementation(project(":clever-data-redis"))
}
