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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

springBoot {
    buildInfo()
}
