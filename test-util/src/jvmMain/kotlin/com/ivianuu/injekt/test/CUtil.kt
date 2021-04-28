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

@file:Suppress("UNCHECKED_CAST")

package com.ivianuu.injekt.test

import androidx.compose.compiler.plugins.kotlin.*
import com.ivianuu.injekt.compiler.*
import com.tschuchort.compiletesting.*
import io.kotest.matchers.*
import io.kotest.matchers.string.*
import org.intellij.lang.annotations.*
import org.jetbrains.kotlin.name.*
import java.net.*
import java.nio.file.*
import kotlin.reflect.*

var fileIndex = 0

val defaultGivenImports = listOf(
    "com.ivianuu.injekt.*",
    "com.ivianuu.injekt.common.*",
    "com.ivianuu.injekt.scope.*"
)

fun source(
    @Language("kotlin") source: String,
    name: String = "File${fileIndex++}.kt",
    injektImports: Boolean = true,
    packageFqName: FqName = FqName("com.ivianuu.injekt.integrationtests"),
    givenImports: List<String> = defaultGivenImports
) = SourceFile.kotlin(
    name = name,
    contents = buildString {
        if (injektImports) {
            appendLine(
                "@file:GivenImports(${givenImports.joinToString(",\n") { "\"$it\"" }}) "
            )
            appendLine()
            appendLine("package $packageFqName")
            appendLine()
            appendLine("import androidx.compose.runtime.*")
            appendLine("import com.ivianuu.injekt.*")
            appendLine("import com.ivianuu.injekt.common.*")
            appendLine("import com.ivianuu.injekt.internal.*")
            appendLine("import com.ivianuu.injekt.scope.*")
            appendLine("import com.ivianuu.injekt.test.*")
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
    @Language("kotlin") source1: String,
    @Language("kotlin") source2: String,
    @Language("kotlin") source3: String,
    config: KotlinCompilation.() -> Unit = {},
    assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() },
) = codegen(
    sources = listOf(source(source1), source(source2), invokableSource(source3)),
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
            override val result: KotlinCompilation.Result
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
    @Language("kotlin") source1: String,
    @Language("kotlin") source2: String,
    @Language("kotlin") source3: String,
    config: KotlinCompilation.(Int) -> Unit = {},
    assertions: KotlinCompilationAssertionScope.(Boolean) -> Unit = { compilationShouldBeOk() }
) {
    singleAndMultiCodegen(
        listOf(listOf(source(source1)), listOf(source(source2)), listOf(invokableSource(source3))),
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
        config(-1)
    }, { assertions(false) })
    multiCodegen(sources, {
        workingDir = Files.createTempDirectory("multi-compilation").toFile()
        config(it)
    }, { assertions(true) })
}

fun multiCodegen(
    @Language("kotlin") source1: String,
    @Language("kotlin") source2: String,
    config: KotlinCompilation.(Int) -> Unit = {},
    assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() }
) {
    multiCodegen(listOf(listOf(source(source1)), listOf(invokableSource(source2))), config, assertions)
}

fun multiCodegen(
    @Language("kotlin") source1: String,
    @Language("kotlin") source2: String,
    @Language("kotlin") source3: String,
    config: KotlinCompilation.(Int) -> Unit = {},
    assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() }
) {
    multiCodegen(listOf(listOf(source(source1)), listOf(source(source2)), listOf(
        invokableSource(source3))), config, assertions)
}

fun multiCodegen(
    sources: List<List<SourceFile>>,
    config: KotlinCompilation.(Int) -> Unit = {},
    assertions: KotlinCompilationAssertionScope.() -> Unit = { compilationShouldBeOk() }
) {
    val prevCompilations = mutableListOf<KotlinCompilation>()
    val results = sources.mapIndexed { index, sourceFiles ->
        compile {
            this.sources = sourceFiles
            this.classpaths += prevCompilations.map { it.classesDir }
            config(index)
            prevCompilations += this
        }
    }
    object : KotlinCompilationAssertionScope {
        override val result: KotlinCompilation.Result
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
            .map { SourceFileAccessor.writeIfNeeded(it, workingDir.resolve("sources")
                .also { it.mkdirs() }) }
            .forEach { kotlincArguments += "-Xcommon-sources=$it" }
        this.sources = platformSources + commonSources
        this.compile()
        config(this)
    }
    assertions(
        object : KotlinCompilationAssertionScope {
            override val result: KotlinCompilation.Result
                get() = result
        }
    )
}

fun compilation(block: KotlinCompilation.() -> Unit = {}) = KotlinCompilation().apply {
    compilerPlugins = listOf(InjektComponentRegistrar(), ComposeComponentRegistrar())
    commandLineProcessors = listOf(InjektCommandLineProcessor(), ComposeCommandLineProcessor())
    inheritClassPath = true
    useIR = true
    jvmTarget = "1.8"
    verbose = false
    kotlincArguments += "-XXLanguage:+NewInference"
    pluginOptions += PluginOption(
        "com.ivianuu.injekt",
        "dumpDir",
        workingDir.resolve("injekt/dump").absolutePath
    )
    block()
}

fun compile(block: KotlinCompilation.() -> Unit = {}) = compilation(block).compile()

fun KotlinCompilationAssertionScope.compilationShouldBeOk() {
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
}

interface KotlinCompilationAssertionScope {
    val result: KotlinCompilation.Result
    val classLoader: ClassLoader get() = result.classLoader
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

fun KotlinCompilationAssertionScope.shouldNotContainMessage(message: String) {
    result.messages shouldNotContain message
}

@Suppress("Assert")
inline fun KotlinCompilationAssertionScope.irAssertions(block: (String) -> Unit) {
    compilationShouldBeOk()
    result.outputDirectory
        .parentFile
        .resolve("injekt/dump")
        .walkTopDown()
        .filter { it.isFile }
        .map { it.readText() }
        .joinToString("\n")
        .also {
            assert(it.isNotEmpty()) {
                "Source is empty"
            }
        }
        .let(block)
}

@Suppress("Assert")
fun KotlinCompilationAssertionScope.irShouldContain(times: Int, text: String) {
    irAssertions {
        val matchesCount = it.countMatches(text)
        assert(matchesCount == times) {
            "expected '$text' $times times but was found $matchesCount times in '$it'"
        }
    }
}

private fun String.countMatches(other: String): Int = split(other)
    .dropLastWhile { it.isEmpty() }.size - 1

@Suppress("Assert")
fun KotlinCompilationAssertionScope.irShouldNotContain(text: String) {
    irAssertions {
        assert(text !in it) {
            "'$text' in source '$it'"
        }
    }
}
