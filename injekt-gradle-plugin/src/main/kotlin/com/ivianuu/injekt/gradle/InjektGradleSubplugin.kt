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

package com.ivianuu.injekt.gradle

import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mapClasspath
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KtGpAccessor
import java.io.File

@AutoService(KotlinGradleSubplugin::class)
open class InjektGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {

    private val kotlinToGenerateStubsTask = mutableMapOf<KotlinCompile, GenerateStubsTask>()

    override fun isApplicable(project: Project, task: AbstractCompile) =
        task is KotlinCompile && project.plugins.hasPlugin(InjektGradlePlugin::class.java)

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<*>?
    ): List<SubpluginOption> {
        kotlinCompile as KotlinCompile

        val sourceSetName = if (variantData != null) {
            // Lol
            variantData.javaClass.getMethod("getName").run {
                isAccessible = true
                invoke(variantData) as String
            }
        } else {
            if (kotlinCompilation == null) error("In non-Android projects, Kotlin compilation should not be null")
            kotlinCompilation.compilationName
        }

        val generateStubsTaskName = "generateInjekt${sourceSetName.capitalize()}Stubs"

        val outputDir = File(project.buildDir, "generated/source/injekt/$sourceSetName")

        val generateStubsTask = project.tasks.create(
            generateStubsTaskName,
            GenerateStubsTask::class.java
        )
        generateStubsTask.kotlinCompileTask = kotlinCompile

        generateStubsTask.pluginOptions.addPluginArgument(
            getCompilerPluginId(), SubpluginOption("outputDir", outputDir.absolutePath)
        )

        KtGpAccessor.registerKotlinCompileTaskData(
            generateStubsTaskName,
            KtGpAccessor.getKotlinCompileTaskDataCompilation(project, kotlinCompile.name)
        )

        val kotlinOptions = kotlinCompile.kotlinOptions
        with(generateStubsTask.kotlinOptions) {
            apiVersion = kotlinOptions.apiVersion
            languageVersion = kotlinOptions.languageVersion
            includeRuntime = kotlinOptions.includeRuntime
            javaParameters = kotlinOptions.javaParameters
            jdkHome = kotlinOptions.jdkHome
            jvmTarget = kotlinOptions.jvmTarget
            noJdk = kotlinOptions.noJdk
            noReflect = kotlinOptions.noReflect
            noStdlib = kotlinOptions.noStdlib
            useIR = kotlinOptions.useIR
        }

        generateStubsTask.setDestinationDir { outputDir }
        generateStubsTask.mapClasspath { kotlinCompile.classpath }
        generateStubsTask.dependsOn(*kotlinCompile.dependsOn.toTypedArray())
        if (project.plugins.hasPlugin("org.jetbrains.kotlin.kapt")) {
            generateStubsTask.dependsOn("kapt${sourceSetName.capitalize()}Kotlin")
            kotlinCompile.mustRunAfter(generateStubsTask)
        } else {
            kotlinCompile.dependsOn(generateStubsTask)
        }

        kotlinCompile.source("$outputDir/kotlin")

        kotlinToGenerateStubsTask[kotlinCompile] = generateStubsTask

        return emptyList()
    }

    override fun getSubpluginKotlinTasks(
        project: Project,
        kotlinCompile: AbstractCompile
    ): List<AbstractCompile> {
        val generateStubsTask = kotlinToGenerateStubsTask[kotlinCompile]
        return if (generateStubsTask == null) emptyList() else listOf(generateStubsTask)
    }

    override fun getCompilerPluginId(): String = "com.ivianuu.injekt"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.GROUP_ID,
        artifactId = BuildConfig.ARTIFACT_ID,
        version = BuildConfig.VERSION
    )

}
