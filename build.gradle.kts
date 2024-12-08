import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    `java-library`
    id("nom.lombok")
    id("nom.lib")
}

repositories {
    mavenCentral()
    
    maven {
        url = uri("https://nexus.velocitypowered.com/repository/maven-public/")
    }
}

dependencies {
    // These JARs are so old that they aren't on any repository, so include them locally
    compileOnly(files("libs/spigot-1.8.8.jar"))
    compileOnly(files("libs/craftbukkit-1.8.8.jar"))
    
    // JSON dependencies
    api("com.fasterxml.jackson.core:jackson-core:2.15.2")
    api("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    
    compileOnly("org.apache.logging.log4j:log4j-api:2.22.0")
    
    compileOnly("commons-io:commons-io:2.13.0")
    implementation("io.github.classgraph:classgraph:4.8.110") // for reflection
    
    implementation("com.github.zafarkhaja:java-semver:0.9.0")
}

group = "com.mcctf"
version = "2.0.1"
description = "CTF"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.named<Jar>("jar") {
    manifest {
        archiveVersion.set("2.0.1")
        archiveBaseName.set("mcctf")
        attributes["CTF-Version"] = "2.0.1"
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("mcctf")
    archiveVersion.set("2.0.1")
    destinationDirectory.set(file("server/plugins"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
