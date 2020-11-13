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
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
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
                                    .copy(superTypes = emptyList())
                            },
                            isTypeAlias = true
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

        val isSuspend = descriptor.isSuspend
        val isComposable = descriptor.hasAnnotation(InjektFqNames.Composable)

        val funApiValueParameters = descriptor.allParameters
            .filter { it.hasAnnotation(InjektFqNames.FunApi) }
        declarationStore.generatedClassifierFor(descriptor.fqNameSafe)!!.funApiParams += funApiValueParameters
            .map { it.name }

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
            .copy(isExtensionFunction = funApiValueParameters.any {
                it == descriptor.extensionReceiverParameter
            })
        declarationStore.generatedClassifierFor(descriptor.fqNameSafe)!!.superTypes += expandedFunType

        val code = buildCodeString {
            emitLine("@file:Suppress(\"UNCHECKED_CAST\", \"NOTHING_TO_INLINE\")")
            emitLine("package $packageFqName")

            val imports = mutableSetOf(
                "import ${InjektFqNames.Binding.asString()}",
                "import ${InjektFqNames.FunBinding.asString()}"
            )
            imports += (descriptor.findPsi()!!.containingFile as KtFile)
                .importDirectives
                .map { it.text }

            imports.forEach {
                emitLine(it)
            }

            emitLine()

            emit("@${InjektFqNames.FunApiParams}([")
            funApiValueParameters.forEachIndexed { index, parameter ->
                emit("\"${parameter.name}\"")
                if (index != funApiValueParameters.lastIndex) emit(", ")
            }
            emitLine("])")

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

            emitLine(expandedFunType.render())

            emitLine()

            if (isComposable) emitLine("@${InjektFqNames.Composable}")

            if (descriptor.visibility == Visibilities.INTERNAL) {
                emit("internal ")
            }

            if (isSuspend) emit("suspend ")

            emit("inline fun ")
            if (descriptor.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit("reified ${typeParameter.name}")
                        if (index != descriptor.typeParameters.lastIndex) emit(", ")
                    }
                emit("> ")
            }

            emit(descriptor.name)
            if (descriptor.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != descriptor.typeParameters.lastIndex) emit(", ")
                    }
                emit(">")
            }

            val invokeFunctionName = "invoke${descriptor.name.asString().capitalize()}".asNameId()

            emit(".$invokeFunctionName(")
            funApiValueParameters.forEachIndexed { index, valueParameter ->
                val typeRef = typeTranslator.toTypeRef(valueParameter.type, descriptor)
                if (valueParameter is ValueParameterDescriptor && valueParameter.isCrossinline) {
                    emit("crossinline ")
                }  else if (typeRef.fullyExpandedType.isFunction || typeRef.fullyExpandedType.isSuspendFunction ||
                    declarationStore.generatedClassifierFor(typeRef.classifier.fqName) != null ||
                    declarationStore.generatedClassifierFor(typeRef.fullyExpandedType.classifier.fqName) != null) {
                    emit("noinline ")
                }
                emit("${if (valueParameter == descriptor.extensionReceiverParameter) "_receiver"
                else valueParameter.name.asString()}: ${typeTranslator.toTypeRef(valueParameter.type, descriptor)}")
                if (valueParameter is ValueParameterDescriptor && valueParameter.declaresDefaultValue()) {
                    emit(" = ${(valueParameter.findPsi() as KtParameter).defaultValue!!.text}")
                }
                if (index != funApiValueParameters.lastIndex) emit(", ")

            }
            emit("): ${expandedFunType.typeArguments.last()} ")
            if (descriptor.typeParameters.isNotEmpty()) {
                emit(" where ")
                val typeParametersWithUpperBound = descriptor.typeParameters
                    .flatMap { typeParameter ->
                        typeParameter.upperBounds
                            .map {
                                typeTranslator.toClassifierRef(
                                    typeParameter
                                ).copy(fqName = packageFqName.child(invokeFunctionName).child(typeParameter.name)) to
                                        typeTranslator.toTypeRef(it, descriptor)
                            }
                    }
                typeParametersWithUpperBound.forEachIndexed { index, (typeParameter, upperBound) ->
                    emit("${typeParameter.fqName.shortName()} : $upperBound")
                    if (index != typeParametersWithUpperBound.lastIndex) emit(", ")
                }
                emitSpace()
            }
            braced {
                emit("return invoke(")
                funApiValueParameters.forEachIndexed { index, valueParameter ->
                    emit(
                        if (valueParameter == descriptor.extensionReceiverParameter) "_receiver"
                        else valueParameter.name.asString()
                    )
                    if (index != funApiValueParameters.lastIndex) emit(", ")

                }
                emitLine(")")
            }
        }

        fileManager.generateFile(packageFqName, fileName, code)
    }
}
