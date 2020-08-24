package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.compilation
import com.ivianuu.injekt.test.source
import com.tschuchort.compiletesting.MainComponentRegistrar
import com.tschuchort.compiletesting.SourceFile
import io.github.classgraph.ClassGraph
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.ICReporterBase
import org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunner
import org.jetbrains.kotlin.incremental.IncrementalModuleEntry
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJvm
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files

class IncrementalTest {

    @Test
    fun test() {
        val projectRoot = Files.createTempDirectory("root")
            .toFile().also { it.mkdirs() }
        val workingDir = projectRoot.resolve("workingDir")
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
        )

        var changingCalleeSource = source(
            """
                @Reader
                fun changingCallee() {
                }
            """,
            name = "ChangingCallee.kt",
            initializeInjekt = false
        )

        val notChangingCalleeSource = source(
            """
                @Reader
                fun notChangingCallee() {
                }
            """,
            name = "NotChangingCallee.kt",
            initializeInjekt = false
        )

        val contextSource = source(
            """
                @InitializeInjekt
                val context = rootContext<TestContext>("hello")
            """,
            name = "Context.kt",
            initializeInjekt = false
        )

        val runReaderSource = source(
            """
                val result = context.runReader { caller() }
            """,
            name = "RunReader.kt",
            initializeInjekt = false
        )

        val unrelatedSource = source(
            """
                class Unrelated
            """,
            name = "Unrelated.kt",
            initializeInjekt = false
        )

        compileIncrementally(
            moduleName = "module",
            projectRoot = projectRoot,
            workingDir = workingDir,
            sources = listOf(
                callerSource,
                changingCalleeSource,
                notChangingCalleeSource,
                contextSource,
                runReaderSource,
                unrelatedSource
            ),
            otherModules = emptyList(),
            providedChangedFiles = null
        ).run {
            assertTrue("Caller.kt is marked dirty" in messageOutput)
            assertTrue("ChangingCallee.kt is marked dirty" in messageOutput)
            assertTrue("NotChangingCallee.kt is marked dirty" in messageOutput)
            assertTrue("Context.kt is marked dirty" in messageOutput)
            assertTrue("RunReader.kt is marked dirty" in messageOutput)
            assertTrue("Unrelated.kt is marked dirty" in messageOutput)
        }

        changingCalleeSource = source(
            """
                @Reader
                fun changingCallee() {
                }
            """,
            name = "ChangingCallee.kt",
            initializeInjekt = false
        )

        compileIncrementally(
            moduleName = "module",
            projectRoot = projectRoot,
            workingDir = workingDir,
            sources = listOf(
                callerSource,
                changingCalleeSource,
                notChangingCalleeSource,
                contextSource,
                runReaderSource,
                unrelatedSource
            ),
            otherModules = emptyList(),
            providedChangedFiles = null
        ).run {
            assertTrue("Caller.kt is marked dirty" !in messageOutput)
            assertTrue("ChangingCallee.kt is marked dirty" !in messageOutput)
            assertTrue("NotChangingCallee.kt is marked dirty" !in messageOutput)
            assertTrue("Context.kt is marked dirty" !in messageOutput)
            assertTrue("RunReader.kt is marked dirty" !in messageOutput)
            assertTrue("Unrelated.kt is marked dirty" !in messageOutput)
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
        )

        compileIncrementally(
            moduleName = "module",
            projectRoot = projectRoot,
            workingDir = workingDir,
            sources = listOf(
                callerSource,
                changingCalleeSource,
                notChangingCalleeSource,
                contextSource,
                runReaderSource,
                unrelatedSource
            ),
            otherModules = emptyList(),
            providedChangedFiles = null
        ).run {
            assertTrue("Caller.kt is marked dirty" in messageOutput)
            assertTrue("ChangingCallee.kt is marked dirty" in messageOutput)
            assertTrue("NotChangingCallee.kt is marked dirty" !in messageOutput)
            assertTrue("Context.kt is marked dirty" in messageOutput)
            assertTrue("RunReader.kt is marked dirty" in messageOutput)
            assertTrue("Unrelated.kt is marked dirty" !in messageOutput)
        }
    }

    @Test
    fun testMulti() {
        val projectRoot = Files.createTempDirectory("root")
            .toFile().also { it.mkdirs() }

        val workingDirA = projectRoot.resolve("workingDirA")
            .also { it.mkdirs() }
        val workingDirB = projectRoot.resolve("workingDirB")
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
        )

        var changingCalleeSource = source(
            """
                @Reader
                fun changingCallee() {
                }
            """,
            name = "ChangingCallee.kt",
            initializeInjekt = false
        )

        val notChangingCalleeSource = source(
            """
                @Reader
                fun notChangingCallee() {
                }
            """,
            name = "NotChangingCallee.kt",
            initializeInjekt = false
        )

        val contextSource = source(
            """
                @InitializeInjekt
                val context = rootContext<TestContext>("hello")
            """,
            name = "Context.kt",
            initializeInjekt = false
        )

        val runReaderSource = source(
            """
                val result = context.runReader { caller() }
            """,
            name = "RunReader.kt",
            initializeInjekt = false
        )

        val unrelatedSource = source(
            """
                class Unrelated
            """,
            name = "Unrelated.kt",
            initializeInjekt = false
        )

        val resultA1 = compileIncrementally(
            moduleName = "modulea",
            projectRoot = projectRoot,
            workingDir = workingDirA,
            sources = listOf(
                changingCalleeSource,
                notChangingCalleeSource,
                unrelatedSource
            ),
            otherModules = emptyList(),
            providedChangedFiles = null
        ).apply {
            assertTrue("ChangingCallee.kt is marked dirty" in messageOutput)
            assertTrue("NotChangingCallee.kt is marked dirty" in messageOutput)
            assertTrue("Unrelated.kt is marked dirty" in messageOutput)
        }
        val resultB1 = compileIncrementally(
            moduleName = "moduleb",
            projectRoot = projectRoot,
            workingDir = workingDirB,
            sources = listOf(
                callerSource,
                contextSource,
                runReaderSource
            ),
            otherModules = listOf(
                Module(
                    "modulea",
                    workingDirA,
                    resultA1.classLoader
                )
            ),
            providedChangedFiles = null
        ).apply {
            assertTrue("Caller.kt is marked dirty" in messageOutput)
            assertTrue("Context.kt is marked dirty" in messageOutput)
            assertTrue("RunReader.kt is marked dirty" in messageOutput)
        }

        changingCalleeSource = source(
            """
                @Reader
                fun changingCallee() {
                }
            """,
            name = "ChangingCallee.kt",
            initializeInjekt = false
        )

        val resultA2 = compileIncrementally(
            moduleName = "modulea",
            projectRoot = projectRoot,
            workingDir = workingDirA,
            sources = listOf(
                changingCalleeSource,
                notChangingCalleeSource,
                unrelatedSource
            ),
            otherModules = emptyList(),
            providedChangedFiles = null
        ).apply {
            assertTrue("ChangingCallee.kt is marked dirty" !in messageOutput)
            assertTrue("NotChangingCallee.kt is marked dirty" !in messageOutput)
            assertTrue("Unrelated.kt is marked dirty" !in messageOutput)
        }
        val resultB2 = compileIncrementally(
            moduleName = "moduleb",
            projectRoot = projectRoot,
            workingDir = workingDirB,
            sources = listOf(
                callerSource,
                contextSource,
                runReaderSource
            ),
            otherModules = listOf(
                Module(
                    "modulea",
                    workingDirA,
                    resultA2.classLoader
                )
            ),
            providedChangedFiles = null
        ).apply {
            assertTrue("Caller.kt is marked dirty" !in messageOutput)
            assertTrue("Context.kt is marked dirty" !in messageOutput)
            assertTrue("RunReader.kt is marked dirty" !in messageOutput)
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
        )

        val resultA3 = compileIncrementally(
            moduleName = "modulea",
            projectRoot = projectRoot,
            workingDir = workingDirA,
            sources = listOf(
                changingCalleeSource,
                notChangingCalleeSource,
                unrelatedSource
            ),
            otherModules = emptyList(),
            providedChangedFiles = null
        ).apply {
            assertTrue("ChangingCallee.kt is marked dirty" in messageOutput)
            assertTrue("NotChangingCallee.kt is marked dirty" !in messageOutput)
            assertTrue("Unrelated.kt is marked dirty" !in messageOutput)
        }
        val resultB3 = compileIncrementally(
            moduleName = "moduleb",
            projectRoot = projectRoot,
            workingDir = workingDirB,
            sources = listOf(
                callerSource,
                contextSource,
                runReaderSource
            ),
            otherModules = listOf(
                Module(
                    "modulea",
                    workingDirA,
                    resultA3.classLoader
                )
            ),
            providedChangedFiles = ChangedFiles.Known(
                modified = listOf(resultA3.fileBySourceFile.getValue(changingCalleeSource)),
                removed = emptyList()
            )
        ).apply {
            assertTrue("Caller.kt is marked dirty" in messageOutput)
            assertTrue("Context.kt is marked dirty" in messageOutput)
            assertTrue("RunReader.kt is marked dirty" in messageOutput)
        }
    }

    private class Result(
        val messageOutput: String,
        val classLoader: ClassLoader,
        val fileBySourceFile: Map<SourceFile, File>
    )

    private class Module(
        val name: String,
        val workingDir: File,
        val classLoader: ClassLoader?
    )

    private fun compileIncrementally(
        moduleName: String,
        workingDir: File,
        projectRoot: File,
        sources: List<SourceFile>,
        otherModules: List<Module>,
        providedChangedFiles: ChangedFiles?
    ): Result {
        println("compile start $moduleName")
        println()
        val srcDir = workingDir.resolve("src")
            .also { it.mkdirs() }
        val fileBySourceFile = sources.associateWith { it.writeIfNeeded(srcDir) }
        val messages = mutableListOf<String>()

        val kotlinCompileArgs = compilation {
            this.workingDir = workingDir
            this.moduleName = moduleName
            val classGraph = ClassGraph()
            otherModules.forEach {
                if (it.classLoader != null) classGraph.addClassLoader(it.classLoader)
            }
            val classpaths = classGraph.classpathFiles
            val modules = classGraph.modules.mapNotNull { it.locationFile }
            this.classpaths += (classpaths + modules).distinctBy(File::getAbsolutePath)
        }

        val args = kotlinCompileArgs.commonK2JVMArgs().apply {
            MainComponentRegistrar.threadLocalParameters.set(
                MainComponentRegistrar.ThreadLocalParameters(
                    listOf(),
                    KaptOptions.Builder(),
                    kotlinCompileArgs.compilerPlugins
                )
            )
            pluginClasspaths =
                (pluginClasspaths ?: emptyArray()) + arrayOf(kotlinCompileArgs.getResourcesPath())
        }

        val cachesDir = workingDir.resolve("caches")
        val kotlinExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
        val allExtensions = kotlinExtensions + "java"
        val rootsWalk = listOf(srcDir).asSequence().flatMap { it.walk() }
        val files = rootsWalk.filter(File::isFile)
        val sourceFiles = files.filter { it.extension.toLowerCase() in allExtensions }.toList()
        val buildHistoryFile = File(cachesDir, "build-history.bin")
        args.javaSourceRoots = listOf(srcDir).map { it.absolutePath }.toTypedArray()

        val moduleEntries = otherModules.associateWith {
            IncrementalModuleEntry(
                it.workingDir.path,
                it.name,
                it.workingDir.resolve("build").also { it.mkdirs() },
                it.workingDir.resolve("caches").resolve("build-history.bin")
            )
        }

        val moduleApiHistory = ModulesApiHistoryJvm(
            IncrementalModuleInfo(
                projectRoot,
                moduleEntries.mapKeys { it.key.workingDir.resolve("classes") }
                    .also { println(it) },
                moduleEntries.mapKeys { it.key.name }.mapValues { setOf(it.value) }
                    .also { println(it) },
                emptyMap(),
                emptyMap()
            )
        )

        val reporter = object : ICReporterBase() {
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

        val compiler = IncrementalJvmCompilerRunner(
            cachesDir,
            reporter,
            usePreciseJavaTracking = true,
            outputFiles = emptyList(),
            buildHistoryFile = buildHistoryFile,
            modulesApiHistory = moduleApiHistory,
            kotlinSourceFilesExtensions = kotlinExtensions
        )

        IncrementalCompilation.setIsEnabledForJvm(true)
        compiler.compile(sourceFiles, args, MessageCollector.NONE, providedChangedFiles)

        val classLoader = URLClassLoader(
            arrayOf(
                *(kotlinCompileArgs.classpaths + kotlinCompileArgs.classesDir)
                    .map { it.toURI().toURL() }.toTypedArray()
            ),
            this::class.java.classLoader
        )
        println()
        println("compile end $moduleName")
        println()
        return Result(
            messages.joinToString("\n"),
            classLoader,
            fileBySourceFile
        )
    }

}
