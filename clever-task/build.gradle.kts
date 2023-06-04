dependencies {
    api(project(":clever-data-jdbc"))
    compileOnly(project(":clever-web"))
    testImplementation("org.postgresql:postgresql")
    testImplementation("mysql:mysql-connector-java")
    testImplementation(project(":clever-data-redis"))
}
