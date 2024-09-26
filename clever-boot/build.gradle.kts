import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.text.SimpleDateFormat
import java.util.*

plugins {
    //groovy
    id("org.jetbrains.kotlin.jvm")
    //id("org.springframework.boot")
}

dependencies {
    api(project(":clever-spring"))
    api(project(":clever-core"))
    api(project(":clever-data-jdbc"))
    api(project(":clever-data-redis"))
    api(project(":clever-web"))
    api(project(":clever-security"))
    // api(project(":clever-groovy"))
//    api(project(":clever-task"))
//    api(project(":clever-task-ext"))
    // api("org.jetbrains.kotlin:kotlin-stdlib-common")
    // api("org.jetbrains.kotlin:kotlin-stdlib")
    // api("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
    // api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    // api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.postgresql:postgresql")
    api("mysql:mysql-connector-java")
    api("com.oracle.database.jdbc:ojdbc8")
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
    }
}

//tasks.compileJava {
//    enabled = false
//}

kotlin {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_0
        jvmTarget = JvmTarget.JVM_1_8
        javaParameters = true
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// 在源码目录中引入资源(如: mybatis xml文件)
tasks.processResources {
    val includes = listOf("**/*.xml")
    from("src/main/java") {
        include(includes)
    }
    from("src/main/kotlin") {
        include(includes)
    }
}

// 拷贝lib文件
tasks.register("copyJar", Copy::class) {
    val libDir = layout.buildDirectory.dir("libs/lib").get()
    delete(libDir)
    from(configurations.runtimeClasspath)
    into(libDir)
}

// 拷贝配置文件
tasks.register("copyResources", Copy::class) {
    val configDir = layout.buildDirectory.dir("libs/config").get()
    delete(configDir)
    from("src/main/resources")
    into(configDir)
}

// 配置启动jar
tasks.jar {
    enabled = true
    manifest.attributes["Main-Class"] = "org.clever.app.StartApp"
    // lib/jar 加入 classPath
    val classPaths = project.configurations.runtimeClasspath.get().files.map { file -> "lib/${file.name}" }.toMutableList()
    // // resources 资源加入 classPath (当没有把resources资源编译进jar包时很有用)
    // val resourcesFile = File(projectDir.absolutePath, "src/main/resources")
    // val resourcesPath = resourcesFile.absolutePath
    // resourcesFile.listFiles { file -> file.isFile }?.forEach { file ->
    //     var path = file.absolutePath
    //     if (path.startsWith(resourcesPath)) {
    //         path = path.substring(resourcesPath.length + 1)
    //         path = path.replace('\\', '/')
    //     }
    //     classPaths.add("config/$path")
    // }
    manifest.attributes["Class-Path"] = classPaths.joinToString(" ")
    // println("### [${classPaths.joinToString(" ")}]")
}

// 触发class热部署
tasks.getByName("classes") {
    doLast {
        File("./build").mkdirs()
        File("./build", ".hotReload").apply {
            if (this.exists()) {
                this.createNewFile()
            }
            this.appendText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())}\n")
            println("> [触发class热部署] -> ${this.absolutePath}")
        }
    }
}

tasks.getByName("build") {
    dependsOn("copyJar")
    dependsOn("copyResources")
    dependsOn("jar")
}
