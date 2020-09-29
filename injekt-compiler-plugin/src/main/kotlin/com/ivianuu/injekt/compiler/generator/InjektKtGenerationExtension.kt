package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.childContext
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import java.io.File

@Given
class InjektKtGenerationExtension : PartialAnalysisHandlerExtension() {

    override val analyzePartially: Boolean
        get() = !generatedCode

    private var generatedCode = false
    private var generatedFunctionAlias = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider,
    ): AnalysisResult? {
        if (!generatedFunctionAlias) {
            generatedFunctionAlias = true
            files as MutableList<KtFile>
            given<SrcDir>().deleteRecursively()
            files.removeAll { !File(it.virtualFilePath).exists() }
            childContext<GenerationContext>(
                module,
                bindingTrace,
                bindingTrace.bindingContext
            ).runReader {
                given<((FqName, String, String) -> Unit) -> FunctionAliasGenerator>().invoke { fqName, fileName, code ->
                    val file = given<FileManager>().generateFile(fqName, fileName, code)
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
        }

        return super.doAnalysis(project,
            module,
            projectContext,
            files,
            bindingTrace,
            componentProvider)
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

        val newFiles = childContext<GenerationContext>(
            module,
            bindingTrace,
            bindingTrace.bindingContext
        ).runReader {
            given<RootFactoryGenerator>().generate(files)
            given<FileManager>().newFiles
        }

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), newFiles, true
        )
    }

}
