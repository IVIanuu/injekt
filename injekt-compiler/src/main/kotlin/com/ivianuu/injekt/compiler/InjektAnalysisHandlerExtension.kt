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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class InjektAnalysisHandlerExtension(
    private val outputDir: String
) : AnalysisHandlerExtension {
    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        val fileWriter = ServiceLoaderFileWriter(outputDir)

        message("analysis completed")

        files.forEach { file ->
            file.acceptChildren(
                namedFunctionRecursiveVisitor { namedFunction ->
                    val descriptor = bindingTrace[BindingContext.FUNCTION, namedFunction]
                    checkNotNull(descriptor)
                    if (descriptor.annotations.hasAnnotation(InjektClassNames.IntoComponent)) {
                        fileWriter.add(
                            (descriptor.fqNameSafe.pathSegments()
                                .dropLast(1) + descriptor.fqNameSafe.asString().replace(".", "_"))
                                .joinToString(".")
                        )
                    }
                }
            )
            file.acceptChildren(
                classOrObjectRecursiveVisitor { clazz ->
                    val descriptor = clazz.findClassDescriptor(bindingTrace.bindingContext)
                    if (descriptor.hasAnnotatedAnnotations(InjektClassNames.TagMarker)) {
                        val functionName = (descriptor.fqNameSafe.pathSegments()
                            .dropLast(1) + "bind${clazz.name}")
                            .joinToString(".")

                        fileWriter.add(
                            (descriptor.fqNameSafe.pathSegments()
                                .dropLast(1) + functionName.replace(".", "_")).joinToString(".")
                        )
                    }
                }
            )
        }

        fileWriter.writeFile()

        return super.analysisCompleted(project, module, bindingTrace, files)
    }
}