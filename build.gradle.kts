plugins {
    id("java")
}

group = "com.qtpc.tech.nolmax.server"
version = "1.0.4-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "cutiepcRepoReleases"
        url = uri("https://maven.qtpc.tech/releases")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("io.netty:netty-all:4.2.12.Final")
    implementation("com.google.protobuf:protobuf-java:4.34.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.1.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")
    implementation("com.nolmax.database:Nolmax_chat:1.3.18-SNAPSHOT")
    implementation("com.qtpc.tech.nolmax:packet:1.0.7")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.qtpc.tech.nolmax.server.Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}