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

import com.android.build.gradle.BaseExtension
import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@AutoService(KotlinGradleSubplugin::class)
open class InjektGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {

    override fun isApplicable(project: Project, task: AbstractCompile) =
        project.plugins.hasPlugin(InjektGradlePlugin::class.java)

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {
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

        val srcDir = project.buildDir.resolve("generated/source/injekt/$sourceSetName")
            .also { it.mkdirs() }
            .absolutePath

        if (androidProjectHandler != null) {
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

        return listOf(
            SubpluginOption(
                key = "srcDir",
                value = srcDir
            )
        )
    }

    override fun getCompilerPluginId(): String = "com.ivianuu.injekt"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.GROUP_ID,
        artifactId = BuildConfig.ARTIFACT_ID,
        version = BuildConfig.VERSION
    )
}
