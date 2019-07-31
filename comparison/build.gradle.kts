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
    kotlin("android")
    kotlin("kapt")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8-android.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-kapt.gradle")
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

    implementation(Deps.dagger2)
    kapt(Deps.dagger2Compiler)

    implementation(Deps.dagger2Reflect)

    api(Deps.guava)

    implementation(Deps.guice)

    implementation(project(":injekt"))
    kapt(project(":injekt-compiler"))

    implementation(Deps.katana)

    implementation(Deps.kodein)

    implementation(Deps.koin)

    implementation(Deps.kotlinStdLib)

    implementation(Deps.kotlinStatistics)

    implementation(Deps.toothpick)
    kapt(Deps.toothpickCompiler)
}