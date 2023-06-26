/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
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
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.compilerArgumentsConfigurationFlags
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup
import org.jetbrains.kotlin.gradle.tasks.configuration.BaseKotlin2JsCompileConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileCommonConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileConfig
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
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    kotlinCompilation.kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"

    val project = kotlinCompilation.target.project

    val sourceSetName = kotlinCompilation.defaultSourceSetName
    val outputDir = outputDir(project, sourceSetName)
    val srcDir = srcDir(project, sourceSetName)

    val kotlinCompileProvider: TaskProvider<AbstractKotlinCompileTool<*>> =
      project.locateTask(kotlinCompilation.compileKotlinTaskName)
        ?: return project.provider { emptyList() }

    val injektTaskName = kotlinCompileProvider.name.replaceFirst("compile", "injekt")

    val kotlinCompileTask = kotlinCompileProvider.get()

    fun configure(injektTask: InjektTask) {
      injektTask.options.addAll(
        injektTask.project.provider {
          getSubpluginOptions(project, sourceSetName, true)
        }
      )
      injektTask.cacheDir = getCacheDir(project, sourceSetName)
      injektTask.outputDir = outputDir
      injektTask.srcDir = srcDir

      injektTask as AbstractKotlinCompileTool<*>

      injektTask.destinationDirectory.set(outputDir)
      injektTask.outputs.dirs(outputDir, srcDir)
      injektTask.setSource(
        kotlinCompileTask.sources.filter {
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
        project.tasks.register(injektTaskName, injektTaskClass)
          .also { injektTaskProvider ->
            injektTaskProvider.configure { injektTask ->
              if (injektTask is InjektTaskJvm) {
                injektTask.compilerOptions.moduleName.convention(kotlinCompileTask.moduleName.map { "$it-injekt" })
              }

              configure(injektTask)

              injektTask.libraries.setFrom(kotlinCompileTask.project.files(Callable {
                kotlinCompileTask.libraries
              }))
              injektTask.dependsOn(injektTask.libraries)

              getSubpluginOptions(project, sourceSetName, false).forEach { option ->
                kotlinCompileTask.pluginOptions.add(
                  CompilerPluginConfig().apply {
                    addPluginArgument("com.ivianuu.injekt", option)
                  }
                )
              }

              injektTask.configureCompilation(
                kotlinCompilation,
                kotlinCompileTask,
              )
            }

            when (injektTaskClass) {
              InjektTaskJvm::class.java -> {
                KotlinCompileConfig(KotlinCompilationInfo(kotlinCompilation))
                  .execute(injektTaskProvider as TaskProvider<KotlinCompile>)
              }
              InjektTaskJS::class.java -> {
                BaseKotlin2JsCompileConfig<Kotlin2JsCompile>(KotlinCompilationInfo(kotlinCompilation))
                  .execute(injektTaskProvider as TaskProvider<Kotlin2JsCompile>)
                injektTaskProvider.configure {
                  it as Kotlin2JsCompile
                  it.incrementalJsKlib = false
                }
              }
              InjektTaskMetadata::class.java -> {
                KotlinCompileCommonConfig(KotlinCompilationInfo(kotlinCompilation))
                  .execute(injektTaskProvider as TaskProvider<KotlinCompileCommon>)
              }
            }
          }
      }
      is KotlinNativeCompile -> {
        val injektTaskClass = InjektTaskNative::class.java
        project.tasks.register(injektTaskName, injektTaskClass, kotlinCompileTask.compilation).also {
          it.configure { injektTask ->
            getSubpluginOptions(project, sourceSetName, false).forEach { option ->
              kotlinCompileTask.compilerPluginOptions.addPluginArgument("com.ivianuu.injekt", option)
            }
            injektTask.onlyIf { injektTask.konanTarget.enabledOnCurrentHost }
            configure(injektTask)
            injektTask.compilerPluginClasspath = kotlinCompileTask.compilerPluginClasspath
            injektTask.commonSources.from(kotlinCompileTask.commonSources)
            injektTask.compilerPluginOptions.addPluginArgument(kotlinCompileTask.compilerPluginOptions)
            injektTask.doFirst {
              outputDir.deleteRecursively()
            }
          }
        }
      }
      else -> return project.provider { emptyList() }
    }

    kotlinCompileProvider.configure { kotlinCompile ->
      kotlinCompile.dependsOn(injektTaskProvider)
      kotlinCompile.setSource(srcDir)
    }

    return project.provider { emptyList() }
  }

  override fun getCompilerPluginId(): String = "com.ivianuu.injekt"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.ivianuu.injekt",
    artifactId = "injekt-compiler-plugin",
    version = BuildConfig.VERSION
  )
}

interface InjektTask : Task {
  @get:Internal val options: ListProperty<SubpluginOption>

  @get:Internal var cacheDir: File

  @get:Internal var srcDir: File

  @get:OutputDirectory var outputDir: File

  fun configureCompilation(
    kotlinCompilation: KotlinCompilation<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  )
}

@CacheableTask abstract class InjektTaskJvm @Inject constructor(
  objectFactory: ObjectFactory,
  workerExecutor: WorkerExecutor
) : KotlinCompile(
  objectFactory.newInstance(KotlinJvmCompilerOptionsDefault::class.java),
  workerExecutor,
  objectFactory
), InjektTask {
  override fun configureCompilation(
    kotlinCompilation: KotlinCompilation<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  ) {
    kotlinCompile as KotlinCompile
    compileKotlinArgumentsContributor.set(
      kotlinCompile.project.provider {
        kotlinCompile.compilerArgumentsContributor
      }
    )
  }

  @get:Internal internal abstract val compileKotlinArgumentsContributor:
      Property<CompilerArgumentsContributor<K2JVMCompilerArguments>>

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
    args.freeArgs += "-Xallow-kotlin-package"
  }

  @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
  fun `callCompilerAsync$kotlin_gradle_plugin_common`(
    args: K2JVMCompilerArguments,
    kotlinSources: Set<File>,
    inputChanges: InputChanges,
    taskOutputsBackup: TaskOutputsBackup?
  ) {
    if (cacheDir.exists()) {
      args.addChangedFiles(getChangedFiles(inputChanges, incrementalProps))
    } else {
      srcDir.deleteRecursively()
    }
    super.callCompilerAsync(args, kotlinSources, inputChanges, taskOutputsBackup)
  }
}

@CacheableTask abstract class InjektTaskJS @Inject constructor(
  objectFactory: ObjectFactory,
  workerExecutor: WorkerExecutor
) : Kotlin2JsCompile(
  objectFactory.newInstance(KotlinJsCompilerOptionsDefault::class.java),
  objectFactory,
  workerExecutor
), InjektTask {
  override fun configureCompilation(
    kotlinCompilation: KotlinCompilation<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  ) {
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
  fun `callCompilerAsync$kotlin_gradle_plugin_common`(
    args: K2JSCompilerArguments,
    kotlinSources: Set<File>,
    inputChanges: InputChanges,
    taskOutputsBackup: TaskOutputsBackup?
  ) {
    if (cacheDir.exists()) {
      args.addChangedFiles(getChangedFiles(inputChanges, incrementalProps))
    } else {
      srcDir.deleteRecursively()
    }
    super.callCompilerAsync(args, kotlinSources, inputChanges, taskOutputsBackup)
  }
}

@CacheableTask abstract class InjektTaskMetadata @Inject constructor(
  objectFactory: ObjectFactory,
  workerExecutor: WorkerExecutor
) : KotlinCompileCommon(
  objectFactory.newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java),
  workerExecutor,
  objectFactory
), InjektTask {
  override fun configureCompilation(
    kotlinCompilation: KotlinCompilation<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  ) {
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
    compileKotlinArgumentsContributor.get().contributeArguments(
      args,
      compilerArgumentsConfigurationFlags(
        defaultsOnly,
        ignoreClasspathResolutionErrors
      )
    )
    args.addPluginOptions(options.get())
    args.destination = outputDir.canonicalPath
    val classpathList = libraries.files.filter { it.exists() }.toMutableList()
    args.classpath = classpathList.joinToString(File.pathSeparator)
    args.friendPaths = friendPaths.files.map { it.absolutePath }.toTypedArray()
    args.refinesPaths = refinesMetadataPaths.map { it.absolutePath }.toTypedArray()
    args.freeArgs += "-Xallow-kotlin-package"
  }

  @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPOSED_PARAMETER_TYPE")
  fun `callCompilerAsync$kotlin_gradle_plugin_common`(
    args: K2MetadataCompilerArguments,
    kotlinSources: Set<File>,
    inputChanges: InputChanges,
    taskOutputsBackup: TaskOutputsBackup?
  ) {
    if (cacheDir.exists()) {
      args.addChangedFiles(getChangedFiles(inputChanges, incrementalProps))
    } else {
      srcDir.deleteRecursively()
    }
    super.callCompilerAsync(args, kotlinSources, inputChanges, taskOutputsBackup)
  }
}

@CacheableTask abstract class InjektTaskNative @Inject internal constructor(
  compilation: KotlinCompilationInfo,
  objectFactory: ObjectFactory,
  providerFactory: ProviderFactory,
  execOperations: ExecOperations
) : KotlinNativeCompile(compilation, objectFactory, providerFactory, execOperations), InjektTask {
  override val additionalCompilerOptions: Provider<Collection<String>>
    get() = project.provider {
      super.additionalCompilerOptions.get() + options.get().flatMap { listOf("-P", it.toArg()) } +
          "-Xallow-kotlin-package"
    }

  override fun configureCompilation(
    kotlinCompilation: KotlinCompilation<*>,
    kotlinCompile: AbstractKotlinCompile<*>,
  ) = Unit
}

private fun getSubpluginOptions(
  project: Project,
  sourceSetName: String,
  forInjektTask: Boolean
): List<SubpluginOption> = buildList {
  if (forInjektTask) {
    this += SubpluginOption("cacheDir", getCacheDir(project, sourceSetName).path)
    this += SubpluginOption("srcDir", srcDir(project, sourceSetName).path)
  } else {
    this += SubpluginOption("cacheDir", getCacheDir(project, sourceSetName).path)
    this += SubpluginOption("dumpDir", getDumpDir(project, sourceSetName).path)
  }
}

private fun outputDir(project: Project, sourceSetName: String) =
  File(project.project.buildDir, "injekt/output/$sourceSetName")

private fun srcDir(project: Project, sourceSetName: String) =
  File(project.project.buildDir, "injekt/src/$sourceSetName")

private fun getCacheDir(project: Project, sourceSetName: String) =
  File(project.project.buildDir, "injekt/cache/$sourceSetName")

private fun getDumpDir(project: Project, sourceSetName: String) =
  File(project.project.buildDir, "injekt/dump/$sourceSetName")

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
