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
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension

@Binding
class InjektKtGenerationExtension(
    private val generationComponentFactory: (ModuleDescriptor, BindingContext) -> GenerationComponent
) : PartialAnalysisHandlerExtension() {

    override val analyzePartially: Boolean
        get() = !generatedCode

    private var generatedCode = false

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
        val generators = listOf(
            generationComponent.funBindingAliasGenerator,
            generationComponent.effectGenerator,
            generationComponent.indexGenerator,
            generationComponent.componentGenerator
        )
        generators.forEach {
            runExitCatching {
                it.preProcess(files)
            }
        }
        generators.forEach {
            runExitCatching {
                it.generate(files)
            }
        }
        generationComponent.errorCollector.report()
        val newFiles = generationComponent.fileManager.newFiles

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), newFiles, true
        )
    }
}
