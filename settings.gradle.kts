pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        gradlePluginPortal()
    }
    plugins {
        id("io.spring.dependency-management").version("1.1.6")
        id("org.springframework.boot").version("3.3.4")
        id("org.jetbrains.kotlin.jvm").version("2.0.20")
    }
}

rootProject.name = "clever-app"
include("clever-spring")
include("clever-core")
//include("clever-groovy")
//include("clever-js-api")
//include("clever-js-graaljs")
//include("clever-js-nashorn")
//include("clever-js-v8")
include("clever-data-dynamic-sql")
include("clever-data-commons")
include("clever-data-jdbc")
include("clever-data-jdbc-meta")
include("clever-data-redis")
//include("clever-data-rabbitmq")
include("clever-web")
//include("clever-security")
//include("clever-task")
//include("clever-task-ext")
include("clever-boot")
