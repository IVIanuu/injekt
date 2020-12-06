package com.ivianuu.injekt.gradle

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.crash.afterEvaluate
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun AbstractKotlinCompile<*>.setupForInjekt(): List<SubpluginOption> {
    val compilation = AbstractKotlinCompile::class.java
        .getDeclaredMethod("getTaskData\$kotlin_gradle_plugin")
        .invoke(this)
        .let { taskData ->
            taskData.javaClass
                .getDeclaredMethod("getCompilation")
                .invoke(taskData) as KotlinCompilation<*>
        }
    val androidVariantData: com.android.build.gradle.api.BaseVariant? =
        (compilation as? org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation)?.androidVariant

    if (this is KotlinCompile) {
        usePreciseJavaTracking = false
    }

    val sourceSetName =
        androidVariantData?.javaClass?.getMethod("getName")?.run {
            isAccessible = true
            invoke(androidVariantData) as String
        } ?: compilation.compilationName

    val srcDir = project.buildDir.resolve("generated/source/injekt/$sourceSetName")
        .also { it.mkdirs() }

    if (androidVariantData != null) {
        project.extensions.findByType(BaseExtension::class.java)
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

    val cacheDir = project.buildDir.resolve("injekt/cache")
        .also { it.mkdirs() }

    val extension = project.extensions.getByType(InjektExtension::class.java)

    val isIncremental = !extension.generateComponents && !extension.generateMergeComponents

    incremental = isIncremental

    project.afterEvaluate {
        val cleanGeneratedFiles = project.tasks.create(
            "${name}InjektCleanGeneratedFiles", CleanGeneratedFiles::class.java)
        cleanGeneratedFiles.isIncremental = isIncremental
        cleanGeneratedFiles.cacheDir = cacheDir
        cleanGeneratedFiles.generatedSrcDir = srcDir
        cleanGeneratedFiles.srcDirs = if (androidVariantData != null) {
            androidVariantData.sourceSets
                .flatMap { it.javaDirectories }
                .flatMap {
                    it.walkTopDown()
                        .onEnter { it != srcDir }
                        .toList()
                }
        } else {
            project.extensions.findByType(SourceSetContainer::class.java)!!
                .findByName(sourceSetName)
                ?.allSource
                ?.filterNot { it.absolutePath.startsWith(srcDir.absolutePath) }
                ?: emptyList()
        }
        dependsOn(cleanGeneratedFiles)

        log("Setup in ${project.name} $name\n" +
                "source set $sourceSetName\n" +
                "extension: $extension\n" +
                "incremental: $isIncremental\n" +
                "cache dir $cacheDir\n" +
                "gen dir $srcDir\n" +
                "src dirs ${cleanGeneratedFiles.srcDirs.joinToString("\n")}\n" +
                "compilation $compilation" +
                "variant data $androidVariantData")
    }

    return if (project.name == "injekt-compiler-plugin") {
        listOf(
            SubpluginOption(
                key = "srcDir",
                value = srcDir.absolutePath
            )
        )
    } else {
        listOf(
            SubpluginOption(
                key = "generateComponents",
                value = extension.generateComponents.toString()
            ),
            SubpluginOption(
                key = "generateMergeComponents",
                value = extension.generateMergeComponents.toString()
            ),
            SubpluginOption(
                key = "srcDir",
                value = srcDir.absolutePath
            ),
            SubpluginOption(
                key = "cacheDir",
                value = cacheDir.absolutePath
            )
        )
    }
}
