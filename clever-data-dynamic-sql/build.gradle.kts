dependencies {
    compileOnly("org.slf4j:slf4j-api")
    api("ognl:ognl")
    api("org.antlr:antlr4-runtime")
    api("org.apache.commons:commons-lang3")
    testImplementation("commons-io:commons-io")
    testImplementation("ch.qos.logback:logback-classic")
}

tasks.processResources {
    from("src/main/java") {
        include(listOf("**/*.dtd", "**/*.xsd"))
    }
}
