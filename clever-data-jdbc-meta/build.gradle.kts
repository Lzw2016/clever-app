dependencies {
    api(project(":clever-data-jdbc"))
    // api("us.fatehi:schemacrawler")
    // api("us.fatehi:schemacrawler-postgresql")
    api("com.jfinal:enjoy")
    testImplementation("org.postgresql:postgresql")
    testImplementation("mysql:mysql-connector-java")
}
