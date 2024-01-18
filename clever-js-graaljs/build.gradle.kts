dependencies {
    api(project(":clever-js-api"))
    api("org.graalvm.truffle:truffle-api")
    api("org.graalvm.sdk:graal-sdk")
    runtimeOnly("org.graalvm.js:js")
    testRuntimeOnly("org.graalvm.compiler:compiler")
    testRuntimeOnly("org.graalvm.js:js-scriptengine")
    testRuntimeOnly("org.graalvm.tools:profiler")
    testRuntimeOnly("org.graalvm.tools:chromeinspector")
}
