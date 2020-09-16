package com.ivianuu.injekt.integrationtests

/**
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
                class Logger
                @Reader
                inline fun changingCallee() {
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
                fun invoke() {
                    val result = context.runReader { caller() }
                }
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
            assertFalse(hasErrors)
            assertTrue("Caller.kt is marked dirty" in messageOutput)
            assertTrue("ChangingCallee.kt is marked dirty" in messageOutput)
            assertTrue("NotChangingCallee.kt is marked dirty" in messageOutput)
            assertTrue("Context.kt is marked dirty" in messageOutput)
            assertTrue("RunReader.kt is marked dirty" in messageOutput)
            assertTrue("Unrelated.kt is marked dirty" in messageOutput)

            classLoader.loadClass("com.ivianuu.injekt.integrationtests.RunReaderKt")
                .declaredMethods
                .single { it.name == "invoke" }
                .invoke(null)
        }

        changingCalleeSource = source(
            """
                class Logger
                @Reader
                inline fun changingCallee() {
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
            assertFalse(hasErrors)
            assertTrue("Caller.kt is marked dirty" !in messageOutput)
            assertTrue("ChangingCallee.kt is marked dirty" !in messageOutput)
            assertTrue("NotChangingCallee.kt is marked dirty" !in messageOutput)
            assertTrue("Context.kt is marked dirty" !in messageOutput)
            assertTrue("RunReader.kt is marked dirty" !in messageOutput)
            assertTrue("Unrelated.kt is marked dirty" !in messageOutput)
            classLoader.loadClass("com.ivianuu.injekt.integrationtests.RunReaderKt")
                .declaredMethods
                .single { it.name == "invoke" }
                .invoke(null)
        }

        changingCalleeSource = source(
            """
                class Logger
                @Reader
                inline fun changingCallee() {
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
            assertFalse(hasErrors)
            assertTrue("Caller.kt is marked dirty" in messageOutput)
            assertTrue("ChangingCallee.kt is marked dirty" in messageOutput)
            assertTrue("NotChangingCallee.kt is marked dirty" !in messageOutput)
            assertTrue("Context.kt is marked dirty" in messageOutput)
            assertTrue("RunReader.kt is marked dirty" in messageOutput)
            assertTrue("Unrelated.kt is marked dirty" !in messageOutput)
            classLoader.loadClass("com.ivianuu.injekt.integrationtests.RunReaderKt")
                .declaredMethods
                .single { it.name == "invoke" }
                .invoke(null)
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
                class Logger
                @Reader
                inline fun changingCallee() {
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
                fun invoke() {
                    val result = context.runReader { caller() }
                }
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
            assertFalse(hasErrors)
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
                Module("modulea", workingDirA)
            ),
            providedChangedFiles = null
        ).apply {
            assertFalse(hasErrors)
            assertTrue("Caller.kt is marked dirty" in messageOutput)
            assertTrue("Context.kt is marked dirty" in messageOutput)
            assertTrue("RunReader.kt is marked dirty" in messageOutput)
            classLoader.loadClass("com.ivianuu.injekt.integrationtests.RunReaderKt")
                .declaredMethods
                .single { it.name == "invoke" }
                .invoke(null)
        }

        changingCalleeSource = source(
            """
                class Logger
                @Reader
                inline fun changingCallee() {
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
            assertFalse(hasErrors)
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
                Module("modulea", workingDirA)
            ),
            providedChangedFiles = null
        ).apply {
            assertFalse(hasErrors)
            assertTrue("Caller.kt is marked dirty" !in messageOutput)
            assertTrue("Context.kt is marked dirty" !in messageOutput)
            assertTrue("RunReader.kt is marked dirty" !in messageOutput)
            classLoader.loadClass("com.ivianuu.injekt.integrationtests.RunReaderKt")
                .declaredMethods
                .single { it.name == "invoke" }
                .invoke(null)
        }

        changingCalleeSource = source(
            """
                class Logger
                @Reader
                inline fun changingCallee() {
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
            assertFalse(hasErrors)
            assertTrue("ChangingCallee.kt is marked dirty" in messageOutput)
            assertTrue("NotChangingCallee.kt is marked dirty" !in messageOutput)
            assertTrue("Unrelated.kt is marked dirty" !in messageOutput)
        }
        val lol = ""
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
                Module("modulea", workingDirA)
            ),
            providedChangedFiles = listOf(
                callerSource,
                contextSource,
                runReaderSource
            )
        ).apply {
            assertFalse(hasErrors)
            assertTrue("Caller.kt is marked dirty" in messageOutput)
            assertTrue("Context.kt is marked dirty" in messageOutput)
            assertTrue("RunReader.kt is marked dirty" in messageOutput)
            classLoader.loadClass("com.ivianuu.injekt.integrationtests.RunReaderKt")
                .declaredMethods
                .single { it.name == "invoke" }
                .invoke(null)
        }
    }

    private class Result(
        val messageOutput: String,
        val classLoader: ClassLoader,
        val hasErrors: Boolean
    )

    private class Module(
        val name: String,
        val workingDir: File
    )

    private fun compileIncrementally(
        moduleName: String,
        workingDir: File,
        projectRoot: File,
        sources: List<SourceFile>,
        otherModules: List<Module>,
        providedChangedFiles: List<SourceFile>?
    ): Result {
        println("compile start $moduleName")
        println()
        val srcDir = workingDir.resolve("sources")
            .also { it.mkdirs() }
        val fileBySourceFile = sources.associateWith { it.writeIfNeeded(srcDir) }
        val messages = mutableListOf<String>()

        val kotlinCompileArgs = compilation {
            this.workingDir = workingDir
            this.moduleName = moduleName
            otherModules.forEach {
                this.classpaths += it.workingDir.resolve("classes")
            }
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
                ":${it.name}",
                it.name,
                it.workingDir.resolve("build").also { it.mkdirs() },
                it.workingDir.resolve("caches").resolve("build-history.bin")
            )
        }

        val moduleApiHistory = ModulesApiHistoryJvm(
            IncrementalModuleInfo(
                projectRoot,
                moduleEntries.mapKeys { it.key.workingDir.resolve("classes") },
                moduleEntries.mapKeys { it.key.name }.mapValues { setOf(it.value) },
                emptyMap(),
                moduleEntries.mapKeys { it.key.workingDir.resolve("lib.jar") }
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
        var hasErrors = false
        compiler.compile(
            sourceFiles,
            args,
            object : MessageCollector {
                override fun report(
                    severity: CompilerMessageSeverity,
                    message: String,
                    location: CompilerMessageSourceLocation?
                ) {
                    hasErrors = hasErrors || severity.isError
                    println("$severity $message $location")
                }

                override fun hasErrors(): Boolean = hasErrors

                override fun clear() {
                }
            },
            providedChangedFiles?.let {
                ChangedFiles.Known(
                    modified = it.map { fileBySourceFile[it]!! },
                    removed = emptyList()
                )
            }
        )

        val classLoader = URLClassLoader(
            arrayOf(
                *(kotlinCompileArgs.classpaths + kotlinCompileArgs.classesDir)
                    .map { it.toURI().toURL() }.toTypedArray()
            ),
            this::class.java.classLoader
        )

        createJar(
            workingDir.resolve("lib.jar")
                .absolutePath,
            workingDir.resolve("classes").absolutePath
        )

        println()
        println("compile end $moduleName")
        println()
        return Result(
            messages.joinToString("\n"),
            classLoader,
            hasErrors
        )
    }

}

private fun copy(inputStream: InputStream, outputStream: OutputStream) {
    val BUFFER_SIZE = 1024
    val buffer = ByteArray(BUFFER_SIZE)
    val bufferedInputStream = BufferedInputStream(inputStream)
    var len: Int
    while (true) {
        len = bufferedInputStream.read(buffer)
        if (len == -1) {
            break
        }
        outputStream.write(buffer, 0, len)
    }
    bufferedInputStream.close()
}

private fun copyFilesRecursively(dir: File, jarOutputStream: JarOutputStream, prefLen: Int) {
    for (file in dir.listFiles()!!) {
        if (file.isDirectory()) {
            copyFilesRecursively(file, jarOutputStream, prefLen)
        } else {
            val fileName = file.getCanonicalPath().substring(prefLen).replace('\\', '/')
            val jarEntry = JarEntry(fileName)
            jarEntry.setTime(file.lastModified())
            jarOutputStream.putNextEntry(jarEntry)
            val fileInputStream = FileInputStream(file)
            copy(fileInputStream, jarOutputStream)
            jarOutputStream.closeEntry()
        }
    }
}

private fun createJar(jarFile: String, srcDir: String) {
    val manifest = Manifest()
    manifest.getMainAttributes()!!.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    File(jarFile).getParentFile()!!.mkdirs()
    val jarOutputStream = JarOutputStream(FileOutputStream(jarFile), manifest)
    val srcDirFile = File(srcDir).getCanonicalFile()
    val srcDirCanonical = srcDirFile.getCanonicalPath()
    val prefLen = srcDirCanonical.length + if (srcDirCanonical.endsWith('/')) {
        0
    } else {
        1
    }
    copyFilesRecursively(srcDirFile, jarOutputStream, prefLen)
    jarOutputStream.close()
}
 */