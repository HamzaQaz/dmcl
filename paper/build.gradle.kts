plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.1"
}

val modVersion: String = file("../gradle.properties").readLines().first { it.startsWith("modVersion") }.substringAfter("=").trim()

group = "com.n1ghtlydev"
version = modVersion
base.archivesName.set("dmcl-paper")

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

dependencies {
    // API-only: Paper's plugin API is stable across 1.21 - 26.x, so one jar covers them all.
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    shade("net.dv8tion:JDA:5.2.1") { exclude(module = "opus-java") }
    shade("org.xerial:sqlite-jdbc:3.46.1.3")
    shade("com.electronwill.night-config:toml:3.8.1")
    compileOnly("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
    testImplementation("org.slf4j:slf4j-api:2.0.16")
}

// Shared, platform-free code lives in the root project's src/.
sourceSets {
    main {
        java {
            srcDir("../src/main/java")
            exclude("**/adapter/fabric/**", "**/adapter/mc/**", "**/mixin/**", "**/DmclMod.java")
        }
        resources {
            srcDir("../src/main/resources")
            exclude("fabric.mod.json", "dmcl.mixins.json")
        }
    }
    test { java.srcDir("../src/test/java") }
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("failed") }
}

tasks.processResources {
    inputs.property("version", version)
    filesMatching("plugin.yml") { expand(mapOf("version" to version)) }
}

tasks.shadowJar {
    archiveClassifier.set("")
    configurations = listOf(shade)
    relocate("net.dv8tion.jda", "com.n1ghtlydev.dmcl.shaded.jda")
    relocate("com.electronwill.nightconfig", "com.n1ghtlydev.dmcl.shaded.nightconfig")
    relocate("org.sqlite", "com.n1ghtlydev.dmcl.shaded.sqlite")
    mergeServiceFiles()
    minimize { exclude(dependency("org.xerial:sqlite-jdbc:.*")) }
}

tasks.build { dependsOn(tasks.shadowJar) }
