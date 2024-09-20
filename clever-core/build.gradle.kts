dependencies {
    api(project(":clever-spring"))
    api("org.apache.commons:commons-lang3")
    api("commons-io:commons-io")
    api("org.apache.commons:commons-text")
    api("commons-codec:commons-codec")
    api("com.google.guava:guava")
    api("com.squareup.okhttp3:okhttp")
    api("org.json:json")
    api("cglib:cglib")
    api("jakarta.xml.bind:jakarta.xml.bind-api")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-joda")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    // 可选依赖
    compileOnly("com.squareup.retrofit2:converter-jackson")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("org.hibernate.validator:hibernate-validator")
    compileOnly("com.google.zxing:javase")
    compileOnly("com.belerweb:pinyin4j")
    compileOnly("com.github.cage:cage")
    compileOnly("com.github.axet:kaptcha")
    compileOnly("com.github.bingoohuang:patchca")
    // 单元测试
    testImplementation("org.hibernate.validator:hibernate-validator")
}
