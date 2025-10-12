import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.*

plugins {
    id("java")
}

group = "com.serveur.moba"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // TODO : Mettre à jour la version de l'API Paper plus tard
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(23))
}

sourceSets {
    main {
        resources.srcDir("src/main/resources")
    }
}

tasks.processResources {
    // évite l’erreur “duplicate entry”
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
