dependencies {
    api(project(":clever-core"))
    api(project(":clever-data-jdbc"))
    api("io.javalin:javalin")
    api("org.springframework:spring-web")
    api("org.springframework:spring-webmvc")
    // api("jakarta.validation:jakarta.validation-api")
    // api("org.hibernate.validator:hibernate-validator")
    // api("org.glassfish:jakarta.el")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
}
