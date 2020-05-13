plugins {
    kotlin("jvm")
    kotlin("kapt")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-lint.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/mvn-publish.gradle")

dependencies {
    implementation(Deps.processingX)
    kapt(Deps.processingX)
    compileOnly(Deps.Kotlin.compilerEmbeddable)
    implementation(Deps.Kotlin.stdlib)

    testImplementation(project(":injekt-core"))
    testImplementation(Deps.junit)

    // todo remove compile testing deps
    testImplementation("com.squareup.okio:okio:2.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:1.3.70")
    testImplementation("io.github.classgraph:classgraph:4.8.64")
    testImplementation(Deps.Kotlin.compilerEmbeddable)
}
