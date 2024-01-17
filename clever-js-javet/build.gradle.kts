import org.gradle.internal.os.OperatingSystem

val os: OperatingSystem = OperatingSystem.current()
val cpuArch: String = System.getProperty("os.arch")

dependencies {
    api(project(":clever-js-api"))
    if (os.isMacOsX) {
        // Linux and Windows (x86_64)
        api("com.caoccao.javet:javet:3.0.2")
    } else if (os.isLinux && (cpuArch == "aarch64" || cpuArch == "arm64")) {
        // Linux (arm64)
        api("com.caoccao.javet:javet-linux-arm64:3.0.2")
    } else {
        // MacOS (x86_64 and arm64)
        api("com.caoccao.javet:javet-macos:3.0.2")
    }
}
