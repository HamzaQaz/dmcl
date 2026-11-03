plugins {
    id("java")
    id("net.neoforged.moddev") version "2.0.148"
    id("com.gradleup.shadow") version "9.2.1"
}

val minecraftVersion: String by project
val neoVersion: String by project
val compat: String by project
val javaVersion: String by project
val modVersion: String = file("../gradle.properties").readLines().first { it.startsWith("modVersion") }.substringAfter("=").trim()

group = "com.n1ghtlydev"
version = modVersion
base.archivesName.set("dmcl-neoforge-$minecraftVersion")

java { toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt())) }

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
}

neoForge { version = neoVersion }

val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

dependencies {
    shade("net.dv8tion:JDA:5.2.1") { exclude(module = "opus-java"); exclude(group = "org.slf4j") }
    shade("org.xerial:sqlite-jdbc:3.46.1.3")
    shade("com.electronwill.night-config:toml:3.8.1")

    testImplementation("org.slf4j:slf4j-api:2.0.13")
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.20.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
}

// Shared code lives in the root project: platform-free core plus the vanilla adapter (adapter/mc).
sourceSets {
    main {
        java {
            srcDir("../src/main/java")
            srcDir("../src/compat-$compat/java")
            exclude("**/adapter/fabric/**", "**/mixin/**", "**/DmclMod.java")
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
    jvmArgs("-Dnet.bytebuddy.experimental=true")
    testLogging { events("failed") }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion.toInt())
}

tasks.processResources {
    val props = mapOf("version" to modVersion, "mc_version" to minecraftVersion, "neo_version" to neoVersion)
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}

tasks.jar { archiveClassifier.set("slim") }
tasks.shadowJar {
    archiveClassifier.set("")
    configurations = listOf(shade)
    from(sourceSets.main.get().output)
    relocate("net.dv8tion.jda", "com.n1ghtlydev.dmcl.shaded.jda")
    relocate("com.electronwill.nightconfig", "com.n1ghtlydev.dmcl.shaded.nightconfig")
    relocate("org.sqlite", "com.n1ghtlydev.dmcl.shaded.sqlite")
    mergeServiceFiles()
    minimize { exclude(dependency("org.xerial:sqlite-jdbc:.*")) }
}
tasks.build { dependsOn(tasks.shadowJar) }
