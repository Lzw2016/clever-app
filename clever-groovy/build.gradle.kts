apply(plugin = "groovy")

dependencies {
    api(project(":clever-core"))
    // api(project(":clever-web"))
    api("org.apache.groovy:groovy")
}

tasks.compileJava {
    enabled = false
}
