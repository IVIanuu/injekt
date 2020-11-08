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
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny

@Binding
class ImplBindingGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
    private val typeTranslator: TypeTranslator
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                classOrObjectRecursiveVisitor { declaration ->
                    val descriptor = declaration.descriptor<ClassDescriptor>(bindingContext)
                        ?: return@classOrObjectRecursiveVisitor
                    if (descriptor.hasAnnotation(InjektFqNames.ImplBinding)) {
                        runExitCatching {
                            generateImplBinding(descriptor)
                        }
                    }
                }
            )
        }
    }

    private fun generateImplBinding(descriptor: ClassDescriptor) {
        val singleSuperType = descriptor.defaultType.constructor
            .supertypes.first { !it.isAnyOrNullableAny() }
            .let { typeTranslator.toTypeRef(it, descriptor) }
        val packageFqName = descriptor.findPackage().fqName
        val fileName = joinedNameOf(
            packageFqName,
            descriptor.fqNameSafe
        ).asString() + "ImplBinding.kt"
        val implFunctionName = "${descriptor.name.asString()}ImplBinding".asNameId()
        val superTypeFunctionName = singleSuperType.classifier.fqName.shortName()
            .asString().decapitalize().asNameId()
        val targetComponent = descriptor.annotations
            .findAnnotation(InjektFqNames.ImplBinding)
            ?.allValueArguments
            ?.get("scopeComponent".asNameId())
            ?.let { it as KClassValue }
            ?.getArgumentType(descriptor.module)
            ?.let { typeTranslator.toTypeRef(it, descriptor) }
        val injectConstructor = descriptor.getInjectConstructor()!!
        val typeParametersWithUpperBound = descriptor.declaredTypeParameters
            .flatMap { typeParameter ->
                typeParameter.upperBounds
                    .map {
                        typeTranslator.toClassifierRef(
                            typeParameter
                        ).copy(fqName = packageFqName.child(implFunctionName).child(typeParameter.name)) to
                                typeTranslator.toTypeRef(it, descriptor)
                    }
            }
        fileManager.generateFile(
            packageFqName = packageFqName,
            fileName = fileName,
            code = buildCodeString {
                emitLine("package $packageFqName")
                emitLine("import ${InjektFqNames.Binding}")

                emit("@Binding")
                if (targetComponent != null) emitLine("(${targetComponent.classifier.fqName}::class)")
                else emitLine()
                emit("fun ")
                if (descriptor.declaredTypeParameters.isNotEmpty()) {
                    emit("<")
                    descriptor.declaredTypeParameters.forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != descriptor.declaredTypeParameters.lastIndex) emit(", ")
                    }
                    emit("> ")
                }
                emitLine("$implFunctionName(")
                injectConstructor.valueParameters
                    .forEachIndexed { index, valueParameter ->
                        emit("${valueParameter.name}: ${valueParameter.type
                            .let { typeTranslator.toTypeRef(it, descriptor) }.render()}")
                        if (index != injectConstructor.valueParameters.lastIndex) emit(", ")
                    }
                emit("): ${descriptor.defaultType.let { typeTranslator.toTypeRef(it, descriptor) }.render()} ")
                if (typeParametersWithUpperBound.isNotEmpty()) {
                    emit("where ")
                    typeParametersWithUpperBound.forEachIndexed { index, (typeParameter, upperBound) ->
                        emit("${typeParameter.fqName.shortName()} : $upperBound")
                        if (index != typeParametersWithUpperBound.lastIndex) emit(", ")
                    }
                    emitSpace()
                }
                braced {
                    emit("return ${descriptor.name}")
                    if (descriptor.kind != ClassKind.OBJECT) {
                        emit("(")
                        injectConstructor.valueParameters
                            .forEachIndexed { index, valueParameter ->
                                emit("${valueParameter.name}")
                                if (index != injectConstructor.valueParameters.lastIndex) emit(", ")
                            }
                        emitLine(")")
                    } else {
                        emitLine()
                    }
                }
                emitLine()
                emitLine("@Binding")
                emit("val ")
                if (descriptor.declaredTypeParameters.isNotEmpty()) {
                    emit("<")
                    descriptor.declaredTypeParameters.forEachIndexed { index, typeParameter ->
                        emit(typeParameter.name)
                        if (index != descriptor.declaredTypeParameters.lastIndex) emit(", ")
                    }
                    emit("> ")
                }
                emit("${descriptor.defaultType
                    .let { typeTranslator.toTypeRef(it, descriptor) }}.$superTypeFunctionName: ${singleSuperType.render()} ")
                if (typeParametersWithUpperBound.isNotEmpty()) {
                    emit("where ")
                    typeParametersWithUpperBound.forEachIndexed { index, (typeParameter, upperBound) ->
                        emit("${typeParameter.fqName.shortName()} : $upperBound")
                        if (index != typeParametersWithUpperBound.lastIndex) emit(", ")
                    }
                    emitSpace()
                }
                emitLine()
                indented { emitLine("get() = this") }
            }
        )

        val implCallableTypeParameters = descriptor.declaredTypeParameters
            .map {
                val raw = typeTranslator.toClassifierRef(it)
                raw.copy(
                    fqName = packageFqName.child(implFunctionName).child(it.name),
                    superTypes = raw.superTypes.map {
                        if (it.classifier.isTypeParameter &&
                            it.classifier.fqName.parent() == packageFqName.child(descriptor.name))
                            it.copy(classifier = it.classifier.copy(
                                fqName = packageFqName.child(implFunctionName).child(it.classifier.fqName.shortName())
                            )) else it
                    }
                )
            }
        val implCallableSubstitutionMap = descriptor.declaredTypeParameters
            .map { typeTranslator.toClassifierRef(it) }
            .zip(implCallableTypeParameters.map { it.defaultType })
            .toMap()
        val implCallable = Callable(
            packageFqName = packageFqName,
            fqName = packageFqName.child(implFunctionName),
            name = implFunctionName,
            type = descriptor.defaultType
                .let { typeTranslator.toTypeRef(it, descriptor) }
                .substitute(implCallableSubstitutionMap),
            typeParameters = implCallableTypeParameters,
            valueParameters = injectConstructor.valueParameters
                .map {
                    ValueParameterRef(
                        type = it.type
                            .let { typeTranslator.toTypeRef(it, descriptor) }
                            .substitute(implCallableSubstitutionMap),
                        isExtensionReceiver = false,
                        inlineKind = when {
                            it.isNoinline -> ValueParameterRef.InlineKind.NOINLINE
                            it.isCrossinline -> ValueParameterRef.InlineKind.CROSSINLINE
                            else -> ValueParameterRef.InlineKind.NONE
                        },
                        name = it.name,
                        bindingAdapterArgName = it.getBindingAdapterArgName()
                    )
                },
            targetComponent = targetComponent,
            contributionKind = Callable.ContributionKind.BINDING,
            isCall = true,
            callableKind = Callable.CallableKind.DEFAULT,
            bindingAdapters = emptyList(),
            isEager = false,
            isExternal = false,
            isInline = true,
            isFunBinding = false,
            visibility = Visibilities.PUBLIC,
            receiver = null
        )
        declarationStore.addGeneratedCallable(implCallable, descriptor.findPsi()!!.containingFile as KtFile)
        declarationStore.addGeneratedInternalIndex(
            descriptor.findPsi()!!.containingFile as KtFile,
            Index(implCallable.fqName, "function")
        )

        val superTypeCallableTypeParameters = descriptor.declaredTypeParameters
            .map {
                val raw = typeTranslator.toClassifierRef(it)
                raw.copy(
                    fqName = packageFqName.child(implFunctionName).child(it.name),
                    superTypes = raw.superTypes.map {
                        if (it.classifier.isTypeParameter &&
                            it.classifier.fqName.parent() == packageFqName.child(descriptor.name))
                            it.copy(classifier = it.classifier.copy(
                                fqName = packageFqName.child(implFunctionName).child(it.classifier.fqName.shortName())
                            )) else it
                    }
                )
            }
        val superTypeCallableSubstitutionMap = descriptor.declaredTypeParameters
            .map { typeTranslator.toClassifierRef(it) }
            .zip(implCallableTypeParameters.map { it.defaultType })
            .toMap()
        val superTypeCallable = Callable(
            packageFqName = packageFqName,
            fqName = packageFqName.child(superTypeFunctionName),
            name = superTypeFunctionName,
            type = singleSuperType.substitute(superTypeCallableSubstitutionMap),
            typeParameters = superTypeCallableTypeParameters,
            valueParameters = listOf(
                ValueParameterRef(
                    type = descriptor.defaultType
                        .let { typeTranslator.toTypeRef(it, descriptor) }
                        .substitute(superTypeCallableSubstitutionMap),
                    isExtensionReceiver = true,
                    inlineKind = ValueParameterRef.InlineKind.NONE,
                    name = "_receiver".asNameId(),
                    bindingAdapterArgName = null
                )
            ),
            targetComponent = null,
            contributionKind = Callable.ContributionKind.BINDING,
            isCall = false,
            callableKind = Callable.CallableKind.DEFAULT,
            bindingAdapters = descriptor
                .annotations
                .filter { it.hasAnnotation(InjektFqNames.BindingAdapter) }
                .map { declarationStore.bindingAdapterDescriptorForAnnotation(it, descriptor) },
            isEager = false,
            isExternal = false,
            isInline = true,
            isFunBinding = false,
            visibility = Visibilities.PUBLIC,
            receiver = null
        )

        declarationStore.addGeneratedCallable(superTypeCallable, descriptor.findPsi()!!.containingFile as KtFile)
        declarationStore.addGeneratedInternalIndex(
            descriptor.findPsi()!!.containingFile as KtFile,
            Index(superTypeCallable.fqName, "property")
        )
    }

}
