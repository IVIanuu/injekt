package com.ivianuu.injekt.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import junit.framework.Assert.assertEquals
import org.intellij.lang.annotations.Language

fun source(
    @Language("kotlin") source: String,
    name: String = "File.kt",
    injektImports: Boolean = true
) = SourceFile.kotlin(
    name = name,
    contents = buildString {
        if (injektImports) {
            appendln("import com.ivianuu.injekt.*")
            appendln()
        }

        append(source)
    }
)

fun codegenTest(
    source: String,
    assertions: KotlinCompilation.Result.() -> Unit = {}
) = codegenTest(source(source), assertions = assertions)

fun codegenTest(
    vararg sources: SourceFile,
    assertions: KotlinCompilation.Result.() -> Unit = {}
) {
    val result = KotlinCompilation().apply {
        this.sources = sources.toList()
        compilerPlugins = listOf(InjektComponentRegistrar())
        inheritClassPath = true
        useIR = true
        jvmTarget = "1.8"
        verbose = false

    }.compile()
    println("Result: ${result.exitCode} m: ${result.messages}")
    assertions(result)
}

fun KotlinCompilation.Result.assertOk() =
    assertEquals(KotlinCompilation.ExitCode.OK, exitCode)

fun KotlinCompilation.Result.assertInternalError() =
    assertEquals(KotlinCompilation.ExitCode.INTERNAL_ERROR, exitCode)