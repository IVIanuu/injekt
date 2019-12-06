package com.ivianuu.injekt.gradle

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

open class InjektGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.findByType(LibraryExtension::class.java)
            ?.sourceSets?.forEach { sourceSet ->
            val outputDir = File(project.buildDir, "generated/source/injekt/${sourceSet.name}/")
            sourceSet.java.srcDir(outputDir)
        }

        project.extensions.findByType(BaseAppModuleExtension::class.java)
            ?.sourceSets?.forEach { sourceSet ->
            val outputDir = File(project.buildDir, "generated/source/injekt/${sourceSet.name}/")
            sourceSet.java.srcDir(outputDir)
        }
    }

}
