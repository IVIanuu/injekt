/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.ivianuu.injekt.gradle

import org.gradle.api.*
import org.gradle.api.model.*
import org.gradle.api.provider.*
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.*
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.SourceRoots
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*
import java.util.concurrent.*
import javax.inject.*

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
            kotlinCompileTask.pluginOptions.addPluginArgument("com.ivianuu.injekt", option)
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
              kotlinCompileTask.compilerPluginOptions.addPluginArgument("com.ivianuu.injekt", option)
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

  override fun getCompilerPluginId(): String = "com.ivianuu.injekt"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.ivianuu.injekt",
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
    classpathSnapshotProperties.useClasspathSnapshot.value(false).disallowChanges()
  }

  @get:Internal internal abstract val compileKotlinArgumentsContributor:
      Property<CompilerArgumentsContributor<K2JVMCompilerArguments>>

  init {
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
    taskOutputsBackup: TaskOutputsBackup?,
  ) {
    args.addChangedFiles(changedFiles)
    super.callCompilerAsync(args, sourceRoots, changedFiles, taskOutputsBackup)
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
    taskOutputsBackup: TaskOutputsBackup?,
  ) {
    args.addChangedFiles(changedFiles)
    super.callCompilerAsync(args, sourceRoots, changedFiles, taskOutputsBackup)
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
    taskOutputsBackup: TaskOutputsBackup?,
  ) {
    args.addChangedFiles(changedFiles)
    super.callCompilerAsync(args, sourceRoots, changedFiles, taskOutputsBackup)
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

private fun SubpluginOption.toArg() = "plugin:com.ivianuu.injekt:$key=$value"

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
