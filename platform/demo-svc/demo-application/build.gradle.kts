plugins {
    id("ofbiz-msa.java-library")
}

dependencies {
    api(project(":platform:demo-svc:demo-domain"))
    testImplementation(project(":platform:arch-rules"))
}
