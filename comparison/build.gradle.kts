/*
 * Copyright 2019 Manuel Wrage
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

/*
 * Copyright 2018 Manuel Wrage
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
    id("com.android.application")
    id("com.ivianuu.injekt")
    kotlin("android")
    kotlin("kapt")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8-android.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-kapt.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-lint.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-source-sets-android.gradle")

android {
    compileSdkVersion(Build.compileSdk)

    defaultConfig {
        applicationId = Build.applicationIdComparison
        buildToolsVersion = Build.buildToolsVersion
        minSdkVersion(Build.minSdkComparison)
        targetSdkVersion(Build.targetSdk)
        versionCode = Build.versionCode
        versionName = Build.versionName
    }
}

dependencies {
    implementation(Deps.androidxAppCompat)

    implementation(files("libs/dagger-1-shadowed.jar"))
    kapt(files("libs/dagger-1-compiler-shadowed.jar"))

    implementation(Deps.Dagger2.dagger2)
    kapt(Deps.Dagger2.compiler)

    implementation(Deps.dagger2Reflect)

    api(Deps.guava)

    implementation(Deps.guice)

    implementation(project(":injekt"))

    implementation(Deps.katana)

    implementation(Deps.kodein)

    implementation(Deps.koin)

    implementation(Deps.Kotlin.stdlib)

    implementation(Deps.kotlinStatistics)

    implementation(Deps.Toothpick.toothpick)
    kapt(Deps.Toothpick.compiler)
}