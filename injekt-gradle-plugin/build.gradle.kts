plugins {
    id("java-gradle-plugin")
    kotlin("jvm")
    kotlin("kapt")
    id("de.fuerstenau.buildconfig")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-lint.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/mvn-publish.gradle")

gradlePlugin {
    plugins {
        create("injektPlugin") {
            id = "com.ivianuu.injekt"
            implementationClass = "com.ivianuu.injekt.gradle.InjektGradlePlugin"
        }
    }
}

buildConfig {
    clsName = "BuildConfig"
    packageName = "com.ivianuu.injekt.gradle"

    version = Publishing.version
    buildConfigField("String", "GROUP_ID", Publishing.groupId)
    buildConfigField("String", "ARTIFACT_ID", "injekt-compiler-plugin")
}

dependencies {
    implementation(Deps.androidGradlePlugin)
    implementation(Deps.Kotlin.gradlePluginApi)
    implementation(Deps.processingX)
    kapt(Deps.processingX)
}