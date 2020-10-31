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
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.componentimpl.emitCallableInvocation
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

@Binding(GenerationComponent::class)
class FunBindingGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
    private val typeTranslator: TypeTranslator
) : Generator {

    private val funBindings = mutableListOf<FunctionDescriptor>()

    override fun preProcess(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                namedFunctionRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<FunctionDescriptor>(bindingContext)
                    if (descriptor?.hasAnnotation(InjektFqNames.FunBinding) == true) {
                        funBindings += descriptor
                    }
                }
            )
            funBindings.forEach { descriptor ->
                runExitCatching {
                    declarationStore.addGeneratedClassifier(
                        ClassifierRef(
                            fqName = descriptor.fqNameSafe,
                            typeParameters = descriptor.typeParameters.map {
                                typeTranslator.toClassifierRef(it)
                            }
                        )
                    )
                }
            }
        }
    }

    override fun generate(files: List<KtFile>) {
        funBindings.forEach { descriptor ->
            runExitCatching {
                generateFunBinding(descriptor)
            }
        }
    }

    private fun generateFunBinding(descriptor: FunctionDescriptor) {
        val packageFqName = descriptor.findPackage().fqName
        val fileName = joinedNameOf(
            packageFqName,
            descriptor.fqNameSafe
        ).asString() + "FunBinding.kt"
        val bindingFunctionName = "${descriptor.name.asString()}FunBinding".asNameId()

        val isSuspend = descriptor.isSuspend
        val isComposable = descriptor.hasAnnotation(InjektFqNames.Composable)

        fun TypeRef.toProviderType(): TypeRef {
            return (if (isSuspend) descriptor.builtIns.getSuspendFunction(0)
                    else descriptor.builtIns.getFunction(0))
                .let {
                    typeTranslator.toTypeRef(it.defaultType, descriptor)
                        .copy(isComposable = isComposable)
                }
                .typeWith(listOf(this))
                .copy(isInlineProvider = true)
        }

        val funApiValueParameters = descriptor.allParameters
            .filter { it.hasAnnotation(InjektFqNames.FunApi) }

        val expandedFunType = (if (isSuspend) {
            descriptor.module.builtIns.getSuspendFunction(funApiValueParameters.size)
                .defaultType
        } else {
            descriptor.module.builtIns.getFunction(funApiValueParameters.size)
                .defaultType
        }).let { typeTranslator.toTypeRef(it, descriptor) }
            .typeWith(funApiValueParameters.map {
                typeTranslator.toTypeRef(it.type, descriptor)
            } + typeTranslator.toTypeRef(descriptor.returnType!!, descriptor))
            .copy(isComposable = isComposable)

        val code = buildCodeString {
            emitLine("package $packageFqName")
            emitLine("import ${InjektFqNames.Binding}")
            emitLine("import ${InjektFqNames.FunBinding}")
            emitLine()
            if (descriptor.visibility == Visibilities.INTERNAL) {
                emit("internal ")
            }

            emit("typealias ${descriptor.name}")

            if (descriptor.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != descriptor.typeParameters.lastIndex) emit(", ")
                    }
                emit(">")
            }
            emit(" = ")

            emit(expandedFunType.render())

            emitLine()

            emitLine("@${InjektFqNames.Eager}")
            emitLine("@Binding")
            emitLine("@FunBinding")
            if (descriptor.visibility != Visibilities.INTERNAL) {
                emit("inline ")
            }
            emit("fun ")
            if (descriptor.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit("reified ${typeParameter.name} : ${typeTranslator.toTypeRef(typeParameter.upperBounds.single(), descriptor).render()}")
                        if (index != descriptor.typeParameters.lastIndex) emit(", ")
                    }
                emit("> ")
            }

            emitLine("$bindingFunctionName(")

            val dependencyValueParameters = descriptor.allParameters
                .filterNot { it.hasAnnotation(InjektFqNames.FunApi) }
            dependencyValueParameters
                .forEachIndexed { index, valueParameter ->
                    val typeRef = valueParameter.type
                        .let { typeTranslator.toTypeRef(it, descriptor) }
                        .toProviderType()
                    if (descriptor.visibility != Visibilities.INTERNAL) {
                        emit("crossinline ")
                    }
                    emit("${if (valueParameter != descriptor.extensionReceiverParameter) valueParameter.name else "_receiver"}: " +
                            "${typeRef.render()}")
                    if (index != dependencyValueParameters.lastIndex) emit(", ")
                }
            emit("): ${descriptor.name}")
            if (descriptor.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != descriptor.typeParameters.lastIndex) emit(", ")
                    }
                emit(">")
            }
            emitSpace()
            braced {
                emit("return { ")
                funApiValueParameters
                    .filter { it != descriptor.extensionReceiverParameter }
                    .forEachIndexed { index, parameter ->
                        emit("p$index: ${typeTranslator.toTypeRef(parameter.type, descriptor).renderExpanded()}")
                        if (index != funApiValueParameters.lastIndex) emit(", ")
                    }
                emitLine(" ->")
                val callable = declarationStore.callableForDescriptor(descriptor)
                var funApiParamIndex = 0
                emitCallableInvocation(
                    callable,
                    null,
                    callable.valueParameters.map { parameter ->
                        when {
                            (descriptor.allParameters.single {
                                (it == descriptor.extensionReceiverParameter &&
                                        parameter.name == "_receiver".asNameId()) ||
                                        it.name == parameter.name
                            }.hasAnnotation(InjektFqNames.FunApi)) -> {
                                {
                                    if (parameter.isExtensionReceiver) {
                                        emit("this")
                                    } else {
                                        emit("p${funApiParamIndex++}")
                                    }
                                }
                            }
                            else -> {
                                {
                                    emit("${parameter.name}()")
                                }
                            }
                        }
                    }
                )
                emitLine()
                emitLine("}")
            }
        }

        fileManager.generateFile(packageFqName, fileName, code)

        val callableTypeParameters = descriptor.typeParameters
            .map {
                ClassifierRef(
                    packageFqName.child(bindingFunctionName)
                        .child(it.name),
                    superTypes = /*it.upperBounds
                        .map {
                            typeTranslator.toTypeRef(it, descriptor)
                        }*/ emptyList(),
                    isTypeParameter = true
                )
            }
        val bindingCallableSubstitutionMap = descriptor.typeParameters
            .map { typeTranslator.toClassifierRef(it) }
            .zip(callableTypeParameters.map { it.defaultType })
            .toMap()
        val bindingCallable = Callable(
            packageFqName = packageFqName,
            fqName = packageFqName.child(bindingFunctionName),
            name = bindingFunctionName,
            type = declarationStore.generatedClassifierFor(descriptor.fqNameSafe)!!
                .defaultType
                .typeWith(callableTypeParameters.map { it.defaultType }),
            typeParameters = callableTypeParameters,
            valueParameters = (listOfNotNull(descriptor.extensionReceiverParameter) + descriptor.valueParameters)
                .filterNot { it.hasAnnotation(InjektFqNames.FunApi) }
                .map {
                    ValueParameterRef(
                        type = it.type
                            .let { typeTranslator.toTypeRef(it, descriptor) }
                            .toProviderType()
                            .substitute(bindingCallableSubstitutionMap),
                        isExtensionReceiver = false,
                        inlineKind = ValueParameterRef.InlineKind.CROSSINLINE,
                        name = if (it == descriptor.extensionReceiverParameter)
                            "_receiver".asNameId() else it.name,
                        bindingAdapterArgName = it.getBindingAdapterArgName()
                    )
                },
            targetComponent = null,
            contributionKind = Callable.ContributionKind.BINDING,
            isCall = true,
            callableKind = Callable.CallableKind.DEFAULT,
            bindingAdapters = descriptor
                .annotations
                .filter { it.hasAnnotation(InjektFqNames.BindingAdapter) }
                .map { declarationStore.bindingAdapterDescriptorForAnnotation(it, descriptor) },
            isEager = true,
            isExternal = false,
            isInline = true,
            isFunBinding = true
        )
        declarationStore.addGeneratedCallable(bindingCallable, descriptor.findPsi()!!.containingFile as KtFile)
        declarationStore.addGeneratedInternalIndex(
            descriptor.findPsi()!!.containingFile as KtFile,
            Index(bindingCallable.fqName, "function")
        )
    }
}
