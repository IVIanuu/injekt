plugins {
    kotlin("jvm")
    kotlin("kapt")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/mvn-publish.gradle")

dependencies {
    api(Deps.processingX)
    kapt(Deps.processingX)
    compileOnly(Deps.Kotlin.compiler)
    api(Deps.Kotlin.stdlib)
}