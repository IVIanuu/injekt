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

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.compilerArgumentsConfigurationFlags
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinNativeCompilationData
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile.Configurator
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject

class InjektPlugin : KotlinCompilerPluginSupportPlugin {
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
    kotlinCompilation.target.project.plugins.hasPlugin(InjektPlugin::class.java)

  override fun apply(target: Project) {
    target.extensions.add("injekt", InjektExtension())
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    kotlinCompilation.kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"

    val project = kotlinCompilation.target.project

    val extension = project.extensions.getByType(InjektExtension::class.java)

    val sourceSetName = kotlinCompilation.defaultSourceSetName
    val outputDir = outputDir(project, sourceSetName)
    val srcDir = srcDir(project, sourceSetName)

    val kotlinCompileProvider: TaskProvider<AbstractCompile> =
      project.locateTask(kotlinCompilation.compileKotlinTaskName)
        ?: return project.provider { emptyList() }

    val injektTaskName = kotlinCompileProvider.name.replaceFirst("compile", "injekt")

    val kotlinCompileTask = kotlinCompileProvider.get()

    fun configure(injektTask: InjektTask) {
      injektTask.options.addAll(
        injektTask.project.provider {
          getSubpluginOptions(project, sourceSetName, extension, true)
        }
      )
      injektTask.outputDir = outputDir
      injektTask.cacheDir = getCacheDir(project, sourceSetName)

      injektTask as AbstractCompile

      injektTask.destinationDirectory.set(outputDir)
      (injektTask as InjektTask).outputs.dirs(outputDir, srcDir)
      injektTask.source(
        kotlinCompileTask.source.filter {
          it.extension == "kt" &&
              !it.absolutePath.startsWith(srcDir.absolutePath)
        }
      )
    }

    val injektTaskProvider = when (kotlinCompileTask) {
      is AbstractKotlinCompile<*> -> {
        val injektTaskClass = when (kotlinCompileTask) {
          is KotlinCompile -> InjektTaskJvm::class.java
          is Kotlin2JsCompile -> InjektTaskJS::class.java
          is KotlinCompileCommon -> InjektTaskMetadata::class.java
          else -> return project.provider { emptyList() }
        }
        project.tasks.register(injektTaskName, injektTaskClass) { injektTask ->
          configure(injektTask)
          injektTask.classpath = kotlinCompileTask.project.files(Callable { kotlinCompileTask.classpath })

          getSubpluginOptions(project, sourceSetName, extension, false).forEach { option ->
            kotlinCompileTask.pluginOptions.addPluginArgument("com.ivianuu.injek".combine("t"), option)
          }

          injektTask.configureCompilation(
            kotlinCompilation as KotlinCompilationData<*>,
            kotlinCompileTask,
          )
        }
      }
      is KotlinNativeCompile -> {
        val injektTaskClass = InjektTaskNative::class.java
        val pluginConfigurationName =
          (kotlinCompileTask.compilation as AbstractKotlinNativeCompilation).pluginConfigurationName
        project.tasks.register(injektTaskName, injektTaskClass, kotlinCompileTask.compilation).apply {
          configure { injektTask ->
            getSubpluginOptions(project, sourceSetName, extension, false).forEach { option ->
              kotlinCompileTask.compilerPluginOptions.addPluginArgument("com.ivianuu.injek".combine("t"), option)
            }
            injektTask.onlyIf { kotlinCompileTask.compilation.konanTarget.enabledOnCurrentHost }
            configure(injektTask)
            injektTask.compilerPluginClasspath = project.configurations.getByName(pluginConfigurationName)
            injektTask.commonSources.from(kotlinCompileTask.commonSources)
          }
        }
      }
      else -> return project.provider { emptyList() }
    }

    kotlinCompileProvider.configure { kotlinCompile ->
      kotlinCompile.dependsOn(injektTaskProvider)
      kotlinCompile.source(srcDir)
    }

    return project.provider { emptyList() }
  }

  override fun getCompilerPluginId(): String = "com.ivianuu.injek".combine("t")

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.ivianuu.injek".combine("t"),
    artifactId = if (javaClass.name == "com.ivianuu.shaded_injekt.gradle.InjektPlugin")
      "injekt-compiler-plugin-shaded" else "injekt-compiler-plugin",
    version = BuildConfig.VERSION
  )
}

interface InjektTask : Task {
  @get:Internal val options: ListProperty<SubpluginOption>

  @get:OutputDirectory var outputDir: File

  @get:LocalState var cacheDir: File

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
    compileKotlinArgumentsContributor.set(
      kotlinCompile.project.provider {
        kotlinCompile.compilerArgumentsContributor
      }
    )
  }

  @get:Internal internal abstract val compileKotlinArgumentsContributor:
      Property<CompilerArgumentsContributor<K2JVMCompilerArguments>>

  init {
    incremental = true
    useClasspathSnapshot.set(true)
    // Mute a warning from ScriptingGradleSubplugin, which tries to get `sourceSetName` before this task is
    // configured.
    sourceSetName.set("main")
  }

  override fun setupCompilerArgs(
    args: K2JVMCompilerArguments,
    defaultsOnly: Boolean,
    ignoreClasspathResolutionErrors: Boolean,
  ) {
    args.fillDefaultValues()
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
    args.freeArgs += "-Xallow-kotlin-package"
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
}

@CacheableTask abstract class InjektTaskJS @Inject constructor(
  objectFactory: ObjectFactory,
) : Kotlin2JsCompile(KotlinJsOptionsImpl(), objectFactory), InjektTask {
  override fun configureCompilation(
    kotlinCompilation: KotlinCompilationData<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  ) {
    Configurator<InjektTaskJS>(kotlinCompilation).configure(this)
    kotlinCompile as Kotlin2JsCompile
    compileKotlinArgumentsContributor.set(
      kotlinCompile.project.provider {
        kotlinCompile.abstractKotlinCompileArgumentsContributor
      }
    )
  }

  @get:Internal internal abstract val compileKotlinArgumentsContributor:
      Property<CompilerArgumentsContributor<K2JSCompilerArguments>>

  init {
    // todo
    incremental = false
  }

  override fun setupCompilerArgs(
    args: K2JSCompilerArguments,
    defaultsOnly: Boolean,
    ignoreClasspathResolutionErrors: Boolean,
  ) {
    args.fillDefaultValues()
    compileKotlinArgumentsContributor.get().contributeArguments(
      args,
      compilerArgumentsConfigurationFlags(
        defaultsOnly,
        ignoreClasspathResolutionErrors
      )
    )
    args.addPluginOptions(options.get())
    args.outputFile = File(outputDir, "dummyOutput.js").canonicalPath
    args.freeArgs += "-Xallow-kotlin-package"
  }

  @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
  fun `callCompilerAsync$kotlin_gradle_plugin`(
    args: K2JSCompilerArguments,
    sourceRoots: SourceRoots,
    changedFiles: ChangedFiles,
  ) {
    args.addChangedFiles(changedFiles)
    super.callCompilerAsync(args, sourceRoots, changedFiles)
  }
}

@CacheableTask abstract class InjektTaskMetadata :
  KotlinCompileCommon(KotlinMultiplatformCommonOptionsImpl()), InjektTask {
  override fun configureCompilation(
    kotlinCompilation: KotlinCompilationData<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  ) {
    Configurator<InjektTaskMetadata>(kotlinCompilation).configure(this)
    kotlinCompile as KotlinCompileCommon
    compileKotlinArgumentsContributor.set(
      kotlinCompile.project.provider {
        kotlinCompile.abstractKotlinCompileArgumentsContributor
      }
    )
  }

  @get:Internal internal abstract val compileKotlinArgumentsContributor:
      Property<CompilerArgumentsContributor<K2MetadataCompilerArguments>>

  init {
    incremental = true
  }

  override fun setupCompilerArgs(
    args: K2MetadataCompilerArguments,
    defaultsOnly: Boolean,
    ignoreClasspathResolutionErrors: Boolean,
  ) {
    args.fillDefaultValues()
    compileKotlinArgumentsContributor.get().contributeArguments(
      args,
      compilerArgumentsConfigurationFlags(
        defaultsOnly,
        ignoreClasspathResolutionErrors
      )
    )
    args.addPluginOptions(options.get())
    args.destination = outputDir.canonicalPath
    val classpathList = classpath.files.filter { it.exists() }.toMutableList()
    args.classpath = classpathList.joinToString(File.pathSeparator)
    args.friendPaths = friendPaths.files.map { it.absolutePath }.toTypedArray()
    args.refinesPaths = refinesMetadataPaths.map { it.absolutePath }.toTypedArray()
    args.freeArgs += "-Xallow-kotlin-package"
  }

  @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
  fun `callCompilerAsync$kotlin_gradle_plugin`(
    args: K2MetadataCompilerArguments,
    sourceRoots: SourceRoots,
    changedFiles: ChangedFiles,
  ) {
    args.addChangedFiles(changedFiles)
    super.callCompilerAsync(args, sourceRoots, changedFiles)
  }
}

@CacheableTask abstract class InjektTaskNative @Inject constructor(
  injected: KotlinNativeCompilationData<*>,
) : KotlinNativeCompile(injected), InjektTask {
  override fun buildCompilerArgs(): List<String> =
    super.buildCompilerArgs() + options.get().flatMap { listOf("-P", it.toArg()) } +
        "-Xallow-kotlin-package"

  override fun configureCompilation(
    kotlinCompilation: KotlinCompilationData<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  ) = Unit
}

private fun getSubpluginOptions(
  project: Project,
  sourceSetName: String,
  extension: InjektExtension,
  forInjektTask: Boolean
): List<SubpluginOption> {
  val options = mutableListOf<SubpluginOption>()
  if (forInjektTask) {
    options += SubpluginOption("cacheDir", getCacheDir(project, sourceSetName).path)
    options += SubpluginOption("srcDir", srcDir(project, sourceSetName).path)
  } else {
    options += SubpluginOption("rootPackage", extension.rootPackage)
    options += SubpluginOption("dumpDir", getDumpDir(project, sourceSetName).path)
    options += SubpluginOption("infoDir", getInfoDir(project, sourceSetName).path)
  }
  return options
}

private fun outputDir(project: Project, sourceSetName: String) =
  File(project.project.buildDir, "injekt/output/$sourceSetName")

private fun srcDir(project: Project, sourceSetName: String) =
  File(project.project.buildDir, "injekt/src/$sourceSetName")

private fun getCacheDir(project: Project, sourceSetName: String) =
  File(project.project.buildDir, "injekt/cache/$sourceSetName")

private fun getDumpDir(project: Project, sourceSetName: String) =
  File(project.project.buildDir, "injekt/dump/$sourceSetName")

private fun getInfoDir(project: Project, sourceSetName: String) =
  File(project.project.buildDir, "injekt/info/$sourceSetName")

private inline fun <reified T : Task> Project.locateTask(name: String): TaskProvider<T>? =
  try {
    tasks.withType(T::class.java).named(name)
  } catch (e: UnknownTaskException) {
    null
  }

private fun SubpluginOption.toArg() = "plugin:com.ivianuu.injek".combine("t:$key=$value")

private fun CommonCompilerArguments.addPluginOptions(options: List<SubpluginOption>) {
  pluginOptions = (options.map { it.toArg() } + pluginOptions!!).toTypedArray()
}

private fun CommonCompilerArguments.addChangedFiles(changedFiles: ChangedFiles) {
  if (changedFiles is ChangedFiles.Known) {
    val options = mutableListOf<SubpluginOption>()
    changedFiles.modified.filter { it.extension == "kt" }.ifNotEmpty {
      options += SubpluginOption("modifiedFiles", joinToString(File.pathSeparator) { it.path })
    }
    changedFiles.removed.filter { it.extension == "kt" }.ifNotEmpty {
      options += SubpluginOption("removedFiles", joinToString(File.pathSeparator) { it.path })
    }
    options.ifNotEmpty { addPluginOptions(this) }
  }
}

fun String.combine(other: String) = this + other
