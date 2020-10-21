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
        fileManager.generateFile(
            packageFqName = packageFqName,
            fileName = fileName,
            code = buildCodeString {
                emitLine("package $packageFqName")
                emitLine("import ${InjektFqNames.Binding}")

                emit("@Binding")
                if (targetComponent != null) emitLine("(${targetComponent.classifier.fqName}::class)")
                else emitLine()
                emitLine("fun $implFunctionName(")
                injectConstructor.valueParameters
                    .forEachIndexed { index, valueParameter ->
                        emit("${valueParameter.name}: ${valueParameter.type
                            .let { typeTranslator.toTypeRef(it, descriptor) }.render()}")
                        if (index != injectConstructor.valueParameters.lastIndex) emit(", ")
                    }
                emit("): ${descriptor.defaultType.let { typeTranslator.toTypeRef(it, descriptor) }.render()} ")
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
                emitLine("val ${descriptor.name}.$superTypeFunctionName: ${singleSuperType.render()}")
                indented { emitLine("get() = this") }
            }
        )

        val implCallable = Callable(
            packageFqName = packageFqName,
            fqName = packageFqName.child(implFunctionName),
            name = implFunctionName,
            type = descriptor.defaultType
                .let { typeTranslator.toTypeRef(it, descriptor) },
            typeParameters = emptyList(),
            valueParameters = injectConstructor.valueParameters
                .map {
                    ValueParameterRef(
                        type = it.type
                            .let { typeTranslator.toTypeRef(it, descriptor) },
                        isExtensionReceiver = false,
                        isAssisted = it.type.hasAnnotation(InjektFqNames.Assisted),
                        name = it.name
                    )
                },
            targetComponent = targetComponent,
            contributionKind = Callable.ContributionKind.BINDING,
            isCall = true,
            callableKind = Callable.CallableKind.DEFAULT,
            isExternal = false
        )
        declarationStore.addGeneratedBinding(implCallable)
        declarationStore.addGeneratedInternalIndex(
            descriptor.findPsi()!!.containingFile as KtFile,
            Index(implCallable.fqName, "function")
        )

        val superTypeCallable = Callable(
            packageFqName = packageFqName,
            fqName = packageFqName.child(superTypeFunctionName),
            name = superTypeFunctionName,
            type = singleSuperType,
            typeParameters = emptyList(),
            valueParameters = listOf(
                ValueParameterRef(
                    type = descriptor.defaultType
                        .let { typeTranslator.toTypeRef(it, descriptor) },
                    isExtensionReceiver = true,
                    isAssisted = false,
                    name = "receiver".asNameId()
                )
            ),
            targetComponent = null,
            contributionKind = Callable.ContributionKind.BINDING,
            isCall = false,
            callableKind = Callable.CallableKind.DEFAULT,
            isExternal = false
        )

        declarationStore.addGeneratedBinding(superTypeCallable)
        declarationStore.addGeneratedInternalIndex(
            descriptor.findPsi()!!.containingFile as KtFile,
            Index(superTypeCallable.fqName, "property")
        )
    }

}
