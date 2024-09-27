dependencies {
    api(project(":clever-data-jdbc"))
    compileOnly(project(":clever-web"))
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.postgresql:postgresql")
    testImplementation("mysql:mysql-connector-java")
    testImplementation(project(":clever-data-redis"))
}
