// ofbiz-msa.java-library — pure Java library module (domain, application).
// Java 21, JUnit 5, no frameworks (enforced by OfbizArchitectureRules).
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Every module carries the ArchUnit layering test (repo rule: enforced per module).
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
