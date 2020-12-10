/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension

class InjektKtGenerationExtension(
    private val generationComponentFactory: (
        ModuleDescriptor,
        BindingContext,
        BindingTrace,
        LazyTopDownAnalyzer,
    ) -> GenerationComponent,
) : PartialAnalysisHandlerExtension() {

    override val analyzePartially: Boolean
        get() = !generatedCode

    private var generatedCode = false

    private var generationComponent: GenerationComponent? = null
    private var hasErrors = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider,
    ): AnalysisResult? {
        files as ArrayList<KtFile>
        if (!generatedCode) {
            val lazyTopDownAnalyzer = componentProvider.get<LazyTopDownAnalyzer>()
            val generationComponent = generationComponentFactory(module,
                bindingTrace.bindingContext, bindingTrace, lazyTopDownAnalyzer)
                .also { this.generationComponent = it }
            generationComponent.declarationStore.module = module
            val tmpFiles = files.toList()
            files.clear()
            files += generationComponent.fileManager.preGenerate(tmpFiles)

            val result = super.doAnalysis(project,
                module,
                projectContext,
                files,
                bindingTrace,
                componentProvider)

            generationComponent.indexGenerator.generate(files)

            files.forEach { it.accept(generationComponent.givenCallChecker) }
            generationComponent.givenInfoGenerator.generate(files)

            hasErrors = bindingTrace.bindingContext.diagnostics.any {
                it.severity == Severity.ERROR
            }
            return result
        }
        return null
    }

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>,
    ): AnalysisResult? {
        if (generatedCode || hasErrors) return null
        generatedCode = true

        files as List<KtFile>

        val generationComponent = generationComponent!!
        generationComponent.fileManager.postGenerate()
        val newFiles = generationComponent.fileManager.newFiles
        this.generationComponent = null

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), newFiles, true
        )
    }
}
