import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.7"
    id("net.kyori.blossom") version "1.3.0"
}

group = "nl.hauntedmc.velocityhotreloader"
val dependencyDir = "${group}.velocity.dependencies"
version = "1.1.0"

val javaVersion = 21

java {
    java.toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

base {
    archivesName.set(rootProject.name)
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://libraries.minecraft.net")
}

dependencies {
    implementation("net.kyori:adventure-text-minimessage:4.26.1") {
        exclude("net.kyori", "adventure-api")
    }
    testImplementation("net.kyori:adventure-text-serializer-plain:${VersionConstants.adventureVersion}")
    implementation("com.google.code.gson:gson:2.8.6")
    compileOnly("com.mojang:brigadier:1.0.18")
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    compileOnly("com.electronwill.night-config:toml:3.6.3")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    implementation("net.kyori:adventure-text-serializer-gson:${VersionConstants.adventureVersion}") {
        exclude("net.kyori", "adventure-api")
        exclude("com.google.code.gson", "gson")
    }
}

tasks {
    blossom {
        replaceToken("{version}", version, "src/main/java/nl/hauntedmc/velocityhotreloader/VHR.java")
    }

    clean {
        dependsOn("cleanJars")
    }

    build {
        dependsOn("shadowJar")
    }

    compileJava {
        options.release.set(javaVersion)
        options.encoding = Charsets.UTF_8.name()
        options.isDeprecation = true
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
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
    destinationDirectory.set(file("jars"))
    exclude("com/mojang/**")
    exclude("javax/annotation/**")
    exclude("org/checkerframework/**")
    exclude("plugin.yml")
    relocate("com.google.gson", "${dependencyDir}.gson")
    relocate("net.kyori.adventure.text.minimessage", "${dependencyDir}.adventure.text.minimessage")
    relocate("net.kyori.adventure.text.serializer.gson", "${dependencyDir}.impl.adventure.text.serializer.gson")
}

tasks.register("cleanJars") {
    delete(file("jars"))
}
