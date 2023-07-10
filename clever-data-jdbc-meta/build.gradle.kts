//plugins {
//    groovy
//    id("org.jetbrains.kotlin.jvm")
//}

dependencies {
    api(project(":clever-data-jdbc"))
    // api("us.fatehi:schemacrawler")
    // api("us.fatehi:schemacrawler-postgresql")
    api("com.jfinal:enjoy")
    api("com.querydsl:querydsl-codegen")
    // api("org.apache.groovy:groovy")
    // api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.postgresql:postgresql")
    testImplementation("mysql:mysql-connector-java")
    testImplementation("com.oracle.database.jdbc:ojdbc8:23.2.0.0")
}
