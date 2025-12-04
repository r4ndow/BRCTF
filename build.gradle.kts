import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import org.yaml.snakeyaml.Yaml

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.yaml:snakeyaml:2.5")
    }
}

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow").version("6.1.0")
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
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

    // Logging
    compileOnly("org.apache.logging.log4j:log4j-api:2.22.0")

    // Plugin versioning
    implementation("com.github.zafarkhaja:java-semver:0.9.0")
}

// Read the version straight from the plugin.yml
val yaml: Map<String, Any> = Yaml().load(file("src/main/resources/plugin.yml").inputStream())
val pluginVersion = yaml["version"] as String
println("Detected plugin version $pluginVersion from plugin.yml")

group = "com.mcctf"
version = pluginVersion
description = "CTF"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.named<Jar>("jar") {
    manifest {
        archiveVersion.set(pluginVersion)
        archiveBaseName.set("mcctf")
        attributes["CTF-Version"] = pluginVersion
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("") // remove the -all suffix
    archiveBaseName.set("mcctf")
    archiveVersion.set(pluginVersion)

    doLast {
        copy {
            from(archiveFile)
            into("server/plugins")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
