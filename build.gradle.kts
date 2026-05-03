plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
}

val modId: String by project
val modVersion: String by project
val minecraftVersion: String by project
val yarnMappings: String by project
val loaderVersion: String by project
val fabricVersion: String by project
val javaVersion: String by project

base.archivesName.set(modId)
group = project.property("group").toString()
version = modVersion

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    shade("net.dv8tion:JDA:5.2.1") {
        exclude(module = "opus-java")
    }
    shade("org.xerial:sqlite-jdbc:3.46.1.3")
    shade("com.electronwill.night-config:toml:3.8.1")
    shade("org.slf4j:slf4j-api:2.0.16")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mod_id", modId)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version, "mod_id" to modId))
    }
}

tasks.shadowJar {
    archiveClassifier.set("dev-shadow")
    configurations = listOf(shade)
    relocate("net.dv8tion.jda", "com.westwardmc.dmcl.shaded.jda")
    relocate("com.electronwill.nightconfig", "com.westwardmc.dmcl.shaded.nightconfig")
    relocate("org.sqlite", "com.westwardmc.dmcl.shaded.sqlite")
    mergeServiceFiles()
    minimize {
        exclude(dependency("org.xerial:sqlite-jdbc:.*"))
    }
}

tasks.remapJar {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion.toInt())
}
