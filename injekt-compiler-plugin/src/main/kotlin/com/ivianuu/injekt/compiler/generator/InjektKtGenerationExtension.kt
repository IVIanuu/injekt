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

package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.GenerateComponents
import com.ivianuu.injekt.compiler.GenerateMergeComponents
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension

@Binding
class InjektKtGenerationExtension(
    private val generationComponentFactory: (
        ModuleDescriptor,
        BindingContext
    ) -> GenerationComponent,
    private val generateComponents: GenerateComponents,
    private val generateMergeComponents: GenerateMergeComponents
) : PartialAnalysisHandlerExtension() {

    override val analyzePartially: Boolean
        get() = !generatedCode

    private var generatedCode = false

    private var generationComponent: GenerationComponent? = null

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        files as ArrayList<KtFile>
        if (!generatedCode) {
            generationComponent = generationComponentFactory(module, bindingTrace.bindingContext)
            val tmpFiles = files.toList()
            files.clear()
            files += generationComponent!!.fileManager.preGenerate(tmpFiles)
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

        val generationComponent = generationComponent!!

        val generators = listOfNotNull(
            generationComponent.funBindingGenerator,
            generationComponent.indexGenerator,
            if (generateComponents || generateMergeComponents) generationComponent.componentGenerator else null
        )
        generators.forEach { it.generate(files) }
        generationComponent.fileManager.postGenerate()
        val newFiles = generationComponent.fileManager.newFiles

        this.generationComponent = null

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), newFiles, true
        )
    }
}
