import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.kyori.blossom") version "1.3.0"
}

group = "${rootProject.group}"
val dependencyDir = "${group}.velocity.dependencies"
version = rootProject.version
base {
    archivesName.set("${rootProject.name}-Velocity")
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }

    maven("https://libraries.minecraft.net")
}

dependencies {
    implementation("org.incendo:cloud-velocity:${VersionConstants.cloudMinecraftVersion}")
    implementation("net.kyori:adventure-text-minimessage:4.26.1") {
        exclude("net.kyori", "adventure-api")
    }
    implementation(project(":Common"))
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-brigadier:1.0.0-SNAPSHOT")
    compileOnly("com.electronwill.night-config:toml:3.6.3")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
}

tasks {
    blossom {
        replaceToken("{version}", version, "src/main/java/nl/hauntedmc/velocityhotreloaded/velocity/VHR.java")
    }
}
