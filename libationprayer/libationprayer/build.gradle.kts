plugins {
    id("java")
    kotlin("jvm")
}

group = "org.powbot.community.libationprayer"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.powbot.org/releases")
}

dependencies {
    implementation("org.powbot:client-sdk:3+")
    implementation("org.powbot:client-sdk-loader:3+")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(11)
}

tasks.jar {
    archiveBaseName.set("Libationprayer")
}
