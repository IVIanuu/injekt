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
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

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

    override fun generate(files: List<KtFile>) {
        funBindings.forEach { descriptor ->
            generateFunBinding(descriptor)
        }
    }

    private fun generateFunBinding(descriptor: FunctionDescriptor) {
        val packageFqName = descriptor.findPackage().fqName
        val fileName = joinedNameOf(
            packageFqName,
            descriptor.fqNameSafe
        ).asString() + "FunBinding.kt"
        val bindingFunctionName = "${descriptor.name.asString()}FunBinding".asNameId()
        val code = buildCodeString {
            emitLine("package $packageFqName")
            emitLine("import ${InjektFqNames.Binding}")
            emitLine()
            val isSuspend = descriptor.isSuspend
            val isComposable = descriptor.hasAnnotation(InjektFqNames.Composable)
            val returnType = typeTranslator.toTypeRef(descriptor.returnType!!, descriptor)

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
            if (isComposable) emit("@${InjektFqNames.Composable} ")
            if (isSuspend) emit("suspend ")
            if (descriptor.extensionReceiverParameter?.type
                    ?.hasAnnotation(InjektFqNames.Assisted) == true) {
                emit("${typeTranslator.toTypeRef(descriptor.extensionReceiverParameter!!.type, descriptor).render()}.")
            }
            emit("(")
            val assistedValueParameters = descriptor.valueParameters
                .filter { it.type.hasAnnotation(InjektFqNames.Assisted) }
            assistedValueParameters.forEachIndexed { index, param ->
                emit(typeTranslator.toTypeRef(param.type, descriptor).render())
                if (index != assistedValueParameters.lastIndex) emit(", ")
            }
            emitLine(") -> ${returnType.render()}")

            emitLine()

            emitLine("@Binding")
            emit("fun ")
            if (descriptor.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit("${typeParameter.name} : ${typeTranslator.toTypeRef(typeParameter.upperBounds.single(), descriptor).render()}")
                        if (index != descriptor.typeParameters.lastIndex) emit(", ")
                    }
                emit("> ")
            }
            descriptor.extensionReceiverParameter
                ?.takeUnless { it.type.hasAnnotation(InjektFqNames.Assisted) }
                ?.let { typeTranslator.toTypeRef(it.type, descriptor) }
                ?.let { emit("(${it.render()}).") }
            emitLine("$bindingFunctionName(")

            val nonAssistedValueParameters = descriptor.valueParameters
                .filterNot { it.type.hasAnnotation(InjektFqNames.Assisted) }
            nonAssistedValueParameters
                .forEachIndexed { index, valueParameter ->
                    emit("${valueParameter.name}: ${valueParameter.type
                        .let { typeTranslator.toTypeRef(it, descriptor) }.render()}")
                    if (index != nonAssistedValueParameters.lastIndex) emit(", ")
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
                assistedValueParameters
                    .forEachIndexed { index, parameter ->
                        emit("p$index: ${typeTranslator.toTypeRef(parameter.type, descriptor).renderExpanded()}")
                        if (index != assistedValueParameters.lastIndex) emit(", ")
                    }
                emitLine(" ->")
                var assistedIndex = 0
                val callable = declarationStore.callableForDescriptor(descriptor)
                emitCallableInvocation(
                    callable,
                    null,
                    callable.valueParameters.map { parameter ->
                        when {
                            parameter.isAssisted -> {
                                {
                                    if (parameter.isExtensionReceiver) {
                                        emit("this")
                                    } else {
                                        emit("p${assistedIndex++}")
                                    }
                                }
                            }
                            else -> {
                                {
                                    if (parameter.isExtensionReceiver) {
                                        emit("this@$bindingFunctionName")
                                    } else {
                                        emit(parameter.name)
                                    }
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
                .filterNot { it.type.hasAnnotation(InjektFqNames.Assisted) }
                .map {
                    ValueParameterRef(
                        type = it.type
                            .let { typeTranslator.toTypeRef(it, descriptor) }
                            .substitute(bindingCallableSubstitutionMap),
                        isExtensionReceiver = it.name.isSpecial,
                        isAssisted = it.type.hasAnnotation(InjektFqNames.Assisted),
                        name = it.name
                    )
                },
            targetComponent = null,
            contributionKind = Callable.ContributionKind.BINDING,
            isCall = true,
            callableKind = Callable.CallableKind.DEFAULT,
            isExternal = false
        )
        declarationStore.addGeneratedBinding(bindingCallable)
        declarationStore.addGeneratedInternalIndex(
            descriptor.findPsi()!!.containingFile as KtFile,
            Index(bindingCallable.fqName, "function")
        )
    }
}
