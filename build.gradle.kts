// Wave Motorcycle - Paper 26.2 plugin
// Build: ./gradlew build
// Outputs:
//   build/libs/WaveMotorcycle-<version>.jar       (the plugin)
//   build/resourcepacks/wave-motorcycle-pack-<version>.zip (the resource pack)

plugins {
    java
}

group = "com.wavemotorcycle"
version = "1.0.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    mavenCentral()
}

dependencies {
    // Paper 26.2 API (latest 26.2 build; the server provides it at runtime).
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveBaseName.set("WaveMotorcycle")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // The resource pack sources live under resources/ for development; they are
    // shipped as a separate ZIP (see buildResourcePack) instead of inside the jar.
    exclude("resourcepack/**")
}

// ---------------------------------------------------------------------------
// Resource pack assembly
// ---------------------------------------------------------------------------

val resourcePackDir = layout.projectDirectory.dir("src/main/resources/resourcepack")
val resourcePackOut = layout.buildDirectory.dir("resourcepacks")

tasks.register<Zip>("buildResourcePack") {
    group = "build"
    description = "Assembles the Wave Motorcycle resource pack as a distributable ZIP."
    archiveFileName.set("wave-motorcycle-pack-${project.version}.zip")
    destinationDirectory.set(resourcePackOut)
    from(resourcePackDir)
    // pack.mcmeta must sit at the ZIP root
    include("pack.mcmeta")
    include("assets/**")
}

tasks.build {
    dependsOn("buildResourcePack")
}
