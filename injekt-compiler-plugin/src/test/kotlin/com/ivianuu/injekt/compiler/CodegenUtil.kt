package com.ivianuu.injekt.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import junit.framework.Assert.assertEquals
import org.intellij.lang.annotations.Language

fun codegenTest(
    @Language("kotlin") source: String,
    fileName: String = "Test.kt",
    injektImports: Boolean = true,
    assertions: KotlinCompilation.Result.() -> Unit = {}
) {
    val result = KotlinCompilation().apply {
        sources = listOf(
            SourceFile.kotlin(
                fileName,
                buildString {
                    if (injektImports) {
                        appendln("import com.ivianuu.injekt.*")
                        appendln()
                    }

                    append(source)
                }
            ))
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