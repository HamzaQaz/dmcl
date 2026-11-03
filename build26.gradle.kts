plugins {
    id("net.fabricmc.fabric-loom") version "1.19.3"
    id("java")
    id("com.gradleup.shadow") version "9.2.1"
    id("com.modrinth.minotaur") version "2.8.7"
}

val modId: String by project
val modVersion: String by project
val minecraftVersion: String by project
val loaderVersion: String by project
val fabricVersion: String by project
val javaVersion: String by project
val compat: String by project

base.archivesName.set("$modId-fabric-$minecraftVersion")
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
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    shade("net.dv8tion:JDA:5.2.1") {
        exclude(module = "opus-java")
    }
    shade("org.xerial:sqlite-jdbc:3.46.1.3")
    shade("com.electronwill.night-config:toml:3.8.1")
    shade("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.20.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
}

sourceSets.main { java.srcDir("src/compat-$compat/java") }

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dnet.bytebuddy.experimental=true")
    testLogging { events("passed", "skipped", "failed") }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mod_id", modId)
    inputs.property("mc_version", minecraftVersion)
    inputs.property("java_version", javaVersion)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version, "mod_id" to modId, "mc_version" to minecraftVersion, "java_version" to javaVersion))
    }
}

tasks.shadowJar {
    
    configurations = listOf(shade)
    relocate("net.dv8tion.jda", "com.n1ghtlydev.dmcl.shaded.jda")
    relocate("com.electronwill.nightconfig", "com.n1ghtlydev.dmcl.shaded.nightconfig")
    relocate("org.sqlite", "com.n1ghtlydev.dmcl.shaded.sqlite")
    mergeServiceFiles()
    minimize {
        exclude(dependency("org.xerial:sqlite-jdbc:.*"))
    }
}

tasks.shadowJar { archiveClassifier.set("") }
tasks.jar { archiveClassifier.set("slim") }

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion.toInt())
}

tasks.register<Copy>("installMod") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    val target = file(providers.gradleProperty("dmclInstallDir").orElse("").get().ifEmpty { "/nonexistent" })
    onlyIf { target.isDirectory }
    into(target)
    doLast {
        logger.lifecycle("Installed dmcl jar to ${'$'}target")
    }
}

// Opt-in: ./gradlew build -PdmclInstallDir=/path/to/server/mods
tasks.build {
    finalizedBy(tasks.named("installMod"))
}

// ---- Modrinth publishing ----
// Run with: MODRINTH_TOKEN=... ./gradlew modrinth
modrinth {
    token.set(System.getenv("MODRINTH_TOKEN") ?: "")
    projectId.set(System.getenv("MODRINTH_PROJECT_ID") ?: "v2ecfuiA")
    versionNumber.set(modVersion)
    versionType.set("release")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(listOf("1.21.1", "1.21.2", "1.21.3"))
    loaders.add("fabric")
    dependencies {
        required.project("fabric-api")
    }
    syncBodyFrom.set(rootProject.file("README.md").readText())
    changelog.set(
        "First public build of DMCL. See README for setup. Compatible with Minecraft 1.21.1 through 1.21.3."
    )
}
