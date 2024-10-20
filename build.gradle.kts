import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "nl.hauntedmc.velocityhotreloaded"
val dependencyDir = "${group}.dependencies"
version = "1.0.2"

val javaVersion = 21

java.toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.github.goooler.shadow")

    java.toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://libraries.minecraft.net")
    }

    dependencies {
        implementation("org.incendo:cloud-core:${VersionConstants.cloudVersion}")
        implementation("org.incendo:cloud-brigadier:${VersionConstants.cloudMinecraftVersion}")
        compileOnly("net.kyori:adventure-text-minimessage:${VersionConstants.adventureVersion}")
        testImplementation("net.kyori:adventure-text-serializer-plain:${VersionConstants.adventureVersion}")
        implementation("com.github.FrankHeijden:MinecraftReflection:1.0.0")
        implementation("com.google.code.gson:gson:2.8.6")
        compileOnly("com.mojang:brigadier:1.0.18")
    }

    tasks {
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
    }

    tasks.withType<ShadowJar> {
        exclude("com/mojang/**")
        exclude("javax/annotation/**")
        exclude("org/checkerframework/**")
        relocate("com.google.gson", "${dependencyDir}.gson")
        relocate("dev.frankheijden.minecraftreflection", "${dependencyDir}.minecraftreflection")
        relocate("cloud.commandframework", "${dependencyDir}.cloud")
        relocate("io.leangen.geantyref", "${dependencyDir}.typetoken")
        relocate("net.kyori.adventure.text.minimessage", "${dependencyDir}.adventure.text.minimessage")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Common", "shadow"))
    implementation(project(":Velocity", "shadow"))
    implementation("net.kyori:adventure-text-serializer-gson:${VersionConstants.adventureVersion}") {
        exclude("net.kyori", "adventure-api")
        exclude("com.google.code.gson", "gson")
    }
}

tasks {
    clean {
        dependsOn("cleanJars")
    }

    build {
        dependsOn("shadowJar", "copyJars")
    }
}

tasks.withType<ShadowJar> {
    relocate("net.kyori.adventure.text.serializer.gson", "${dependencyDir}.impl.adventure.text.serializer.gson")
}

fun outputTasks(): List<Task> {
    return listOf(
        ":Velocity:shadowJar",
    ).map { tasks.findByPath(it)!! }
}

tasks.register("cleanJars") {
    delete(file("jars"))
}

tasks.register<Copy>("copyJars") {
    outputTasks().forEach {
        from(it) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
    into(file("jars"))
    rename("(.*)-all.jar", "$1.jar")
}

