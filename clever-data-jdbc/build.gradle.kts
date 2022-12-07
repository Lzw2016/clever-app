dependencies {
    api(project(":clever-core"))
    api(project(":clever-data-dynamic-sql"))
    api("com.zaxxer:HikariCP")
    api("com.querydsl:querydsl-core")
    api("com.querydsl:querydsl-sql")
    api("com.github.jsqlparser:jsqlparser")
    api("p6spy:p6spy")
    // api("com.jfinal:enjoy")
    api("org.springframework.boot:spring-boot-starter-jdbc")
    // testImplementation("org.postgresql:postgresql")
    // runtimeOnly("mysql:mysql-connector-java")
    // runtimeOnly("com.oracle.database.jdbc:ojdbc8")
    // runtimeOnly("com.microsoft.sqlserver:mssql-jdbc")
}

sourceSets {
    main {
        resources {
            srcDirs("src/main/resources", "src/main/java")
        }
    }
}
