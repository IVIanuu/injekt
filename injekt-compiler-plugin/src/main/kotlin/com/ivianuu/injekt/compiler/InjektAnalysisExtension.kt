package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.ast.psi.Psi2AstGenerator
import com.ivianuu.injekt.compiler.ast.string.toAstString
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class InjektAnalysisExtension : AnalysisHandlerExtension {

    private var generatedCode = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? = if (generatedCode) AnalysisResult.EMPTY else null

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (generatedCode) return null
        generatedCode = true

        files as ArrayList<KtFile>

        files
            .map { Psi2AstGenerator(bindingTrace).generateFile(it) }
            .forEach {
                println(it.toAstString())
            }

        return null
    }

}
