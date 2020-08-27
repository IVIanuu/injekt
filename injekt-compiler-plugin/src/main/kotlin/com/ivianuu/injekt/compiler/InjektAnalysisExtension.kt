package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.callExpressionRecursiveVisitor
import org.jetbrains.kotlin.psi.declarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.util.slicedMap.MutableSlicedMap
import java.io.File
import java.nio.file.Paths

class InjektAnalysisExtension : AnalysisHandlerExtension {

    private var generatedCode = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        println("hello ${files.map { it.name }}")

        if (files.size > 1) return null

        files as ArrayList<KtFile>

        componentProvider.get<LazyTopDownAnalyzer>().apply {
            analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
            analyzeDeclarations(TopDownAnalysisMode.LocalDeclarations, files)
        }

        files.forEach {
            it.accept(
                declarationRecursiveVisitor {
                    val descriptor =
                        bindingTrace.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it]
                            ?: error("lol ${it.text}")
                    println("decl ${it.name} -> $descriptor")
                }
            )
            it.accept(
                callExpressionRecursiveVisitor {
                    val resolvedCall = it.getResolvedCall(bindingTrace.bindingContext)
                        ?: error("no resolved call ${it.text}")
                    println("call ${it.text} -> $resolvedCall")
                }
            )
        }

        if (!generatedCode) {
            val firstFile = files.first()

            val newSource = buildString {
                appendLine("package com.ivianuu.injekt.integrationtests")
                appendLine()
                appendLine("import com.ivianuu.injekt.*")
                appendLine("import com.ivianuu.injekt.internal.*")
                appendLine("import com.ivianuu.injekt.test.*")
                appendLine("import kotlin.reflect.*")
                appendLine("import kotlinx.coroutines.*")
                appendLine()
                appendLine("@InitializeInjekt interface InjektInitializer")
                appendLine()

                appendLine(
                    """
                                    fun foo(context: Any) = Foo()
        
                                    class Dummy {
                                        class Inner
                                    }
        
                                    fun func(context: Any): Foo = error("lol")
        
                                    fun invoke(): Foo {
                                        return func(Any())
                                    }
                                """
                )
            }

            var virtualFile = firstFile.virtualFile
            val path = "build/generated/source/kapt/main"
            val directory = Paths.get("", *path.split("/").toTypedArray()).toFile()
            directory.mkdirs()
            virtualFile =
                CoreLocalVirtualFile(CoreLocalFileSystem(), File(directory, firstFile.name).apply {
                    writeText(newSource)
                })

            val replacementFile = KtFile(
                viewProvider = MetaFileViewProvider(firstFile.manager, virtualFile) {
                    it?.also {
                        it.setText(newSource)
                    }
                },
                isCompiled = false
            )
            files[files.indexOf(firstFile)] = replacementFile

            (bindingTrace as BindingTraceContext)
                .clearDiagnostics()
            BindingTraceContext::class.java
                .declaredFields
                .single { it.name == "map" }
                .also { it.isAccessible = true }
                .get(bindingTrace)
                .let { it as MutableSlicedMap }
                .clear()

        }

        return if (!generatedCode) AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext,
            module,
            emptyList(),
            emptyList()
        ).also { generatedCode = true } else null
    }

}

class MetaFileViewProvider(
    psiManager: PsiManager,
    virtualFile: VirtualFile,
    val transformation: (Document?) -> Document?
) : SingleRootFileViewProvider(psiManager, virtualFile) {
    override fun getDocument(): Document? = transformation(super.getDocument())
}
