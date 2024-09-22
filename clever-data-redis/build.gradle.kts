dependencies {
    api(project(":clever-core"))
    api(project(":clever-data-commons"))
    api("org.apache.commons:commons-pool2")
    api("io.lettuce:lettuce-core")
    api("org.redisson:redisson")
    // api("de.ruedigermoeller:fst")
    api("org.springframework.data:spring-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
}
