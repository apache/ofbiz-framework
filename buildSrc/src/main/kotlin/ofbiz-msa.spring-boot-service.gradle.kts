// ofbiz-msa.spring-boot-service — runnable Spring Boot 3.3 service module
// (adapters/infrastructure). Applies dependency management + common service
// dependencies (OTel, actuator). Virtual threads on by default in
// application.yaml (FUTURESTATE-LOWLEVEL §1).
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.67.0"))
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

}

// Security overrides above the Boot 3.3 BOM (trivy CI gate, 2026-10):
// Boot 3.3.x EOL leaves tomcat/jackson/micrometer behind; pin the fixed
// versions until the program moves to a supported Boot line. These use the
// dependency-management DSL — BOM imports beat Gradle forces/constraints.
configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
    dependencies {
        dependency("org.apache.tomcat.embed:tomcat-embed-core:10.1.55")
        dependency("org.apache.tomcat.embed:tomcat-embed-el:10.1.55")
        dependency("org.apache.tomcat.embed:tomcat-embed-websocket:10.1.55")
        dependency("com.fasterxml.jackson.core:jackson-core:2.18.11")
        dependency("com.fasterxml.jackson.core:jackson-databind:2.18.11")
        dependency("com.fasterxml.jackson.core:jackson-annotations:2.18.11")
        dependency("io.micrometer:micrometer-core:1.15.12")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

springBoot {
    buildInfo()
}
