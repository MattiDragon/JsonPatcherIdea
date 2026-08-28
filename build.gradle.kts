import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

val languageServerConfiguration = configurations.register("languageServer") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

repositories {
    mavenLocal()
    maven("https://jitpack.io") {
        content {
            includeGroupAndSubgroups("dev.mattidragon")
            includeGroupAndSubgroups("com.github")
        }
    }
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    languageServerConfiguration("dev.mattidragon.jsonpatcher:JsonPatcher-Lang-Server:2.0.0-beta.9:all")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2026.2.0.1")
        testFramework(TestFrameworkType.Platform)

        bundledPlugin("org.jetbrains.plugins.textmate")
    }
}

tasks.processResources {
    from(languageServerConfiguration.get().singleFile) {
        rename { "server.jar" }
    }
}
tasks.patchChangelog {

}
tasks.patchPluginXml {

}