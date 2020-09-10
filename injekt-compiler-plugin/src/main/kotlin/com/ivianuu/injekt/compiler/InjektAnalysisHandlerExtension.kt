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

import com.ivianuu.injekt.compiler.analysis.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.analysis.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.analysis.hasAnnotation
import com.ivianuu.injekt.compiler.transform.asNameId
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.types.KotlinType
import java.io.File

class InjektAnalysisHandlerExtension(
    private val outputDir: File
) : AnalysisHandlerExtension {

    private var generatedCode = false

    override fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<KtFile>
    ): AnalysisResult? {
        if (generatedCode) return null
        generatedCode = true

        files.forEach { file ->
            file.accept(
                namedDeclarationRecursiveVisitor { namedDeclaration ->
                    val descriptor =
                        bindingTrace.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, namedDeclaration]
                            ?: return@namedDeclarationRecursiveVisitor

                    if (descriptor.hasAnnotation(InjektFqNames.Module)) {
                        indexFqName(descriptor.fqNameSafe)
                        return@namedDeclarationRecursiveVisitor
                    }

                    if (!descriptor.hasAnnotation(InjektFqNames.Given) &&
                        !descriptor.hasAnnotatedAnnotations(
                            InjektFqNames.Adapter,
                            descriptor.module
                        )
                    )
                        return@namedDeclarationRecursiveVisitor
                    val targetDescriptor = if (descriptor is ConstructorDescriptor)
                        descriptor.constructedClass else descriptor
                    val moduleName = targetDescriptor.fqNameSafe
                        .pathSegments().joinToString("") + "Module"

                    val targetContext = if (descriptor.hasAnnotation(InjektFqNames.Given)) {
                        descriptor.annotations.findAnnotation(InjektFqNames.Given)!!
                            .getTargetContextOrAny(descriptor.module)
                    } else {
                        descriptor.getAnnotatedAnnotations(InjektFqNames.Adapter, descriptor.module)
                            .single()
                            .getTargetContextOrAny(descriptor.module)
                    }

                    val packageName = targetDescriptor.findPackage().fqName.asString()

                    val moduleCode = buildCodeString {
                        emitLine("package $packageName")
                        emitLine()
                        emitLine("import com.ivianuu.injekt.Module")
                        emitLine("import com.ivianuu.injekt.ContextBuilder")
                        emitLine("import com.ivianuu.injekt.scoped")
                        emitLine("import com.ivianuu.injekt.keyOf")
                        emitLine()
                        emitLine("@Module($targetContext::class)")
                        emit("fun ContextBuilder.$moduleName() ")

                        val functionDescriptor = if (targetDescriptor is ClassDescriptor)
                            targetDescriptor.constructors
                                .singleOrNull { it.hasAnnotation(InjektFqNames.Given) }
                                ?: targetDescriptor.unsubstitutedPrimaryConstructor!!
                        else targetDescriptor as FunctionDescriptor
                        val functionName = if (functionDescriptor is ConstructorDescriptor)
                            functionDescriptor.constructedClass.fqNameSafe.asString()
                        else functionDescriptor.name.asString()
                        val dispatchReceiver =
                            functionDescriptor.dispatchReceiverParameter?.returnType

                        fun emitProvider() {
                            braced {
                                if (targetDescriptor.hasAnnotation(InjektFqNames.Given) ||
                                    descriptor !is FunctionDescriptor
                                ) {
                                    val assistedParameters = functionDescriptor.valueParameters
                                    if (assistedParameters.isEmpty()) {
                                        emit(
                                            "${
                                                dispatchReceiver?.render()?.let { "$it." }.orEmpty()
                                            }$functionName()"
                                        )
                                    } else {
                                        emit("{ ")
                                        assistedParameters.forEachIndexed { index, param ->
                                            emit("p$index: ${param.returnType!!.render()}")
                                            if (index != assistedParameters.lastIndex) {
                                                emit(", ")
                                            } else {
                                                emitLine(" ->")
                                            }
                                        }
                                        indented {
                                            emit(
                                                "${
                                                    dispatchReceiver?.render()?.let { "$it." }
                                                        .orEmpty()
                                                }$functionName("
                                            )
                                            assistedParameters.forEachIndexed { index, param ->
                                                emit("p$index")
                                                if (index != assistedParameters.lastIndex) emit(", ")
                                            }
                                            emitLine(")")
                                        }
                                        emit("}")
                                    }
                                } else {
                                    val assistedParameters = functionDescriptor.valueParameters
                                    emit("{ ")
                                    assistedParameters.forEachIndexed { index, param ->
                                        emit("p$index: ${param.returnType!!.render()}")
                                        if (index != assistedParameters.lastIndex) {
                                            emit(", ")
                                        } else {
                                            emitLine(" ->")
                                        }
                                    }
                                    indented {
                                        emit(
                                            "${
                                                dispatchReceiver?.render()?.let { "$it." }.orEmpty()
                                            }$functionName("
                                        )
                                        assistedParameters.forEachIndexed { index, param ->
                                            emit("p$index")
                                            if (index != assistedParameters.lastIndex) emit(", ")
                                        }
                                        emitLine(")")
                                    }
                                    emit("}")
                                }
                            }
                        }
                        braced {
                            if (descriptor.hasAnnotation(InjektFqNames.Given)) {
                                if (targetContext != InjektFqNames.AnyContext)
                                    emit("scoped ")
                                else emit("unscoped ")
                                emitProvider()
                            } else {
                                val adapterImpl = descriptor.getAnnotatedAnnotations(
                                    InjektFqNames.Adapter, descriptor.module
                                )
                                    .single()
                                    .annotationClass!!
                                    .companionObjectDescriptor!!
                                emit("with(${adapterImpl.fqNameSafe}) ")
                                braced {
                                    emit("configure(keyOf()) ")
                                    emitProvider()
                                }
                            }
                        }
                    }

                    outputDir.resolve(packageName.replace(".", "/"))
                        .also { it.mkdirs() }
                        .resolve("$moduleName.kt")
                        .also { it.createNewFile() }
                        .writeText(moduleCode)

                    indexFqName(FqName(packageName).child(moduleName.asNameId()))
                }
            )
        }

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingTrace.bindingContext, module, emptyList(), listOf(outputDir), true
        )
    }

    private fun indexFqName(
        fqName: FqName
    ) {
        val moduleIndexName = fqName.pathSegments().joinToString("_")

        val indexCode = buildCodeString {
            emitLine("package ${InjektFqNames.IndexPackage}")
            emitLine()
            emitLine("internal val $moduleIndexName = Unit")
        }

        outputDir.resolve(InjektFqNames.IndexPackage.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve("$moduleIndexName.kt")
            .also { it.createNewFile() }
            .writeText(indexCode)
    }

    private fun KotlinType.render() = buildString {
        fun KotlinType.renderInner() {
            append(constructor.declarationDescriptor!!.fqNameSafe)
            if (arguments.isNotEmpty()) {
                append("<")
                arguments.forEachIndexed { index, argument ->
                    if (argument.isStarProjection) append("*")
                    else argument.type.renderInner()
                    if (index != arguments.lastIndex) append(", ")
                }
                append(">")
            }
        }
        renderInner()
    }

    private fun AnnotationDescriptor.getTargetContextOrAny(module: ModuleDescriptor): FqName {
        return allValueArguments.values.singleOrNull()
            ?.let { it as KClassValue }
            ?.getArgumentType(module)
            ?.constructor
            ?.declarationDescriptor
            ?.fqNameSafe
            ?: InjektFqNames.AnyContext
    }

}
