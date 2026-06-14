plugins {
    java
    id("com.gradleup.shadow") version "8.3.0" // Shadow plugin
}

val pluginName: String by project
val repoUrl: String by project
val developerId: String by project
val developerName: String by project
val pluginVersion: String = project.version.toString()

repositories {
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    implementation(project(":core-api"))
    compileOnly("io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.12.2")
    implementation("com.zaxxer:HikariCP:5.1.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveBaseName.set(pluginName)
    archiveVersion.set(pluginVersion)
    archiveClassifier.set("")

    manifest {
        attributes(
            "Implementation-Title" to pluginName,
            "Implementation-Version" to pluginVersion,
            "Implementation-Vendor" to developerName,
            "License" to "MIT"
        )
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set(pluginName)
    archiveVersion.set(pluginVersion)
    archiveClassifier.set("")
    relocate("com.zaxxer.hikari", "org.yuemi.bingogacha.plugin.lib.hikari")
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}
