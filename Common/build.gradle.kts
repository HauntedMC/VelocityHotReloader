import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.kyori.blossom") version "1.3.0"
}

group = rootProject.group
version = "${rootProject.version}"
base {
    archivesName.set("${rootProject.name}-Common")
}

tasks {
    blossom {
        replaceToken("{version}", version, "src/main/java/nl/hauntedmc/velocityhotreloaded/common/VHRApp.java")
    }
}

tasks.withType<ShadowJar> {
    exclude("plugin.yml")
}
