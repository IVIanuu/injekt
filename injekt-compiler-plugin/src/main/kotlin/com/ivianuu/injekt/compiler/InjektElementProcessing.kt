package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.FileSpec
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

class InjektElementProcessing(
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

        val processors = listOf(
            KeyOverloadProcessor(),
            SyntheticAnnotationPropertyProcessor()
        )

        var generatedFiles = false

        val generateFile: (FileSpec) -> Unit = generatedFile@{ fileSpec ->
            val outputFile =
                File(outputDir, fileSpec.packageName.replace(".", "/") + fileSpec.name + ".kt")
            if (outputFile.exists()) {
                val oldContent = outputFile.readText()
                val newContent = fileSpec.toString()
                if (oldContent == newContent) {
                    message("Do not generate file ${fileSpec.packageName}.${fileSpec.name} nothing has changed")
                    return@generatedFile
                }
            }

            fileSpec.writeTo(outputFile)
            generatedFiles = true
        }

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
            processors.forEach {
                it.processFile(file, bindingTrace, generateFile)
            }
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
            message("No files generated stop analysis")
            null
        }
    }

}

interface ElementProcessor {

    fun processFile(
        file: KtFile,
        bindingTrace: BindingTrace,
        generateFile: (FileSpec) -> Unit
    )

}