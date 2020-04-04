import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-lint.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/mvn-publish.gradle")

dependencies {
    compile(Deps.processingX)
    kapt(Deps.processingX)
    compileOnly(Deps.Kotlin.compilerEmbeddable)
    compile(Deps.Kotlin.stdlib)
}

tasks.withType<ShadowJar> {
    configurations = listOf(
        project.configurations.getByName("compile"),
        project.configurations.getByName("compileOnly")
    )
    relocate("org.jetbrains.kotlin.com.intellij", "com.intellij")
    relocate("org.jetbrains.kotlin.load", "kotlin.reflect.jvm.internal.impl.load")
}
