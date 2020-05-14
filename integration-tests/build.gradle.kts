plugins {
    kotlin("jvm")
    kotlin("kapt")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-lint.gradle")

dependencies {
    implementation(Deps.processingX)
    kapt(Deps.processingX)

    implementation(project(":injekt-compiler-plugin"))
    implementation(project(":injekt-core"))
    implementation(project(":injekt-common"))

    implementation(Deps.junit)

    // todo remove compile testing deps
    implementation("com.squareup.okio:okio:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:1.3.70")
    implementation("io.github.classgraph:classgraph:4.8.64")
    implementation(Deps.Kotlin.compilerEmbeddable)
}