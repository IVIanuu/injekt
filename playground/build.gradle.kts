plugins {
    kotlin("jvm")
    kotlin("kapt")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")

dependencies {
    implementation(project(":injekt"))
    kapt(project(":injekt-compiler"))
}