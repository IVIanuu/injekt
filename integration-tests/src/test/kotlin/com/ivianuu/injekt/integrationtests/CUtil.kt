/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UNCHECKED_CAST")
@file:OptIn(ExperimentalCompilerApi::class, ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.backend.*
import com.ivianuu.injekt.ksp.*
import com.tschuchort.compiletesting.*
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.*
import io.kotest.matchers.string.*
import org.intellij.lang.annotations.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.extensions.*
import java.net.*
import java.nio.file.*
import kotlin.reflect.*

var fileIndex = 0

fun source(
  @Language("kotlin") source: String,
  name: String = "File${fileIndex++}.kt",
  injektImports: Boolean = true,
  packageFqName: FqName = FqName("com.ivianuu.injekt.integrationtests")
) = SourceFile.kotlin(
  name = name,
  contents = buildString {
    if (injektImports) {
      appendLine()
      appendLine("package $packageFqName")
      appendLine()
      appendLine("import androidx.compose.runtime.*")
      appendLine("import com.ivianuu.injekt.*")
      appendLine("import com.ivianuu.injekt.common.*")
      appendLine("import com.ivianuu.injekt.internal.*")
      appendLine("import kotlin.reflect.*")
      appendLine("import kotlinx.coroutines.*")
      appendLine()
    }

    append(source)
  }
)

fun invokableSource(
  @Language("kotlin") source: String,
  injektImports: Boolean = true,
) = source(source, "File.kt", injektImports)

fun codegen(
  @Language("kotlin") source1: String,
  config: KotlinCompilation.() -> Unit = {},
  assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() },
) = codegen(
  sources = listOf(invokableSource(source1)),
  config = config,
  assertions = assertions
)

fun codegen(
  @Language("kotlin") source1: String,
  @Language("kotlin") source2: String,
  config: KotlinCompilation.() -> Unit = {},
  assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() },
) = codegen(
  sources = listOf(source(source1), invokableSource(source2)),
  config = config,
  assertions = assertions
)

fun codegen(
  sources: List<SourceFile>,
  config: KotlinCompilation.() -> Unit = {},
  assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() },
) {
  val result = compile {
    this.sources = sources.toList()
    config()
  }
  println("Result: ${result.exitCode} m: ${result.messages}")
  assertions(
    object : KotlinCompilationAssertionScope {
      override val result: JvmCompilationResult
        get() = result
    }
  )
}

fun singleAndMultiCodegen(
  @Language("kotlin") source1: String,
  @Language("kotlin") source2: String,
  config: KotlinCompilation.(Int) -> Unit = {},
  assertions: KotlinCompilationAssertionScope.(Boolean) -> Unit = { compilationShouldBeOk() }
) {
  singleAndMultiCodegen(
    listOf(listOf(source(source1)), listOf(invokableSource(source2))),
    config, assertions
  )
}

fun singleAndMultiCodegen(
  sources: List<List<SourceFile>>,
  config: KotlinCompilation.(Int) -> Unit = {},
  assertions: KotlinCompilationAssertionScope.(Boolean) -> Unit = { compilationShouldBeOk() }
) {
  codegen(sources.flatten(), {
    workingDir = Files.createTempDirectory("single-compilation").toFile()
    moduleName = "single-compilation"
    config(-1)
  }, { assertions(false) })
  multiCodegen(sources, {
    workingDir = Files.createTempDirectory("multi-compilation-$it").toFile()
    config(it)
  }, { assertions(true) })
}

fun multiCodegen(
  @Language("kotlin") source1: String,
  @Language("kotlin") source2: String,
  config: KotlinCompilation.(Int) -> Unit = {},
  assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() }
) {
  multiCodegen(
    listOf(listOf(source(source1)), listOf(invokableSource(source2))),
    config,
    assertions
  )
}

fun multiCodegen(
  sources: List<List<SourceFile>>,
  config: KotlinCompilation.(Int) -> Unit = {},
  assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() }
) {
  val prevCompilations = mutableListOf<KotlinCompilation>()
  val results = sources.mapIndexed { index, sourceFiles ->
    compile {
      workingDir = Files.createTempDirectory("multi-compilation-$index").toFile()
      this.sources = sourceFiles
      moduleName = "multi-compilation-$index"
      classpaths += prevCompilations.map { it.classesDir }
      config(index)
      prevCompilations += this
    }
  }
  object : KotlinCompilationAssertionScope {
    override val result: JvmCompilationResult
      get() = results.last()
    override val classLoader: ClassLoader = URLClassLoader(
      results.flatMap { it.classLoader.urLs.toList() }
        .toTypedArray()
    )
  }.assertions()
}

fun multiPlatformCodegen(
  @Language("kotlin") commonSource: String,
  @Language("kotlin") platformSource: String,
  config: KotlinCompilation.() -> Unit = {},
  assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() },
) {
  multiPlatformCodegen(
    commonSources = listOf(source(commonSource)),
    platformSources = listOf(invokableSource(platformSource)),
    config = config,
    assertions = assertions
  )
}

fun multiPlatformCodegen(
  commonSources: List<SourceFile>,
  platformSources: List<SourceFile>,
  config: KotlinCompilation.() -> Unit = {},
  assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() },
) {
  val result = compile {
    kotlincArguments += "-Xmulti-platform=true"
    commonSources
      .map {
        SourceFile::class.java
          .declaredMethods
          .first { it.name.startsWith("writeIfNeeded") }
          .invoke(it, workingDir.resolve("sources").also { it.mkdirs() })
      }
      .forEach { kotlincArguments += "-Xcommon-sources=$it" }
    sources = platformSources + commonSources
    config()
  }
  assertions(
    object : KotlinCompilationAssertionScope {
      override val result: JvmCompilationResult
        get() = result
    }
  )
}

fun compile(block: KotlinCompilation.() -> Unit = {}): JvmCompilationResult {
  fun baseCompilation(block: KotlinCompilation.() -> Unit) = KotlinCompilation().apply {
    inheritClassPath = true
    jvmTarget = "1.8"
    verbose = false
    block()
  }

  val kspCompilation = baseCompilation {
    configureKsp(useKsp2 = false) {
      symbolProcessorProviders += InjektSymbolProcessor.Provider()
      incremental = false
      withCompilation = true
    }

    supportsK2 = false
    languageVersion = "1.9"

    compilerPluginRegistrars += object : CompilerPluginRegistrar() {
      override val supportsK2: Boolean
        get() = false
      override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(
          object : IrGenerationExtension {
            override fun generate(
              moduleFragment: IrModuleFragment,
              pluginContext: IrPluginContext
            ) {
              dumpAllFiles = true
              moduleFragment.dumpToFiles(
                workingDir.resolve("injekt/dump")
                  .also { it.mkdirs() },
                InjektContext()
              )
            }
          }
        )
      }
    }

    block()
  }

  val kspResult = kspCompilation.compile()

  kspResult.generatedFiles.forEach {
    if (it.absolutePath.endsWith(".class") &&
      "ProvidersMarker" !in it.absolutePath &&
      "Injectables" !in it.absolutePath
    ) {
      it.delete()
    }
  }

  val pluginCompilation = baseCompilation {
    classpaths += kspCompilation.classesDir

    dumpAllFiles = true
    compilerPluginRegistrars += InjektCompilerPluginRegistrar()
    commandLineProcessors += InjektCommandLineProcessor()
    pluginOptions += PluginOption(
      "com.ivianuu.injekt",
      "dumpDir",
      workingDir.resolve("injekt/dump")
        .also { it.mkdirs() }
        .absolutePath
    )

    block()
  }

  return pluginCompilation.compile()
}

interface KotlinCompilationAssertionScope {
  val result: JvmCompilationResult
  val classLoader: ClassLoader get() = result.classLoader
}

fun KotlinCompilationAssertionScope.compilationShouldBeOk() {
  result.exitCode shouldBe KotlinCompilation.ExitCode.OK
}

@JvmName("invokeSingleFileTypeless")
fun KotlinCompilationAssertionScope.invokeSingleFile(vararg args: Any?): Any? =
  invokeSingleFile<Any?>(*args)

fun <T> KotlinCompilationAssertionScope.invokeSingleFile(vararg args: Any?): T {
  val generatedClass = classLoader.getSingleClass().java
  return generatedClass.declaredMethods
    .single { it.name == "invoke" && it.parameterTypes.size == args.size }
    .invoke(null, *args) as T
}

private fun ClassLoader.getSingleClass(): KClass<*> =
  loadClass("com.ivianuu.injekt.integrationtests.FileKt").kotlin

fun KotlinCompilationAssertionScope.compilationShouldHaveFailed(message: String? = null) {
  result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
  message?.let { shouldContainMessage(message) }
}

fun KotlinCompilationAssertionScope.shouldContainMessage(message: String) {
  result.messages shouldContain message
}
