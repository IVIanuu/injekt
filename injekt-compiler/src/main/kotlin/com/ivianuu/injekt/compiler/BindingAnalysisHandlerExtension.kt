/*
 * Copyright 2019 Manuel Wrage
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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.classRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import java.io.File

class BindingAnalysisHandlerExtension(
    private val outputDir: File
) : PartialAnalysisHandlerExtension() {

    override val analyzePartially: Boolean
        get() = !generatedFiles

    private var generatedFiles = false
    private lateinit var resolveSession: ResolveSession

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        resolveSession = componentProvider.get()
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
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (generatedFiles) return null
        generatedFiles = true

        msg { "on analysis complete" }

        outputDir.deleteRecursively()
        outputDir.mkdirs()

        files.forEach { file ->
            file.accept(
                classRecursiveVisitor { ktClass ->
                    val classDescriptor =
                        resolveSession.resolveToDescriptor(ktClass) as ClassDescriptor
                    msg { "process class $ktClass desc is $classDescriptor" }

                    val descriptor = createBindingDescriptor(
                        ktClass,
                        classDescriptor,
                        bindingTrace
                    ) ?: return@classRecursiveVisitor

                    val generator = BindingGenerator(descriptor)
                    generator.generate().writeTo(outputDir)
                }
            )
        }

        return if (bindingTrace.bindingContext.diagnostics.isEmpty()) {
            msg { "analysis try with addional roots" }
            AnalysisResult.RetryWithAdditionalRoots(
                bindingContext = bindingTrace.bindingContext,
                moduleDescriptor = module,
                additionalJavaRoots = emptyList(),
                additionalKotlinRoots = listOf(outputDir)
            )
        } else {
            msg { "analysis error" }
            AnalysisResult.compilationError(bindingTrace.bindingContext)
        }
    }


}