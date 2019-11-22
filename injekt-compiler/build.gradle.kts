import org.anarres.gradle.plugin.jarjar.JarjarTask

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.anarres.jarjar")
    id("maven-publish")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/mvn-publish.gradle")

configurations {
    register("jarFiles")
    register("embeddablePlugin")
}

dependencies {
    "jarFiles"(project(":injekt-compiler-hosted")) {
        isTransitive = false
    }
}

val embeddedPlugin = tasks.register<JarjarTask>("jarJar") {
    destinationName = "injekt-kotlin-compiler.jar"
    from(configurations.getByName("jarFiles"))
    classRename("com.intellij.**", "org.jetbrains.kotlin.com.intellij.@1")
}.get()

publishing {
    publications {
        getByName<MavenPublication>("MyPublication") {
            artifact(embeddedPlugin.destinationPath)
        }
    }
}

// todo
tasks.withType<GenerateMavenPom>().configureEach {
    dependsOn(embeddedPlugin)
}