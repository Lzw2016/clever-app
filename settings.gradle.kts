pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        gradlePluginPortal()
    }
    plugins {
        id("io.spring.dependency-management").version("1.0.12.RELEASE")
        id("org.springframework.boot").version("2.6.12")
        id("org.jetbrains.kotlin.jvm").version("1.6.21")
    }
}

rootProject.name = "clever-app"
include("clever-spring")
include("clever-core")
include("clever-web")
include("clever-security")
include("clever-groovy")
include("clever-data-commons")
include("clever-data-dynamic-sql")
include("clever-data-jdbc")
include("clever-data-jdbc-meta")
include("clever-data-redis")
include("clever-data-rabbitmq")
include("clever-task")
include("clever-task-ext")
include("clever-boot")
