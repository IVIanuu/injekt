package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.CollectAdditionalSourcesExtension
import org.jetbrains.kotlin.psi.KtFile

@Binding class InjektCollectAdditionalSourcesExtension(
    funBindingProcessor: FunBindingProcessor,
    indexProcessor: IndexProcessor
) : CollectAdditionalSourcesExtension {

    private val processors = listOf(funBindingProcessor, indexProcessor)

    override fun collectAdditionalSourcesAndUpdateConfiguration(
        knownSources: Collection<KtFile>,
        configuration: CompilerConfiguration,
        project: Project
    ): Collection<KtFile> = processors.flatMap {
        try {
            it.process(knownSources.toList())
        } catch (e: ExitGenerationException) {
            emptyList()
        }
    }.also { configuration.addKotlinSourceRoots(it.map { it.virtualFilePath }) }

}
