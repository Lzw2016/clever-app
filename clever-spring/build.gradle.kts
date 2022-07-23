dependencies {
    api("org.yaml:snakeyaml")
    api("org.slf4j:slf4j-api")
    api("org.slf4j:jul-to-slf4j")
    api("ch.qos.logback:logback-classic")
    compileOnlyApi("org.jetbrains.kotlin:kotlin-stdlib")
    compileOnlyApi("org.jetbrains.kotlin:kotlin-reflect")
    compileOnlyApi("javax.money:money-api")
    compileOnlyApi("joda-time:joda-time")
    compileOnlyApi("org.joda:joda-convert")
    testImplementation("commons-io:commons-io")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
}
