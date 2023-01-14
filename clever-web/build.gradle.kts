dependencies {
    api(project(":clever-core"))
    api("io.javalin:javalin")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
}
