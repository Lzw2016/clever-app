dependencies {
    api(project(":clever-core"))
    api(project(":clever-data-commons"))
    api("org.apache.commons:commons-pool2")
    api("io.lettuce:lettuce-core")
    api("org.springframework.data:spring-data-redis")

    // https://www.jianshu.com/p/fabcee952e04
    // testImplementation("org.redisson:redisson:3.16.8")
    // testImplementation("org.redisson:redisson-spring-boot-starter:3.16.8")
}
