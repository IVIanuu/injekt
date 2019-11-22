plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.4.9"
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")

intellij {
    pluginName = "injekt"
    //version = "2019.2"
    setPlugins("org.jetbrains.kotlin:1.3.60-release-IJ2019.3-1")
}

dependencies {
    implementation(project(":injekt-compiler-hosted"))
}