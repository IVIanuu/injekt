package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

@Component
abstract class ApplicationComponent(
    @Binding protected val project: Project,
    @Binding protected val configuration: CompilerConfiguration
) {
    abstract val registerExtensions: registerExtensions

    @Binding(ApplicationComponent::class)
    protected fun srcDir(configuration: CompilerConfiguration): SrcDir =
        File(configuration.getNotNull(SrcDirKey))
            .also { it.mkdirs() }

    @Binding
    protected fun logger(): Logger? = if (loggingEnabled) LoggerImpl else null
}
