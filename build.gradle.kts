plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "com.example.exampleaddon"
version = "1.0"

repositories {
    mavenCentral()
    maven ( "https://repo.bluecolored.de/releases" )
}

dependencies {
    // enable only one of the below:
    compileOnly ( "de.bluecolored:bluemap-api:2.7.6" )
    //compileOnly ( "de.bluecolored:bluemap-core:5.13" )
    //compileOnly ( "de.bluecolored:bluemap-common:5.13" )

    // add more libraries if needed
    //implementation ( "some.example:library:1.0.0" )
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
