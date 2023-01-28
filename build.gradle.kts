import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import java.util.*

val localProperty = Properties().apply {
    val local = project.rootProject.file("local.properties")
    if (local.exists()) {
        local.inputStream().use { this.load(it) }
    }
}

val buildVersion = System.getenv("buildVersion") ?: project.properties["buildVersion"]
val buildSnapshot: Boolean =
    (System.getenv("buildSnapshot") ?: project.properties["buildSnapshot"] as String).toBoolean()

object Ver {
    const val springBootVersion = "2.6.14"
    const val springCloudVersion = "2021.0.5"
    const val javalinVersion = "4.6.7"
    const val kotlinVersion = "1.6.21"
    const val kotlinxCoroutinesVersion = "1.6.4"
    const val groovyVersion = "4.0.8"
    const val antlr4Version = "4.9.3"
    const val jmhVersion = "1.36"
    const val querydslVersion = "5.0.0"
    const val schemacrawlerVersion = "16.16.18"
    const val poiVersion = "5.2.3"
}

buildscript {
    repositories {
        mavenLocal()
        maven("https://maven.aliyun.com/nexus/content/groups/public/")
        mavenCentral()
    }
    dependencies {
        classpath("org.apache.commons:commons-lang3:3.12.0")
        classpath("commons-io:commons-io:2.11.0")
    }
}

plugins {
    idea
    `java-library`
    `maven-publish`
    id("io.spring.dependency-management").version("1.0.12.RELEASE")
    id("org.springframework.boot").version("2.6.12").apply(false)
    id("org.jetbrains.kotlin.jvm").version("1.6.21").apply(false)
}

idea {
    project {
        jdkName = "1.8"
        languageLevel = IdeaLanguageLevel("1.8")
    }
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

allprojects {
    apply(plugin = "java-library")

    group = "org.clever"
    version = "${buildVersion}-${if (buildSnapshot) "SNAPSHOT" else "RELEASE"}"

    repositories {
        mavenLocal()
        maven(url = "https://maven.aliyun.com/nexus/content/groups/public/")
        mavenCentral()
    }

    // 默认是24小时，gradle会检查一次依赖，可以设置每次build都进行检查
    configurations.all {
        // resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }

    java.sourceCompatibility = JavaVersion.VERSION_1_8
    java.targetCompatibility = JavaVersion.VERSION_1_8

    tasks.compileJava {
        options.encoding = "UTF-8"
        // options.isWarnings = false
        // options.isDeprecation = true
        options.compilerArgs.add("-parameters")
    }
}

subprojects {
    apply(plugin = "idea")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "io.spring.dependency-management")

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${Ver.springBootVersion}")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${Ver.springCloudVersion}")
            mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${Ver.kotlinxCoroutinesVersion}")
        }

        dependencies {
            // performance test
            dependency("org.openjdk.jmh:jmh-core:${Ver.jmhVersion}")
            dependency("org.openjdk.jmh:jmh-generator-annprocess:${Ver.jmhVersion}")
            // javax
            dependency("javax.servlet:javax.servlet-api:4.0.1")
            dependency("javax.inject:javax.inject:1")
            dependency("javax.persistence:persistence-api:1.0.2")
            dependency("jakarta.persistence:jakarta.persistence-api:2.2.3")
            dependency("com.google.code.findbugs:jsr305:3.0.2")
            dependency("org.glassfish:javax.el:3.0.0")
            // validation
            dependency("javax.validation:validation-api:2.0.1.Final")
            dependency("org.hibernate.validator:hibernate-validator:6.2.5.Final")
            // time
            dependency("joda-time:joda-time:2.10.14")
            dependency("org.joda:joda-convert:2.2.2")
            // jdbc
            dependency("p6spy:p6spy:3.9.1")
            dependency("com.oracle.database.jdbc:ojdbc8:21.6.0.0.1")
            dependency("com.oracle.database.nls:orai18n:21.3.0.0")
            dependency("org.postgresql:postgresql:42.3.6")
            dependency("mysql:mysql-connector-java:8.0.30")
            dependency("com.microsoft.sqlserver:mssql-jdbc:11.2.1.jre8")
            // apache commons
            dependency("commons-io:commons-io:2.11.0")
            dependency("org.apache.commons:commons-text:1.9")
            dependency("org.apache.commons:commons-email:1.5")
            dependency("commons-beanutils:commons-beanutils:1.9.4")
            // http相关
            dependency("com.squareup.okhttp3:okhttp:4.9.3")
            dependency("com.squareup.retrofit2:converter-jackson:2.9.0")
            dependency("com.squareup.retrofit2:retrofit:2.9.0")
            // json、xml相关
            dependency("org.json:json:20210307")
            dependency("com.alibaba:fastjson:1.2.78")
            dependency("com.thoughtworks.xstream:xstream:1.4.19")
            // 反射相关
            dependency("net.jodah:typetools:0.6.3")
            dependency("cglib:cglib:3.3.0")
            dependency("org.reflections:reflections:0.10.2")
            // javalin
            dependency("io.javalin:javalin:${Ver.javalinVersion}")
            dependency("io.javalin:javalin-bundle:${Ver.javalinVersion}")
            // kotlin
            dependency("org.jetbrains.kotlin:kotlin-stdlib-common:${Ver.kotlinVersion}")
            dependency("org.jetbrains.kotlin:kotlin-stdlib:${Ver.kotlinVersion}")
            dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Ver.kotlinVersion}")
            dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Ver.kotlinVersion}")
            dependency("org.jetbrains.kotlin:kotlin-reflect:${Ver.kotlinVersion}")
            // groovy
            dependency("org.apache.groovy:groovy-all:${Ver.groovyVersion}")
            dependency("org.apache.groovy:groovy:${Ver.groovyVersion}")
            // 验证码
            dependency("com.github.cage:cage:1.0")
            dependency("com.github.axet:kaptcha:0.0.9")
            dependency("com.github.bingoohuang:patchca:0.0.1")
            // antlr4
            dependency("org.antlr:antlr4-runtime:${Ver.antlr4Version}")
            dependency("org.antlr:antlr4:${Ver.antlr4Version}")
            // excel读写
            dependency("org.apache.poi:poi:${Ver.poiVersion}")
            dependency("org.apache.poi:poi-ooxml:${Ver.poiVersion}")
            dependency("org.apache.poi:poi-ooxml:${Ver.poiVersion}")
            dependency("org.apache.poi:poi-ooxml-schemas:${Ver.poiVersion}")
            dependency("com.alibaba:easyexcel:3.1.1")
            // querydsl
            dependency("com.querydsl:querydsl-core:${Ver.querydslVersion}")
            dependency("com.querydsl:querydsl-sql:${Ver.querydslVersion}")
            dependency("com.querydsl:querydsl-jpa:${Ver.querydslVersion}")
            dependency("com.querydsl:querydsl-apt:${Ver.querydslVersion}")
            dependency("com.querydsl:querydsl-codegen:${Ver.querydslVersion}")
            dependency("com.querydsl:querydsl-sql-codegen:${Ver.querydslVersion}")
            // jwt
            dependency("io.jsonwebtoken:jjwt-api:0.11.5")
            dependency("io.jsonwebtoken:jjwt-impl:0.11.5")
            dependency("io.jsonwebtoken:jjwt-jackson:0.11.5")
            // schemacrawler
            dependency("us.fatehi:schemacrawler:${Ver.schemacrawlerVersion}")
            dependency("us.fatehi:schemacrawler-commandline:${Ver.schemacrawlerVersion}")
            dependency("us.fatehi:schemacrawler-postgresql:${Ver.schemacrawlerVersion}")
            dependency("us.fatehi:schemacrawler-oracle:${Ver.schemacrawlerVersion}")
            dependency("us.fatehi:schemacrawler-sqlserver:${Ver.schemacrawlerVersion}")
            dependency("us.fatehi:schemacrawler-mysql:${Ver.schemacrawlerVersion}")
            // 其他工具包
            dependency("org.jetbrains:annotations:23.0.0")
            dependency("com.google.guava:guava:31.0.1-jre")
            dependency("com.google.zxing:javase:3.4.1")
            dependency("ognl:ognl:3.2.21")
            dependency("com.github.jsqlparser:jsqlparser:4.4")
            dependency("com.jfinal:enjoy:4.9.21")
            dependency("com.belerweb:pinyin4j:2.5.1")
            dependency("org.ow2.asm:asm:7.1")
            dependency("org.apache.commons:commons-math3:3.6.1")
            dependency("org.javassist:javassist:3.28.0-GA")
        }
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")
        testImplementation("org.openjdk.jmh:jmh-core")
        testImplementation("org.openjdk.jmh:jmh-generator-annprocess")
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    tasks.withType<Copy>().all {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    tasks.withType<Jar>().all {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    tasks.javadoc {
        options.encoding = "UTF-8"
        (options as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    tasks.jar {
        manifest {
            attributes["provider"] = "gradle"
        }
    }

    // tasks.test {
    //     useJUnitPlatform()
    // }

    publishing {
        repositories {
            maven {
                setUrl("https://nexus.msvc.top/repository/maven-${if (buildSnapshot) "snapshots" else "releases"}/")
                credentials {
                    username = localProperty["NEXUS_USERNAME"] as String?
                    password = localProperty["NEXUS_PASSWORD"] as String?
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }
                from(components["java"])
                pom {
                    name.set("clever-app library")
                    description.set("clever-app library")
                }
            }
        }
    }
}

tasks.jar {
    enabled = false
}
