plugins {
    id("ofbiz-msa.spring-boot-service")
}

dependencies {
    implementation(project(":platform:demo-svc:demo-application"))
    implementation(project(":platform:demo-svc:demo-domain"))
}
