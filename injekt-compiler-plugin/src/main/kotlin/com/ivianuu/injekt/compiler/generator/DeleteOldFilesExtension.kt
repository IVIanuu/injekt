package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.SrcDir
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

@Binding
class DeleteOldFilesExtension(
    private val srcDir: SrcDir
) : AnalysisHandlerExtension {
    private var deletedFiles = false
    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (!deletedFiles) {
            deletedFiles = true
            files as MutableList<KtFile>
            srcDir.deleteRecursively()
            files.removeAll { !File(it.virtualFilePath).exists() }
        }

        return super.doAnalysis(project,
            module,
            projectContext,
            files,
            bindingTrace,
            componentProvider)
    }
}
