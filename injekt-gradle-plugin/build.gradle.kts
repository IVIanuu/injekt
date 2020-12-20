/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("java-gradle-plugin")
    kotlin("jvm")
    kotlin("kapt")
    id("de.fuerstenau.buildconfig")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
//apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-lint.gradle")
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
    implementation(Deps.Kotlin.gradlePlugin)
    implementation(Deps.Kotlin.gradlePluginApi)
    implementation(Deps.processingX)
    kapt(Deps.processingX)
}