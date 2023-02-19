dependencies {
    api(project(":clever-core"))
    api(project(":clever-data-commons"))
    api("org.apache.commons:commons-pool2")
    api("io.lettuce:lettuce-core")
    api("org.redisson:redisson")

    testImplementation("org.springframework.data:spring-data-redis")
    // https://www.jianshu.com/p/fabcee952e04
    //testImplementation("org.redisson:redisson-spring-boot-starter:3.19.3")
    // implementation("de.ruedigermoeller:fst:2.57")
}
