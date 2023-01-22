dependencies {
    api(project(":clever-core"))
    api(project(":clever-data-jdbc"))
    api("io.javalin:javalin")
    api("javax.validation:validation-api:2.0.1.Final")
    api("org.hibernate.validator:hibernate-validator")
    api("org.glassfish:javax.el")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
}
