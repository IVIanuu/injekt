import com.android.build.gradle.api.BaseVariant
import com.ivianuu.injekt.gradle.UpdateCacheTask
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    id("com.android.application")
    kotlin("android")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/android-build-app.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/android-proguard.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8-android.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-lint.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-source-sets-android.gradle")

tasks.withType<KotlinCompile> {
    val updateCacheTask = tasks.create<UpdateCacheTask>("updateCache$name")
    val baseSrcDir = buildDir.resolve("generated/source/injekt")
    val cacheDir = buildDir.resolve("injekt/cache")
    val compilation = AbstractKotlinCompile::class.java
        .getDeclaredMethod("getTaskData\$kotlin_gradle_plugin")
        .invoke(this)
        .let { taskData ->
            taskData.javaClass
                .getDeclaredMethod("getCompilation")
                .invoke(taskData) as KotlinCompilation<*>
        }
    val androidVariantData: BaseVariant? =
        (compilation as? KotlinJvmAndroidCompilation)?.androidVariant

    val sourceSetName = androidVariantData?.name ?: compilation.compilationName

    val resourcesDir = (if (androidVariantData != null) {
        buildDir.resolve("tmp/kotlin-classes/$sourceSetName")
    } else {
        compilation.output.resourcesDir
    }).also { it.mkdirs() }.absolutePath

    updateCacheTask.srcFiles = source.filter { it.name.endsWith(".kt") }
    updateCacheTask.cacheFile = cacheDir.resolve("file-cache")
        .also { if (!it.exists()) it.createNewFile() }

    afterEvaluate { dependsOn(updateCacheTask) }
}

dependencies {
    implementation(Deps.AndroidX.appCompat)
    implementation(project(":injekt-android"))
    implementation(project(":injekt-android-work"))
    implementation(project(":injekt-core"))
    implementation(project(":injekt-common"))
    kotlinCompilerPluginClasspath(project(":injekt-compiler-plugin"))

    implementation(Deps.AndroidX.Compose.runtime)
    kotlinCompilerPluginClasspath(Deps.AndroidX.Compose.compiler)
    implementation(Deps.AndroidX.Compose.material)
}