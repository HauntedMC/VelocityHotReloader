import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    checkstyle
    jacoco
    id("com.gradleup.shadow") version "9.4.1"
    id("net.kyori.blossom") version "2.2.0"
}

group = "nl.hauntedmc.velocityhotreloader"
val dependencyDir = "${group}.velocity.dependencies"
version = "1.2.1"

val javaVersion = 21
val checkstyleVersion = "13.3.0"

java {
    java.toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

checkstyle {
    toolVersion = checkstyleVersion
    configFile = file("config/checkstyle/checkstyle.xml")
    isShowViolations = true
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
    compileOnly("com.mojang:brigadier:1.0.500")
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    compileOnly("com.electronwill.night-config:toml:3.8.4")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-junit-jupiter:5.20.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    testImplementation("com.electronwill.night-config:toml:3.8.4")
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", version.toString())
            }
        }
    }
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

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    test {
        useJUnitPlatform()
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

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.withType<JacocoReport>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
