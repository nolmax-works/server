plugins {
    id("java")
    id("com.google.protobuf") version "0.9.6"
}

group = "com.qtpc.tech.nolmax.server"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("io.netty:netty-all:4.2.10.Final")
    implementation("com.google.protobuf:protobuf-java:4.34.0")
}

tasks.test {
    useJUnitPlatform()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.34.0"
    }
}