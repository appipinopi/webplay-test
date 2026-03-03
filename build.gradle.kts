plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "com.bluejoin.addon"
version = "1.0"

repositories {
    mavenCentral()
    maven ( "https://repo.bluecolored.de/releases" )
}

dependencies {
    compileOnly ( "de.bluecolored:bluemap-api:2.7.6" )
    compileOnly ( "de.bluecolored:bluemap-common:5.13" )
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "utf-8"
}

tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}
