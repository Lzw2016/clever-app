dependencies {
    api(project(":clever-js-api"))
    api("org.graalvm.truffle:truffle-api")
    api("org.graalvm.polyglot:polyglot")
    api("org.graalvm.polyglot:js")
    testRuntimeOnly("org.graalvm.polyglot:inspect")
    testRuntimeOnly("org.graalvm.polyglot:profiler")
}
