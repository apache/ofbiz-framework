// ofbiz-msa.grpc — protobuf + gRPC stub codegen (east-west contracts).
// Proto sources: src/main/proto (published contracts live in platform/contracts/proto).
plugins {
    java
    id("com.google.protobuf")
}

dependencies {
    implementation(platform("io.grpc:grpc-bom:1.66.0"))
    implementation("io.grpc:grpc-services")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("com.google.protobuf:protobuf-java:3.25.5")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.66.0"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}
