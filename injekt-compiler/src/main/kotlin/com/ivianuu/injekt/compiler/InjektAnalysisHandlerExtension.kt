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

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.JavaClassesTrackerImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.callExpressionRecursiveVisitor
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class InjektAnalysisHandlerExtension(
    private val outputPath: String
) : AnalysisHandlerExtension {

    private var generatedFiles = false

    private var javaClassesTracker: JavaClassesTrackerImpl? = null

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        javaClassesTracker = try {
            componentProvider.get()
        } catch (e: Exception) {
            null
        }

        // fixes IC duplicate exception
        javaClassesTracker?.let {
            message("on hehe $it")
            it.javaClass.getDeclaredField("classDescriptors")
                .also { it.isAccessible = true }
                .get(it)
                .let { (it as MutableList<Any>).clear() }
        } ?: message("lolo")

        return null
    }

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (generatedFiles) return null
        generatedFiles = true

        files.forEach { file ->
            file.acceptChildren(
                namedFunctionRecursiveVisitor { namedFunction ->
                    val descriptor = bindingTrace[BindingContext.FUNCTION, namedFunction]
                    checkNotNull(descriptor)
                    if (descriptor.annotations.hasAnnotation(InjektClassNames.IntoComponent)) {
                        FileSpec.builder(
                                "com.ivianuu.injekt.aggregate",
                                descriptor.fqNameSafe.asString().replace(".", "_")
                            )
                            .addType(
                                TypeSpec.classBuilder(
                                        descriptor.fqNameSafe.asString().replace(".", "_")
                                    )
                                    .build()
                            )
                            .build()
                            .writeTo(File(outputPath))
                    }
                }
            )
            file.acceptChildren(
                classOrObjectRecursiveVisitor { clazz ->
                    val descriptor = clazz.findClassDescriptor(bindingTrace.bindingContext)
                    if (descriptor.hasAnnotatedAnnotations(InjektClassNames.TagMarker)) {
                        val functionName = (descriptor.fqNameSafe.pathSegments()
                            .dropLast(1) + "bind${clazz.name}")
                            .joinToString("_")

                        FileSpec.builder(
                                "com.ivianuu.injekt.aggregate",
                                functionName
                            )
                            .addType(
                                TypeSpec.classBuilder(functionName)
                                    .build()
                            )
                            .build()
                            .writeTo(File(outputPath))
                    }
                }
            )
            file.acceptChildren(
                callExpressionRecursiveVisitor { callExpression ->
                    val resolvedCall = callExpression.getResolvedCall(bindingTrace.bindingContext)
                    if (resolvedCall != null &&
                        resolvedCall.resultingDescriptor.annotations.hasAnnotation(InjektClassNames.IntoComponent)
                    ) {
                        bindingTrace.report(
                            InjektErrors.CannotInvokeIntoComponentFunctions.on(
                                callExpression
                            )
                        )
                    }
                }
            )
        }

        // fixes IC duplicate exception
        javaClassesTracker?.let {
            message("on hehe $it")
            it.javaClass.getDeclaredField("classDescriptors")
                .also { it.isAccessible = true }
                .get(it)
                .let { (it as MutableList<Any>).clear() }
        } ?: message("lolo")

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext,
            module,
            emptyList(),
            listOf(File(outputPath))
        )
    }
}
