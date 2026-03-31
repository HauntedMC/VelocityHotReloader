import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("io.github.goooler.shadow") version "8.1.7"
    id("net.kyori.blossom") version "1.3.0"
}

group = "nl.hauntedmc.velocityhotreloader"
val dependencyDir = "${group}.velocity.dependencies"
version = "1.2.0"

val javaVersion = 21

java {
    java.toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    implementation("net.kyori:adventure-text-minimessage:4.26.1") {
        exclude("net.kyori", "adventure-api")
    }
    implementation("com.google.code.gson:gson:2.13.2")
    compileOnly("com.mojang:brigadier:1.0.18")
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    compileOnly("com.electronwill.night-config:toml:3.8.4")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
}

tasks {
    blossom {
        replaceToken("{version}", version, "src/main/java/nl/hauntedmc/velocityhotreloader/VelocityHotReloaded.java")
    }

    build {
        dependsOn("shadowJar")
    }

    compileJava {
        options.release.set(javaVersion)
        options.encoding = Charsets.UTF_8.name()
        options.isDeprecation = true
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    jar {
        enabled = false
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    relocate("com.google.gson", "${dependencyDir}.gson")
    relocate("net.kyori.adventure.text.minimessage", "${dependencyDir}.adventure.text.minimessage")
}
