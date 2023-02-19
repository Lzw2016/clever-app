import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    //id("groovy")
    id("org.jetbrains.kotlin.jvm")
    //id("org.springframework.boot")
}

dependencies {
    api(project(":clever-web"))
    api(project(":clever-security"))
    api(project(":clever-data-jdbc"))
    api(project(":clever-data-redis"))
    // api(project(":clever-groovy"))
    // api("org.jetbrains.kotlin:kotlin-stdlib-common")
    // api("org.jetbrains.kotlin:kotlin-stdlib")
    // api("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
    // api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    // api("org.jetbrains.kotlin:kotlin-reflect")

    api("org.postgresql:postgresql")
    api("mysql:mysql-connector-java")
}

sourceSets {
    main {
        // java {
        //     setSrcDirs(listOf<String>())
        // }
        // withConvention(GroovySourceSet::class) {
        //     groovy {
        //         setSrcDirs(listOf("src/main/java", "src/main/groovy"))
        //     }
        // }
        resources {
            setSrcDirs(listOf("src/main/java", "src/main/kotlin", "src/main/resources"))
            exclude(listOf("**/*.java", "**/*.kt", "**/*.kts"))
        }
    }
}

//tasks.compileJava {
//    enabled = false
//}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

// 拷贝lib文件
tasks.create("copyJar", Copy::class) {
    delete("$buildDir/libs/lib")
    from(configurations.runtimeClasspath)
    into("$buildDir/libs/lib")
}

// 拷贝配置文件
tasks.create("copyResources", Copy::class) {
    delete("$buildDir/libs/config")
    from("src/main/resources")
    into("$buildDir/libs/config")
}

// 配置启动jar
tasks.jar {
    enabled = true
    manifest.attributes["Main-Class"] = "org.clever.app.StartApp"
    val classPaths =
        project.configurations.runtimeClasspath.get().files.map { file -> "lib/${file.name}" }.toMutableList()
    val resourcesPath = File(projectDir.absolutePath, "src/main/resources").absolutePath
    file("src/main/resources").listFiles { file -> file.isFile }?.forEach { file ->
        var path = file.absolutePath
        if (path.startsWith(resourcesPath)) {
            path = path.substring(resourcesPath.length + 1)
            path = path.replace('\\', '/')
        }
        classPaths.add("config/$path")
    }
    manifest.attributes["Class-Path"] = classPaths.joinToString(" ")
    // println("### @# [${classPaths.joinToString(" ")}]")
}

tasks.getByName("build") {
    dependsOn("copyJar")
    dependsOn("copyResources")
    dependsOn("jar")
}
