// ofbiz-msa.contracts-test — dedicated source set (src/contractTest/java) for
// contract verification tests: generated gRPC stubs instantiate, Avro schemas
// load. Runs as part of `check`.
plugins {
    java
}

sourceSets {
    create("contractTest") {
        java.srcDir("src/contractTest/java")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

configurations {
    "contractTestImplementation" { extendsFrom(configurations["implementation"]) }
    "contractTestRuntimeOnly" { extendsFrom(configurations["runtimeOnly"]) }
}

val contractTestTask = tasks.register<Test>("contractTest") {
    description = "Runs contract verification tests."
    group = "verification"
    testClassesDirs = sourceSets["contractTest"].output.classesDirs
    classpath = sourceSets["contractTest"].runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn(contractTestTask)
}

dependencies {
    "contractTestImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
}
