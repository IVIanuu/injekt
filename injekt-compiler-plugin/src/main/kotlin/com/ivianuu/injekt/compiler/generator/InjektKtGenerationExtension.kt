package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Context
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.childContext
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

@Given
class InjektKtGenerationExtension : AnalysisHandlerExtension {

    private lateinit var analysisContext: AnalysisContext

    private var generatedCode = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (!generatedCode) {
            analysisContext = childContext(
                module,
                bindingTrace,
                bindingTrace.bindingContext,
                componentProvider.get<LookupTracker>()
                    .also { lookupTracker = it }
            )

            files as ArrayList<KtFile>
            val copy = files.toList()
            files.clear()
            files += analysisContext.runReader {
                given<KtFileManager>().onPreCompile(copy)
            }
        }
        return null
    }

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (bindingTrace.bindingContext.diagnostics.any {
                it.severity == Severity.ERROR
            }) return null
        if (generatedCode) {
            analysisContext.runReader { given<KtFileManager>().onPostCompile(files as ArrayList) }
            return null
        }
        generatedCode = true

        files as List<KtFile>

        val context = analysisContext.runReader { childContext<KtGenerationContext>() }

        context.runReader {
            given<GivenIndexingGenerator>().generate(files)
            given<EffectGenerator>().generate(files)
            given<ContextFactoryGenerator>().generate(files)
            given<ReaderContextGenerator>().generate(files)
            given<RunReaderCallIndexingGenerator>().generate(files)
            given<RootFactoryGenerator>().generate(files)
        }

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), analysisContext.runReader {
                given<KtFileManager>()
                    .newFiles.toList()
            }, true
        )
    }

}

@Context
interface AnalysisContext
