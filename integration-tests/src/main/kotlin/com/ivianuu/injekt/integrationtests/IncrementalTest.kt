package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.compilation
import com.ivianuu.injekt.test.source
import com.tschuchort.compiletesting.MainComponentRegistrar
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.ICReporterBase
import org.jetbrains.kotlin.incremental.makeIncrementally
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class IncrementalTest {

    @Test
    fun test() {
        val cachesDir = Files.createTempDirectory("caches_dir").toFile()
            .also { it.mkdirs() }

        val srcDir = Files.createTempDirectory("src_dir").toFile()
            .also { it.mkdirs() }

        val callerSource = source(
            """
                @Given
                fun caller() {
                    changingCallee()
                    notChangingCallee()
                }
            """,
            name = "Caller.kt",
            initializeInjekt = false
        ).also { it.writeIfNeeded(srcDir) }

        var changingCalleeSource = source(
            """
                @Reader
                fun changingCallee() {
                }
            """,
            name = "ChangingCallee.kt",
            initializeInjekt = false
        ).also { it.writeIfNeeded(srcDir) }

        val notChangingCallee = source(
            """
                @Reader
                fun notChangingCallee() {
                }
            """,
            name = "NotChangingCallee.kt",
            initializeInjekt = false
        ).also { it.writeIfNeeded(srcDir) }

        val contextSource = source(
            """
                @InitializeInjekt
                val context = rootContext<TestContext>("hello")
            """,
            name = "Context.kt",
            initializeInjekt = false
        ).also { it.writeIfNeeded(srcDir) }

        val runReaderSource = source(
            """
                val result = context.runReader { caller() }
            """,
            name = "RunReader.kt",
            initializeInjekt = false
        ).also { it.writeIfNeeded(srcDir) }

        val unrelatedSource = source(
            """
                class Unrelated
            """,
            name = "Unrelated.kt",
            initializeInjekt = false
        ).also { it.writeIfNeeded(srcDir) }

        fun doCompile(): String {
            val messages = mutableListOf<String>()
            makeIncrementally(
                cachesDir,
                listOf(srcDir),
                compilation {
                    moduleName = "module"
                }.run {
                    commonK2JVMArgs().apply {
                        MainComponentRegistrar.threadLocalParameters.set(
                            MainComponentRegistrar.ThreadLocalParameters(
                                listOf(),
                                KaptOptions.Builder(),
                                compilerPlugins
                            )
                        )
                        pluginClasspaths =
                            (pluginClasspaths ?: emptyArray()) + arrayOf(getResourcesPath())
                    }
                },
                MessageCollector.NONE,
                object : ICReporterBase() {
                    override fun reportCompileIteration(
                        incremental: Boolean,
                        sourceFiles: Collection<File>,
                        exitCode: ExitCode
                    ) {
                    }

                    override fun report(message: () -> String) {
                        println(message())
                        messages += message()
                    }

                    override fun reportVerbose(message: () -> String) {
                        println(message())
                        messages += message()
                    }
                }
            )
            return messages.joinToString("\n")
        }
        doCompile().run {
            assertTrue("Caller.kt is marked dirty" in this)
            assertTrue("ChangingCallee.kt is marked dirty" in this)
            assertTrue("NotChangingCallee.kt is marked dirty" in this)
            assertTrue("Context.kt is marked dirty" in this)
            assertTrue("RunReader.kt is marked dirty" in this)
            assertTrue("Unrelated.kt is marked dirty" in this)
        }

        changingCalleeSource = source(
            """
                @Reader
                fun changingCallee() {
                }
            """,
            name = "ChangingCallee.kt",
            initializeInjekt = false
        ).also { it.writeIfNeeded(srcDir) }

        doCompile().run {
            assertTrue("Caller.kt is marked dirty" !in this)
            assertTrue("ChangingCallee.kt is marked dirty" !in this)
            assertTrue("NotChangingCallee.kt is marked dirty" !in this)
            assertTrue("Context.kt is marked dirty" !in this)
            assertTrue("RunReader.kt is marked dirty" !in this)
            assertTrue("Unrelated.kt is marked dirty" !in this)
        }

        changingCalleeSource = source(
            """
                @Reader
                fun changingCallee() {
                    given<String>()
                }
            """,
            name = "ChangingCallee.kt",
            initializeInjekt = false
        ).also { it.writeIfNeeded(srcDir) }

        doCompile().run {
            assertTrue("Caller.kt is marked dirty" in this)
            assertTrue("ChangingCallee.kt is marked dirty" in this)
            assertTrue("NotChangingCallee.kt is marked dirty" !in this)
            assertTrue("Context.kt is marked dirty" in this)
            assertTrue("RunReader.kt is marked dirty" in this)
            assertTrue("Unrelated.kt is marked dirty" !in this)
        }

    }

}
