package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.SrcDir
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import java.io.File

@Binding
class InjektKtGenerationExtension(
    private val srcDir: SrcDir,
    private val generationComponentFactory: (ModuleDescriptor, BindingContext) -> GenerationComponent
) : PartialAnalysisHandlerExtension() {

    override val analyzePartially: Boolean
        get() = !generatedCode

    private var generatedCode = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider,
    ): AnalysisResult? {
        if (!generatedCode) {
            files as MutableList<KtFile>
            srcDir.deleteRecursively()
            files.removeAll { !File(it.virtualFilePath).exists() }
            val generationComponent = generationComponentFactory(
                module,
                bindingTrace.bindingContext
            )
            val fileManager = generationComponent.fileManager

            generationComponent.functionAliasGeneratorFactory.invoke { fqName, fileName, code ->
                val file = fileManager.generateFile(fqName, fileName, code)
                files += KtFile(
                    SingleRootFileViewProvider(
                        PsiManager.getInstance(project),
                        CoreLocalVirtualFile(
                            CoreLocalFileSystem(),
                            file
                        )
                    ),
                    false
                )
            }.generate(files.toList())
        }

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
        files: Collection<KtFile>,
    ): AnalysisResult? {
        if (generatedCode) return null
        generatedCode = true

        files as List<KtFile>

        val generationComponent = generationComponentFactory(
            module, bindingTrace.bindingContext
        )
        generationComponent.bindingModuleGenerator.generate(files)
        generationComponent.indexGenerator.generate(files)
        generationComponent.componentGenerator.generate(files)
        val newFiles = generationComponent.fileManager.newFiles

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), newFiles, true
        )
    }
}
