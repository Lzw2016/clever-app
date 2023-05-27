dependencies {
    api(project(":clever-data-jdbc"))
    testImplementation("org.postgresql:postgresql")
    testImplementation("mysql:mysql-connector-java")
    testImplementation(project(":clever-data-redis"))
}
