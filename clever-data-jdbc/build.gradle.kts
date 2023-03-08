dependencies {
    api(project(":clever-core"))
    api(project(":clever-data-commons"))
    api(project(":clever-data-dynamic-sql"))
    api("com.zaxxer:HikariCP")
    api("com.querydsl:querydsl-core")
    api("com.querydsl:querydsl-sql")
    api("com.github.jsqlparser:jsqlparser")
    api("p6spy:p6spy")
    testImplementation("org.postgresql:postgresql")
    testImplementation("mysql:mysql-connector-java")
    // runtimeOnly("com.oracle.database.jdbc:ojdbc8")
    // runtimeOnly("com.microsoft.sqlserver:mssql-jdbc")
}
