/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UNCHECKED_CAST")
@file:OptIn(ExperimentalCompilerApi::class, ExperimentalCompilerApi::class)

package injekt.integrationtests

import com.tschuchort.compiletesting.*
import injekt.compiler.*
import injekt.compiler.ir.*
import injekt.compiler.ir.dumpAllFiles
import injekt.compiler.ir.dumpToFiles
import injekt.ksp.*
import io.kotest.matchers.*
import io.kotest.matchers.string.*
import org.intellij.lang.annotations.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.name.*
import java.net.*
import java.nio.file.*
import kotlin.reflect.*

var fileIndex = 0

fun sourceOf(
  @Language("kotlin") source: String,
  name: String = "File${fileIndex++}.kt",
  injektImports: Boolean = true,
  packageFqName: FqName = FqName("injekt.integrationtests")
) = SourceFile.kotlin(
  name = name,
  contents = buildString {
    if (injektImports) {
      appendLine()
      appendLine("package $packageFqName")
      appendLine()
      appendLine("import androidx.compose.runtime.*")
      appendLine("import injekt.*")
      appendLine("import injekt.common.*")
      appendLine("import injekt.internal.*")
      appendLine("import kotlin.reflect.*")
      appendLine("import kotlinx.coroutines.*")
      appendLine()
    }

    append(source)
  }
)

fun invokableSourceOf(
  @Language("kotlin") source: String,
  injektImports: Boolean = true,
) = sourceOf(source, "File.kt", injektImports)

fun codegen(
  @Language("kotlin") source1: String,
  config: KotlinCompilation.() -> Unit = {},
  assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() },
) = codegen(
  sources = listOf(invokableSourceOf(source1)),
  config = config,
  assertions = assertions
)

fun codegen(
  @Language("kotlin") source1: String,
  @Language("kotlin") source2: String,
  config: KotlinCompilation.() -> Unit = {},
  assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() },
) = codegen(
  sources = listOf(sourceOf(source1), invokableSourceOf(source2)),
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
    listOf(listOf(sourceOf(source1)), listOf(invokableSourceOf(source2))),
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
    listOf(listOf(sourceOf(source1)), listOf(invokableSourceOf(source2))),
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
      "InjectablesMarker" !in it.absolutePath &&
      "Injectables" !in it.absolutePath
    ) {
      it.delete()
    }
  }

  val pluginCompilation = baseCompilation {
    classpaths += kspCompilation.classesDir

    dumpAllFiles = true
    componentRegistrars += InjektComponentRegistrar()
    commandLineProcessors += InjektCommandLineProcessor()
    pluginOptions += PluginOption(
      "injekt",
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
  loadClass("injekt.integrationtests.FileKt").kotlin

fun KotlinCompilationAssertionScope.compilationShouldHaveFailed(message: String? = null) {
  result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
  message?.let { shouldContainMessage(message) }
}

fun KotlinCompilationAssertionScope.shouldContainMessage(message: String) {
  result.messages shouldContain message
}
