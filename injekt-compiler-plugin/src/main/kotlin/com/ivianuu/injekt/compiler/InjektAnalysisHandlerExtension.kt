package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class InjektAnalysisHandlerExtension(
    outputDir: String
) : AnalysisHandlerExtension {

    private var processingFinished = false

    private lateinit var container: ComponentProvider

    private val outputDir = File(outputDir).apply { mkdirs() }

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        container = componentProvider
        return super.doAnalysis(
            project,
            module,
            projectContext,
            files,
            bindingTrace,
            componentProvider
        )
    }

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (processingFinished) return null
        processingFinished = true

        var generatedFiles = false

        fun resolveFile(file: KtFile) {
            try {
                container.get<LazyTopDownAnalyzer>().apply {
                    analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, listOf(file))
                    analyzeDeclarations(TopDownAnalysisMode.LocalDeclarations, listOf(file))
                }
            } catch (t: Throwable) {
            }
        }

        files.forEach { file ->
            resolveFile(file)
            SyntheticAnnotationPropertyProcessor(outputDir)
                .processSyntheticAnnotationProperties(file, bindingTrace) { generatedFiles = true }
            KeyOverloadProcessor(module, outputDir)
                .processKeyOverloads(file, bindingTrace) { generatedFiles = true }
        }

        return if (generatedFiles) {
            message("Files generated re run analysis")
            AnalysisResult.RetryWithAdditionalRoots(
                bindingTrace.bindingContext,
                module,
                emptyList(),
                listOf(outputDir)
            )
        } else {
            message("Files not generated do not re run analysis")
            null
        }
    }

}
