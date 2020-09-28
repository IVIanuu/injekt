package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.childContext
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

@Given
class InjektKtGenerationExtension : AnalysisHandlerExtension {

    private var generatedCode = false

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (generatedCode || bindingTrace.bindingContext.diagnostics.any {
                it.severity == Severity.ERROR
            }
        ) return null
        generatedCode = true

        given<SrcDir>()
            .deleteRecursively()

        files as List<KtFile>

        childContext<GenerationContext>(
            module,
            bindingTrace,
            bindingTrace.bindingContext
        ).runReader {
            given<GivenIndexingGenerator>().generate(files)
            given<EffectGenerator>().generate(files)
            given<ContextFactoryGenerator>().generate(files)
            given<ReaderContextGenerator>().generate(files)
            given<RunReaderCallIndexingGenerator>().generate(files)
            given<RootContextFactoryImplGenerator>().generate(files)
        }

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), listOf(given<SrcDir>()), true
        )
    }

}
