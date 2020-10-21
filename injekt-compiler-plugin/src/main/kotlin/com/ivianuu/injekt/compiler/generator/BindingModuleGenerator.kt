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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

@Binding
class BindingModuleGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
    private val moduleDescriptor: ModuleDescriptor,
    private val typeTranslator: TypeTranslator
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitClass(klass: KtClass) {
                        super.visitClass(klass)
                        val descriptor = klass.descriptor<ClassDescriptor>(bindingContext)
                            ?: return
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.BindingModule)) {
                            runExitCatching {
                                generateBindingModuleForDeclaration(descriptor)
                            }
                        }
                    }

                    override fun visitNamedFunction(function: KtNamedFunction) {
                        super.visitNamedFunction(function)
                        val descriptor = function.descriptor<FunctionDescriptor>(bindingContext)
                            ?: return
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.BindingModule)) {
                            runExitCatching {
                                generateBindingModuleForDeclaration(descriptor)
                            }
                        }
                    }
                }
            )
        }
    }

    private fun generateBindingModuleForDeclaration(declaration: DeclarationDescriptor) {
        val bindingModuleAnnotations = declaration
            .getAnnotatedAnnotations(InjektFqNames.BindingModule)
        val bindingModules = bindingModuleAnnotations
            .map { it.type.constructor.declarationDescriptor as ClassDescriptor }
            .map {
                it.unsubstitutedMemberScope.getContributedDescriptors()
                    .filterIsInstance<ClassDescriptor>()
                    .single()
            }

        val targetComponent = bindingModuleAnnotations
            .first()
            .type
            .constructor
            .declarationDescriptor!!
            .annotations
            .findAnnotation(InjektFqNames.BindingModule)!!
            .allValueArguments["component".asNameId()]!!
            .let { it as KClassValue }
            .getArgumentType(moduleDescriptor)
            .let { typeTranslator.toTypeRef(it, declaration) }

        val packageName = declaration.findPackage().fqName
        val bindingModuleName = joinedNameOf(
            packageName,
            FqName("${declaration.fqNameSafe.asString()}BindingModule")
        )

        val rawBindingType = declaration.getBindingType()
        val aliasedType = SimpleTypeRef(
            classifier = ClassifierRef(
                fqName = packageName.child("${bindingModuleName}Alias".asNameId())
            ),
            expandedType = rawBindingType
        )

        val callables = mutableListOf<Callable>()

        val code = buildCodeString {
            emitLine("package $packageName")
            emitLine("import ${declaration.fqNameSafe}")
            emitLine("import ${InjektFqNames.Binding}")
            emitLine("import ${InjektFqNames.MergeInto}")
            emitLine("import ${InjektFqNames.Module}")
            emitLine()
            emitLine("typealias ${aliasedType.classifier.fqName.shortName()} = ${rawBindingType.render()}")
            emitLine()
            emitLine("@MergeInto(${targetComponent.classifier.fqName}::class)")
            emitLine("@Module")
            emit("object $bindingModuleName ")
            braced {
                val assistedParameters = mutableListOf<ValueParameterRef>()
                val valueParameters = mutableListOf<ValueParameterRef>()
                if (declaration is ClassDescriptor) {
                    declaration.getInjectConstructor()
                        ?.valueParameters
                        ?.map {
                            ValueParameterRef(
                                type = it.type.let { typeTranslator.toTypeRef(it, declaration) },
                                isExtensionReceiver = false,
                                isAssisted = it.type.hasAnnotation(InjektFqNames.Assisted),
                                name = it.name
                            )
                        }
                        ?.forEach { valueParameter ->
                            if (valueParameter.isAssisted) {
                                assistedParameters += valueParameter
                            } else {
                                valueParameters += valueParameter
                            }
                        }
                } else if (declaration is FunctionDescriptor) {
                    declaration
                        .valueParameters
                        .map {
                            ValueParameterRef(
                                type = it.type.let { typeTranslator.toTypeRef(it, declaration) },
                                isExtensionReceiver = false,
                                isAssisted = it.type.hasAnnotation(InjektFqNames.Assisted),
                                name = it.name
                            )
                        }
                        .forEach { valueParameter ->
                            if (valueParameter.isAssisted) {
                                assistedParameters += valueParameter
                            } else {
                                valueParameters += valueParameter
                            }
                        }
                }

                emitLine("@Binding")

                val callableKind = if (declaration is FunctionDescriptor &&
                    !declaration.hasAnnotation(InjektFqNames.FunBinding) &&
                    assistedParameters.isEmpty()) {
                    when {
                        declaration.isSuspend -> Callable.CallableKind.SUSPEND
                        declaration.hasAnnotation(InjektFqNames.Composable) -> Callable.CallableKind.COMPOSABLE
                        else -> Callable.CallableKind.DEFAULT
                    }
                } else {
                    Callable.CallableKind.DEFAULT
                }

                when (callableKind) {
                    Callable.CallableKind.DEFAULT -> {}
                    Callable.CallableKind.SUSPEND -> emit("suspend ")
                    Callable.CallableKind.COMPOSABLE -> emitLine("@${InjektFqNames.Composable}")
                }.let {}

                emit("inline fun aliasedBinding(")
                valueParameters.forEachIndexed { index, valueParameter ->
                    val typeRef = valueParameter.type
                    if ((typeRef.isFunction || typeRef.isSuspendFunction) ||
                        (typeRef.expandedType?.isFunction == true || typeRef.expandedType?.isSuspendFunction == true) ||
                        declarationStore.generatedClassifierFor(typeRef.classifier.fqName) != null) {
                        emit("noinline ")
                    }
                    emit("${valueParameter.name}: ${valueParameter.type.render()}")
                    if (index != valueParameters.lastIndex) emit(", ")
                }
                emit("): ${aliasedType.render()} ")
                braced {
                    if (declaration is FunctionDescriptor) {
                        val callable = declarationStore.callableForDescriptor(declaration)
                        fun emitCall() {
                            fun emitCallInner() {
                                emit("${declaration.name}(")
                                val callValueParameters = callable.valueParameters
                                    .filterNot { it.isExtensionReceiver }
                                callValueParameters
                                    .forEachIndexed { index, valueParameter ->
                                        emit(valueParameter.name)
                                        if (index != callValueParameters.lastIndex) emit(", ")
                                    }
                                emit(")")
                            }
                            if (declaration.containingDeclaration is ClassDescriptor) {
                                emit("with(${declaration.containingDeclaration.fqNameSafe}) ")
                                braced { emitCallInner() }
                            } else {
                                emitCallInner()
                            }
                        }

                        if (declaration.hasAnnotation(InjektFqNames.FunBinding) ||
                                assistedParameters.isNotEmpty()) {
                            emit("return { ")
                            assistedParameters.forEachIndexed { index, valueParameter ->
                                emit("${valueParameter.name}: ${valueParameter.type.render()}")
                                if (index != assistedParameters.lastIndex) emit(", ") else emit(" -> ")
                            }
                            emitLine()
                            if (callable.valueParameters.any { it.isExtensionReceiver }) {
                                indented {
                                    emit("with(${callable.valueParameters.first().name}) ")
                                    braced { emitCall() }
                                }
                            } else {
                                emitCall()
                            }
                            emitLine(" }")
                        } else {
                            emit("return ")
                            emitCall()
                        }
                    } else {
                        declaration as ClassDescriptor
                        if (declaration.kind == ClassKind.OBJECT) {
                            emit("return ${rawBindingType.classifier.fqName}")
                        } else {
                            if (assistedParameters.isNotEmpty()) {
                                emit("return { ")
                                assistedParameters.forEachIndexed { index, valueParameter ->
                                    emit("${valueParameter.name}: ${valueParameter.type.render()}")
                                    if (index != assistedParameters.lastIndex) emit(", ") else emit(" -> ")
                                }
                                indented {
                                    emit("${declaration.name}(")
                                    val constructorCallable = declarationStore.callableForDescriptor(
                                        declaration.getInjectConstructor()!!
                                    )
                                    constructorCallable.valueParameters.forEachIndexed { index, valueParameter ->
                                        emit(valueParameter.name)
                                        if (index != constructorCallable.valueParameters.lastIndex) emit(", ")
                                    }
                                    emit(")")
                                }
                                emitLine(" }")
                            } else {
                                emit("return ${rawBindingType.classifier.fqName}(")
                                valueParameters.forEachIndexed { index, valueParameter ->
                                    emit(valueParameter.name)
                                    if (index != valueParameters.lastIndex) emit(", ")
                                }
                                emit(")")
                            }
                        }
                    }
                }
                callables += Callable(
                    packageFqName = packageName,
                    fqName = packageName.child(bindingModuleName)
                        .child("aliasedBinding".asNameId()),
                    name = "aliasedBinding".asNameId(),
                    type = aliasedType,
                    typeParameters = emptyList(),
                    valueParameters = valueParameters,
                    targetComponent = null,
                    contributionKind = Callable.ContributionKind.BINDING,
                    isCall = true,
                    callableKind = callableKind,
                    isExternal = false
                )
                bindingModules
                    .forEach { bindingModule ->
                        val propertyType = bindingModule.defaultType
                            .let { typeTranslator.toTypeRef(it, declaration) }
                            .typeWith(listOf(aliasedType))
                        val propertyName = propertyType
                            .uniqueTypeName()
                        emit("@Module val $propertyName: ${propertyType.render()} = ${bindingModule.fqNameSafe}")
                        if (bindingModule.kind != ClassKind.OBJECT) {
                            emitLine("()")
                        } else {
                            emitLine()
                        }
                        callables += Callable(
                            packageFqName = packageName,
                            fqName = packageName.child(bindingModuleName)
                                .child(propertyName),
                            name = propertyName,
                            type = bindingModule.defaultType
                                .let { typeTranslator.toTypeRef(it, declaration) }
                                .typeWith(listOf(aliasedType)),
                            typeParameters = emptyList(),
                            valueParameters = emptyList(),
                            targetComponent = null,
                            contributionKind = Callable.ContributionKind.MODULE,
                            isCall = false,
                            callableKind = Callable.CallableKind.DEFAULT,
                            isExternal = false
                        )
                    }
            }
        }

        declarationStore.addGeneratedMergeModule(
            targetComponent,
            ModuleDescriptor(
                type = SimpleTypeRef(
                    classifier = ClassifierRef(
                        fqName = packageName.child(bindingModuleName),
                        isObject = true
                    ),
                    isModule = true
                ),
                callables = callables
            )
        )

        fileManager.generateFile(
            packageFqName = declaration.findPackage().fqName,
            fileName = "$bindingModuleName.kt",
            code = code
        )

        declarationStore.addGeneratedInternalIndex(
            declaration.findPsi()!!.containingFile as KtFile,
            Index(packageName.child(bindingModuleName), "class")
        )
    }

    private fun DeclarationDescriptor.getBindingType(): TypeRef {
        return when (this) {
            is ClassDescriptor -> {
                declarationStore.callableForDescriptor(getInjectConstructor()!!).type
            }
            is FunctionDescriptor -> {
                val assistedParameters = valueParameters
                    .filter { it.type.hasAnnotation(InjektFqNames.Assisted) }
                if (!hasAnnotation(InjektFqNames.FunBinding) && assistedParameters.isEmpty()) {
                    returnType!!.let { typeTranslator.toTypeRef(it, this) }
                } else {
                    (if (isSuspend) moduleDescriptor.builtIns.getSuspendFunction(assistedParameters.size)
                    else moduleDescriptor.builtIns.getFunction(assistedParameters.size))
                        .defaultType
                        .replace(
                            newArguments = (assistedParameters
                                .map { it.type } + returnType!!)
                                .map { it.asTypeProjection() }
                        )
                        .let { typeTranslator.toTypeRef(it, this) }
                        .copy(isComposable = hasAnnotation(InjektFqNames.Composable))
                }
            }
            is PropertyDescriptor -> type.let { typeTranslator.toTypeRef(it, this) }
            else -> error("Unexpected given declaration $this")
        }
    }
}
