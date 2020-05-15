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

package com.ivianuu.injekt

import com.ivianuu.injekt.compiler.InjektComponentRegistrar
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.github.classgraph.ClassGraph
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.intellij.lang.annotations.Language
import java.io.File
import kotlin.reflect.KClass

var fileIndex = 0

fun source(
    @Language("kotlin") source: String,
    name: String = "File${fileIndex++}.kt",
    injektImports: Boolean = true
) = SourceFile.kotlin(
    name = name,
    contents = buildString {
        if (injektImports) {
            appendln("import com.ivianuu.injekt.*")
            appendln("import com.ivianuu.injekt.internal.*")
            appendln("import com.ivianuu.injekt.*")
            appendln("import kotlin.reflect.*")
            appendln()
        }

        append(source)
    }
)

fun singleSource(
    @Language("kotlin") source: String,
    name: String = "File.kt",
    injektImports: Boolean = true
) = source(source, name, injektImports)

fun codegen(
    @Language("kotlin") source: String,
    assertions: KotlinCompilation.Result.() -> Unit = { assertOk() }
) = codegen(
    singleSource(source), assertions = assertions
)

fun codegen(
    vararg sources: SourceFile,
    assertions: KotlinCompilation.Result.() -> Unit = { assertOk() }
) {
    val result = compile { this.sources = sources.toList() }
    println("Result: ${result.exitCode} m: ${result.messages}")
    assertions(result)
}

fun multiCodegen(
    vararg sources: List<SourceFile>,
    assertions: (List<KotlinCompilation.Result>) -> Unit = { it.forEach { it.assertOk() } }
) {
    val results = sources.scan(null) { prevCompilation: KotlinCompilation.Result?, sourceFiles ->
        compile {
            this.sources = sourceFiles
            if (prevCompilation != null) {
                val classGraph = ClassGraph()
                    .addClassLoader(prevCompilation.classLoader)
                val classpaths = classGraph.classpathFiles
                val modules = classGraph.modules.mapNotNull { it.locationFile }
                this.classpaths += (classpaths + modules).distinctBy(File::getAbsolutePath)
            }
        }
    }.filterNotNull()
    assertions(results)
}

fun compilation(block: KotlinCompilation.() -> Unit = {}) = KotlinCompilation().apply {
    compilerPlugins = listOf(InjektComponentRegistrar())
    inheritClassPath = true
    useIR = true
    jvmTarget = "1.8"
    verbose = false
    block()
}

fun compile(block: KotlinCompilation.() -> Unit = {}) = compilation(
    block
).compile()

fun KotlinCompilation.Result.assertOk() {
    assertEquals(KotlinCompilation.ExitCode.OK, exitCode)
}

fun KotlinCompilation.Result.expectNoErrorsWhileInvokingSingleFile() {
    assertOk()

    try {
        invokeSingleFile()
    } catch (e: Exception) {
        throw AssertionError(e)
    }
}

@JvmName("invokeSingleFileTypeless")
fun KotlinCompilation.Result.invokeSingleFile(vararg args: Any?): Any? =
    invokeSingleFile<Any?>(*args)

fun <T> KotlinCompilation.Result.invokeSingleFile(vararg args: Any?): T {
    val generatedClass = getSingleClass().java
    return generatedClass.declaredMethods
        .single { it.name == "invoke" && it.parameterCount == args.size }
        .invoke(null, *args) as T
}

private fun KotlinCompilation.Result.getSingleClass(): KClass<*> =
    classLoader.loadClass("FileKt").kotlin

fun KotlinCompilation.Result.assertInternalError(
    message: String? = null
) {
    assertEquals(KotlinCompilation.ExitCode.INTERNAL_ERROR, exitCode)
    message?.let { assertTrue(messages.toLowerCase().contains(it.toLowerCase())) }
}

fun KotlinCompilation.Result.assertCompileError(
    message: String? = null
) {
    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, exitCode)
    message?.let { assertTrue(messages.toLowerCase().contains(it.toLowerCase())) }
}

class Foo

class Bar(foo: Foo)

interface TestComponent

@Scope
annotation class TestScope

@Scope
annotation class TestScope2

interface Command

class CommandA : Command

class CommandB : Command

class CommandC : Command
