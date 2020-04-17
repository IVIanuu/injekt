package com.ivianuu.injekt.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import junit.framework.Assert.assertEquals
import org.intellij.lang.annotations.Language
import org.junit.Test

fun codegenTest(
    @Language("kotlin") source: String,
    fileName: String = "Test.kt",
    injektImports: Boolean = true,
    assertions: KotlinCompilation.Result.() -> Unit = {}
) {
    val result = KotlinCompilation().apply {
        sources = listOf(SourceFile.kotlin(
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

fun KotlinCompilation.Result.assertOk() = assertEquals(KotlinCompilation.ExitCode.OK, exitCode)

class ModuleTest {

    @Test
    fun simpleModule() = codegenTest(
        """ 
        @Module
        fun ComponentDsl.myModule() {
        }
        """
    ) {
        assertOk()
    }

    @Test
    fun testWithDeps() = codegenTest(
        """ 
        @Module
        fun ComponentDsl.myModule() {
            factory { "" }
        }
        """
    ) {
        assertOk()
    }


}