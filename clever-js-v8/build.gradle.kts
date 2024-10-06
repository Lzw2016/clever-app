import org.gradle.internal.os.OperatingSystem

val os: OperatingSystem = OperatingSystem.current()
val cpuArch: String = System.getProperty("os.arch")

dependencies {
    api(project(":clever-js-api"))
    if (os.isMacOsX) {
        // MacOS (x86_64 and arm64)
        api("com.caoccao.javet:javet-macos")
    } else if (os.isLinux && (cpuArch == "aarch64" || cpuArch == "arm64")) {
        // Linux (arm64)
        api("com.caoccao.javet:javet-linux-arm64")
    } else {
        // Linux and Windows (x86_64)
        api("com.caoccao.javet:javet")
    }
}
