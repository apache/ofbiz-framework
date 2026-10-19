plugins {
    id("ofbiz-msa.java-library")
    id("ofbiz-msa.grpc")
    id("ofbiz-msa.contracts-test")
}

dependencies {
    implementation(project(":platform:demo-svc:demo-application"))
    implementation(project(":platform:demo-svc:demo-domain"))
}
