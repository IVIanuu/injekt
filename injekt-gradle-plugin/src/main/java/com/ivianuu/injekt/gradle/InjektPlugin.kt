/*
 * Copyright 2021 Manuel Wrage
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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.ivianuu.injekt.gradle

import com.google.auto.service.AutoService
import java.io.File
import java.util.concurrent.Callable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.fillDefaultValues
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.compilerArgumentsConfigurationFlags
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile.Configurator
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.SourceRoots
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

@AutoService(KotlinCompilerPluginSupportPlugin::class)
open class InjektPlugin : KotlinCompilerPluginSupportPlugin {
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
    kotlinCompilation.target.project.plugins.hasPlugin(InjektPlugin::class.java)

  override fun apply(target: Project) {
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val kotlinCompileProvider: TaskProvider<AbstractCompile> =
      project.locateTask(kotlinCompilation.compileKotlinTaskName) ?: return project.provider { emptyList() }

    val sourceSetName = kotlinCompilation.defaultSourceSetName
    val outputDir = outputDir(project, sourceSetName)

    val injektTaskName = kotlinCompileProvider.name.replaceFirst("compile", "injekt")

    val kotlinCompileTask = kotlinCompileProvider.get()

    fun configureAsInjektTask(injektTask: InjektTask) {
      injektTask.options.addAll(
        injektTask.project.provider {
          getSubpluginOptions(
            project,
            sourceSetName
          )
        }
      )
      injektTask.outputDir = outputDir
      injektTask.cacheDir = cacheDir(project, sourceSetName)
      injektTask.dumpDir = dumpDir(project, sourceSetName)
    }

    fun configureAsAbstractCompile(injektTask: AbstractCompile) {
      injektTask.destinationDirectory.set(outputDir)
      injektTask.outputs.dirs(outputDir)
      kotlinCompilation.allKotlinSourceSets.forEach { sourceSet -> injektTask.source(sourceSet.kotlin) }
    }

    val injektTaskProvider = when (kotlinCompileTask) {
      is AbstractKotlinCompile<*> -> {
        val injektTaskClass = when (kotlinCompileTask) {
          is KotlinCompile -> InjektTaskJvm::class.java
          is KotlinCompileCommon -> InjektTaskMetadata::class.java
          else -> return project.provider { emptyList() }
        }
        project.tasks.register(injektTaskName, injektTaskClass) { injektTask ->
          configureAsInjektTask(injektTask)
          configureAsAbstractCompile(injektTask)

          injektTask.classpath = kotlinCompileTask.project.files(Callable { kotlinCompileTask.classpath })
          injektTask.configureCompilation(
            kotlinCompilation as KotlinCompilationData<*>,
            kotlinCompileTask,
          )
        }
      }
      else -> return project.provider { emptyList() }
    }

    kotlinCompileProvider.configure { kotlinCompile ->
      kotlinCompile.dependsOn(injektTaskProvider)
      kotlinCompile.source(outputDir)
    }

    return project.provider { emptyList() }
  }

  override fun getCompilerPluginId(): String = "com.ivianuu.injekt"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = BuildConfig.GROUP_ID,
    artifactId = BuildConfig.ARTIFACT_ID,
    version = BuildConfig.VERSION
  )

  companion object {
    fun cacheDir(project: Project, sourceSetName: String) =
      File(project.project.buildDir, "injekt/cache/$sourceSetName")

    fun dumpDir(project: Project, sourceSetName: String) =
      File(project.project.buildDir, "injekt/dump/$sourceSetName")

    fun outputDir(project: Project, sourceSetName: String) =
      File(project.project.buildDir, "injekt/generated/$sourceSetName")

    private fun getSubpluginOptions(project: Project, sourceSetName: String): List<SubpluginOption> {
      val options = mutableListOf<SubpluginOption>()
      options += SubpluginOption("cacheDir", cacheDir(project, sourceSetName).path)
      options += SubpluginOption("dumpDir", dumpDir(project, sourceSetName).path)
      options += SubpluginOption("outputDir", outputDir(project, sourceSetName).path)
      return options
    }
  }
}

internal inline fun <reified T : Task> Project.locateTask(name: String): TaskProvider<T>? =
  try {
    tasks.withType(T::class.java).named(name)
  } catch (e: UnknownTaskException) {
    null
  }

interface InjektTask : Task {
  @get:Internal
  val options: ListProperty<SubpluginOption>

  @get:LocalState
  var cacheDir: File

  @get:OutputDirectory
  var dumpDir: File

  @get:OutputDirectory
  var outputDir: File

  fun configureCompilation(
    kotlinCompilation: KotlinCompilationData<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  )
}

@CacheableTask abstract class InjektTaskJvm : KotlinCompile(KotlinJvmOptionsImpl()), InjektTask {
  override fun configureCompilation(
    kotlinCompilation: KotlinCompilationData<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  ) {
    Configurator<InjektTaskJvm>(kotlinCompilation).configure(this)
    kotlinCompile as KotlinCompile
    val providerFactory = kotlinCompile.project.providers
    compileKotlinArgumentsContributor.set(
      providerFactory.provider {
        kotlinCompile.compilerArgumentsContributor
      }
    )
  }

  @get:Internal
  internal abstract val compileKotlinArgumentsContributor:
      Property<CompilerArgumentsContributor<K2JVMCompilerArguments>>

  init {
    incremental = false
  }

  override fun setupCompilerArgs(
    args: K2JVMCompilerArguments,
    defaultsOnly: Boolean,
    ignoreClasspathResolutionErrors: Boolean,
  ) {
    compileKotlinArgumentsContributor.get().contributeArguments(
      args,
      compilerArgumentsConfigurationFlags(
        defaultsOnly,
        ignoreClasspathResolutionErrors
      )
    )
    args.addPluginOptions(options.get())
    args.destinationAsFile = outputDir
    args.allowNoSourceFiles = true
  }

  @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
  fun `callCompilerAsync$kotlin_gradle_plugin`(
    args: K2JVMCompilerArguments,
    sourceRoots: SourceRoots,
    changedFiles: ChangedFiles,
  ) {
    args.addChangedFiles(changedFiles)
    super.callCompilerAsync(args, sourceRoots, changedFiles)
  }

  override fun skipCondition(): Boolean = false
}

abstract class InjektTaskMetadata : KotlinCompileCommon(KotlinMultiplatformCommonOptionsImpl()), InjektTask {
  override fun configureCompilation(
    kotlinCompilation: KotlinCompilationData<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  ) {
    Configurator<InjektTaskMetadata>(kotlinCompilation).configure(this)
    kotlinCompile as KotlinCompileCommon
    val providerFactory = kotlinCompile.project.providers
    compileKotlinArgumentsContributor.set(
      providerFactory.provider {
        kotlinCompile.abstractKotlinCompileArgumentsContributor
      }
    )
  }

  @get:Internal
  internal abstract val compileKotlinArgumentsContributor:
      Property<CompilerArgumentsContributor<K2MetadataCompilerArguments>>

  init {
    incremental = false
  }

  override fun setupCompilerArgs(
    args: K2MetadataCompilerArguments,
    defaultsOnly: Boolean,
    ignoreClasspathResolutionErrors: Boolean,
  ) {
    args.apply { fillDefaultValues() }
    compileKotlinArgumentsContributor.get().contributeArguments(
      args,
      compilerArgumentsConfigurationFlags(
        defaultsOnly,
        ignoreClasspathResolutionErrors
      )
    )
    args.addPluginOptions(options.get())
    args.destination = outputDir.canonicalPath
  }

  // TODO: Ask upstream to open it.
  @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
  fun `callCompilerAsync$kotlin_gradle_plugin`(
    args: K2MetadataCompilerArguments,
    sourceRoots: SourceRoots,
    changedFiles: ChangedFiles,
  ) {
    if (changedFiles.hasNonSourceChange()) {
      clearIncCache()
    } else {
      args.addChangedFiles(changedFiles)
    }
    super.callCompilerAsync(args, sourceRoots, changedFiles)
  }
}

private fun InjektTask.clearIncCache() {
  cacheDir.deleteRecursively()
}

private fun ChangedFiles.hasNonSourceChange(): Boolean {
  if (this !is ChangedFiles.Known)
    return true

  return !(this.modified + this.removed).all {
    it.isKotlinFile(listOf("kt")) || it.isJavaFile()
  }
}

private fun SubpluginOption.toArg() = "plugin:com.ivianuu.injekt:$key=$value"

private fun CommonCompilerArguments.addPluginOptions(options: List<SubpluginOption>) {
  pluginOptions = (options.map { it.toArg() } + pluginOptions!!).toTypedArray()
}

private fun CommonCompilerArguments.addChangedFiles(changedFiles: ChangedFiles) {
  if (changedFiles is ChangedFiles.Known) {
    val options = mutableListOf<SubpluginOption>()
    changedFiles.modified.filter { it.isKotlinFile(listOf("kt")) || it.isJavaFile() }.ifNotEmpty {
      options += SubpluginOption("knownModified", map { it.path }.joinToString(File.pathSeparator))
    }
    changedFiles.removed.filter { it.isKotlinFile(listOf("kt")) || it.isJavaFile() }.ifNotEmpty {
      options += SubpluginOption("knownRemoved", map { it.path }.joinToString(File.pathSeparator))
    }
    options.ifNotEmpty { addPluginOptions(this) }
  }
}
