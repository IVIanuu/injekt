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
    kotlin("jvm")
    kotlin("kapt")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-lint.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/mvn-publish.gradle")

// todo remove
tasks.withType<KotlinCompile> {
    incremental = false
}

dependencies {
    implementation(Deps.Injekt.core)
    kotlinCompilerPluginClasspath(Deps.Injekt.compilerPlugin)
    api(Deps.Ksp.api)
    api(Deps.Ksp.symbolProcessing)
    implementation(Deps.processingX)
    compileOnly(Deps.Kotlin.compilerEmbeddable)
    kapt(Deps.processingX)
    implementation(Deps.Kotlin.stdlib)
}

// todo move
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    val compilation = org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile::class.java
        .getDeclaredMethod("getTaskData\$kotlin_gradle_plugin")
        .invoke(this)
        .let { taskData ->
            taskData.javaClass
                .getDeclaredMethod("getCompilation")
                .invoke(taskData) as org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<*>
        }
    val androidVariantData: com.android.build.gradle.api.BaseVariant? =
        (compilation as? org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation)?.androidVariant

    val sourceSetName = androidVariantData?.name ?: compilation.compilationName

    val srcDir = buildDir.resolve("generated/source/injekt/$sourceSetName")
        .also { it.mkdirs() }.absolutePath

    if (androidVariantData != null) {
        project.extensions.findByType(com.android.build.gradle.BaseExtension::class.java)
            ?.sourceSets
            ?.findByName(sourceSetName)
            ?.java
            ?.srcDir(srcDir)
    } else {
        project.extensions.findByType(SourceSetContainer::class.java)
            ?.findByName(sourceSetName)
            ?.java
            ?.srcDir(srcDir)
    }

    kotlinOptions {
        useIR = true
        freeCompilerArgs += listOf(
            "-P", "plugin:com.ivianuu.injekt:srcDir=$srcDir"
        )
    }
}
