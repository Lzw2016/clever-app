dependencies {
    api(project(":clever-js-api"))
    api("org.graalvm.truffle:truffle-api")
    api("org.graalvm.sdk:graal-sdk")
    api("org.graalvm.js:js-scriptengine")
    runtimeOnly("org.graalvm.js:js")
    runtimeOnly("org.graalvm.tools:profiler")
    runtimeOnly("org.graalvm.tools:chromeinspector")
}
