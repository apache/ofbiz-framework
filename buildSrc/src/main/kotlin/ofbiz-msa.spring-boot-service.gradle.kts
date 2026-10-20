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

// Security overrides one patch ahead of the Boot 4.0.6 BOM (trivy gate,
// 2026-10). Drop entries as the BOM catches up.
configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
    dependencies {
        dependency("org.apache.tomcat.embed:tomcat-embed-core:11.0.25")
        dependency("org.apache.tomcat.embed:tomcat-embed-el:11.0.25")
        dependency("org.apache.tomcat.embed:tomcat-embed-websocket:11.0.25")
        dependency("org.springframework:spring-expression:7.0.9")
        dependency("org.springframework:spring-webmvc:7.0.9")
        dependency("tools.jackson.core:jackson-core:3.1.7")
        dependency("tools.jackson.core:jackson-databind:3.1.7")
        dependency("io.micrometer:micrometer-core:1.16.6")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

springBoot {
    buildInfo()
}
