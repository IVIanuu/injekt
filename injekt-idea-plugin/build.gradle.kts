plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.4.18"
}

dependencies {
    compile(project(":injekt-compiler-plugin", "shadow"))
}

intellij {
    version = "2019.3.4"
    pluginName = "Intellij idea plugin"
    setPlugins("org.jetbrains.kotlin:1.3.61-release-IJ2019.3-1")
}