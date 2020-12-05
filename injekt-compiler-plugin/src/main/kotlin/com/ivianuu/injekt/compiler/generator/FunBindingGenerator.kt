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
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.namedFunctionRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.utils.addToStdlib.cast

@Binding(GenerationComponent::class)
class FunBindingGenerator(
    private val fileManager: FileManager,
    private val module: ModuleDescriptor,
    private val packageFragmentProvider: InjektPackageFragmentProviderExtension
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                namedFunctionRecursiveVisitor { declaration ->
                    if (declaration.hasAnnotation(InjektFqNames.FunBinding)) {
                        generateFunBinding(declaration)
                    }
                }
            )
        }
    }

    private fun generateFunBinding(declaration: KtNamedFunction) {
        val file = declaration.containingKtFile
        val packageFqName = file.packageFqName
        val fileName = joinedNameOf(
            packageFqName,
            declaration.fqName!!
        ).asString() + "FunBinding.kt"

        val isSuspend = declaration.hasModifier(KtTokens.SUSPEND_KEYWORD)
        val isComposable = declaration.hasAnnotation(InjektFqNames.Composable)

        val funApiValueParameters = declaration.valueParameters
            .filter { it.hasAnnotation(InjektFqNames.FunApi) }

        /*val expandedFunType = (if (isSuspend) {
            module.builtIns.getSuspendFunction(funApiValueParameters.size)
                .defaultType
        } else {
            module.builtIns.getFunction(funApiValueParameters.size)
                .defaultType
        }).toTypeRef()
            .typeWith(funApiValueParameters.map {
                typeTranslator.toTypeRef(it.type, declaration)
            } + typeTranslator.toTypeRef(declaration.returnType!!, declaration))
            .copy(isComposable = isComposable)
            .copy(isExtensionFunction = funApiValueParameters.any {
                it == declaration.extensionReceiverParameter
            })
        declarationStore.generatedClassifierFor(declaration.fqNameSafe)!!.expandedType = expandedFunType*/

        val code = buildCodeString {
            emitLine("@file:Suppress(\"UNCHECKED_CAST\", \"NOTHING_TO_INLINE\")")
            emitLine("package $packageFqName")

            val imports = mutableSetOf(
                "import ${InjektFqNames.Binding.asString()}",
                "import ${InjektFqNames.FunBinding.asString()}"
            )
            imports += file
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

            if (declaration.visibilityModifier()?.text == "internal") {
                emit("internal ")
            }

            emit("typealias ${declaration.name}")

            if (declaration.typeParameters.isNotEmpty()) {
                emit("<")
                declaration.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != declaration.typeParameters.lastIndex) emit(", ")
                    }
                emit(">")
            }
            emit(" = ")

           // emitLine(expandedFunType.render())

            /*emitLine()

            if (isComposable) emitLine("@${InjektFqNames.Composable}")

            if (declaration.visibilityModifier()?.text == "internal") {
                emit("internal ")
            }

            if (isSuspend) emit("suspend ")

            emit("inline fun ")
            if (declaration.typeParameters.isNotEmpty()) {
                emit("<")
                declaration.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit("reified ${typeParameter.name}")
                        if (index != declaration.typeParameters.lastIndex) emit(", ")
                    }
                emit("> ")
            }

            emit(declaration.name)
            if (declaration.typeParameters.isNotEmpty()) {
                emit("<")
                declaration.typeParameters
                    .forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != declaration.typeParameters.lastIndex) emit(", ")
                    }
                emit(">")
            }

            val invokeFunctionName = "invoke${declaration.name!!.capitalize()}".asNameId()

            emit(".$invokeFunctionName(")
            funApiValueParameters.forEachIndexed { index, valueParameter ->
                val typeRef = valueParameter.type.toTypeRef()
                if (valueParameter is ValueParameterDescriptor && valueParameter.isCrossinline) {
                    emit("crossinline ")
                }  else if (typeRef.fullyExpandedType.isFunction || typeRef.fullyExpandedType.isSuspendFunction ||
                    declarationStore.generatedClassifierFor(typeRef.classifier.fqName) != null ||
                    declarationStore.generatedClassifierFor(typeRef.fullyExpandedType.classifier.fqName) != null) {
                    emit("noinline ")
                }
                emit("${if (valueParameter == descriptor.extensionReceiverParameter) "_receiver"
                else valueParameter.name.asString()}: ${typeTranslator.toTypeRef(valueParameter.type, descriptor).render()}")
                if (valueParameter is ValueParameterDescriptor && valueParameter.declaresDefaultValue()) {
                    emit(" = ${(valueParameter.findPsi() as KtParameter).defaultValue!!.text}")
                }
                if (index != funApiValueParameters.lastIndex) emit(", ")

            }
            emit("): ${expandedFunType.typeArguments.last().render()} ")
            if (declaration.typeParameters.isNotEmpty()) {
                emit(" where ")
                val typeParametersWithUpperBound = declaration.typeParameters
                    .flatMap { typeParameter ->
                        typeParameter.upperBounds
                            .map {
                                typeTranslator.toClassifierRef(
                                    typeParameter
                                ).copy(fqName = packageFqName.child(invokeFunctionName).child(typeParameter.name)) to
                                        typeTranslator.toTypeRef(it, declaration)
                            }
                    }
                typeParametersWithUpperBound.forEachIndexed { index, (typeParameter, upperBound) ->
                    emit("${typeParameter.fqName.shortName()} : ${upperBound.render()}")
                    if (index != typeParametersWithUpperBound.lastIndex) emit(", ")
                }
                emitSpace()
            }
            braced {
                emit("return invoke(")
                funApiValueParameters.forEachIndexed { index, valueParameter ->
                    emit(
                        if (valueParameter == declaration.extensionReceiverParameter) "_receiver"
                        else valueParameter.name.asString()
                    )
                    if (index != funApiValueParameters.lastIndex) emit(", ")

                }
                emitLine(")")
            }*/
        }

        fileManager.generateFile(
            originatingFile = file,
            packageFqName = packageFqName,
            fileName = fileName,
            code = code
        )
    }
}
