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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext

@Binding
class BindingAdapterGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitClass(klass: KtClass) {
                        super.visitClass(klass)
                        val descriptor = klass.descriptor<ClassDescriptor>(bindingContext)
                            ?: return
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.BindingAdapter)
                            && !descriptor.hasAnnotation(InjektFqNames.ImplBinding)) {
                            runExitCatching {
                                generateBindingAdapterForCallable(
                                    declarationStore.callableForDescriptor(
                                        descriptor.getInjectConstructor()!!
                                    ),
                                    descriptor.findPsi()!!.containingFile as KtFile
                                )
                            }
                        }
                    }

                    override fun visitNamedFunction(function: KtNamedFunction) {
                        super.visitNamedFunction(function)
                        val descriptor = function.descriptor<FunctionDescriptor>(bindingContext)
                            ?: return
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.BindingAdapter) &&
                            !descriptor.hasAnnotation(InjektFqNames.FunBinding)) {
                            runExitCatching {
                                generateBindingAdapterForCallable(
                                    declarationStore.callableForDescriptor(descriptor),
                                    descriptor.findPsi()!!.containingFile as KtFile
                                )
                            }
                        }
                    }

                    override fun visitProperty(property: KtProperty) {
                        super.visitProperty(property)
                        val descriptor = (property.descriptor<DeclarationDescriptor>(bindingContext) as? PropertyDescriptor)
                            ?: return
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.BindingAdapter)) {
                            runExitCatching {
                                generateBindingAdapterForCallable(
                                    declarationStore.callableForDescriptor(descriptor.getter!!),
                                    descriptor.findPsi()!!.containingFile as KtFile
                                )
                            }
                        }
                    }
                }
            )
        }

        declarationStore.generatedCallables
            .filter { it.first.bindingAdapters.isNotEmpty() }
            .forEach { generateBindingAdapterForCallable(it.first, it.second) }
    }

    private fun generateBindingAdapterForCallable(
        callable: Callable,
        file: KtFile
    ) {
        val bindingAdapters = callable.bindingAdapters

        val packageName = callable.packageFqName
        val bindingAdapterNameBaseName = joinedNameOf(
            packageName,
            FqName("${callable.fqName.asString()}BindingAdapter")
        )

        val rawBindingType = callable.type
        val aliasedType = SimpleTypeRef(
            classifier = ClassifierRef(
                fqName = packageName.child("${bindingAdapterNameBaseName}Alias".asNameId()),
                superTypes = listOf(rawBindingType),
                isTypeAlias = true
            ),
            expandedType = rawBindingType
        )

        val callables = mutableListOf<Callable>()

        val code = buildCodeString {
            emitLine("package $packageName")
            emitLine("import ${callable.fqName}")
            emitLine("import ${InjektFqNames.Binding}")
            emitLine("import ${InjektFqNames.MergeInto}")
            emitLine("import ${InjektFqNames.Module}")
            emitLine()
            emitLine("typealias ${aliasedType.classifier.fqName.shortName()} = ${rawBindingType.render()}")
            emitLine()

            val parameters = callable.valueParameters
                .map { valueParameter ->
                    if (callable.isEager &&
                        valueParameter.inlineKind == ValueParameterRef.InlineKind.NONE &&
                        (!valueParameter.type.isFunction ||
                                valueParameter.type.typeArguments.size != 1)) {
                        valueParameter.copy(
                            type = valueParameter.type,
                            inlineKind = ValueParameterRef.InlineKind.CROSSINLINE,
                            isExtensionReceiver = false
                        )
                    } else {
                        valueParameter.copy(isExtensionReceiver = false)
                    }
                }

            if (callable.isEager)
                emitLine("@${InjektFqNames.Eager}")
            emitLine("@Binding")

            val callableKind = callable.callableKind

            when (callableKind) {
                Callable.CallableKind.DEFAULT -> {}
                Callable.CallableKind.SUSPEND -> emit("suspend ")
                Callable.CallableKind.COMPOSABLE -> emitLine("@${InjektFqNames.Composable}")
            }.let {}

            val aliasedBindingName = "${bindingAdapterNameBaseName}_aliasedBinding".asNameId()

            emit("inline fun $aliasedBindingName(")
            parameters.forEachIndexed { index, valueParameter ->
                val typeRef = valueParameter.type
                if (valueParameter.inlineKind == ValueParameterRef.InlineKind.CROSSINLINE) {
                    emit("crossinline ")
                }  else if (typeRef.fullyExpandedType.isFunction || typeRef.fullyExpandedType.isSuspendFunction ||
                    declarationStore.generatedClassifierFor(typeRef.classifier.fqName) != null ||
                    declarationStore.generatedClassifierFor(typeRef.fullyExpandedType.classifier.fqName) != null ||
                    (callable.isFunBinding && typeRef == aliasedType)) {
                    emit("noinline ")
                }
                emit("${valueParameter.name}: ${valueParameter.type.render()}")
                if (index != parameters.lastIndex) emit(", ")
            }
            emit("): ${aliasedType.render()} ")
            braced {
                emit("return ")
                emitCallableInvocation(
                    callable,
                    null,
                    callable.valueParameters.map { parameter ->
                        {
                            emit(parameter.name)
                        }
                    }
                )
            }
            callables += Callable(
                packageFqName = packageName,
                fqName = packageName.child(aliasedBindingName),
                name = aliasedBindingName,
                type = aliasedType,
                typeParameters = emptyList(),
                valueParameters = parameters,
                targetComponent = null,
                contributionKind = Callable.ContributionKind.BINDING,
                isCall = true,
                callableKind = callableKind,
                bindingAdapters = emptyList(),
                isEager = callable.isEager,
                isExternal = false,
                isInline = true,
                isFunBinding = false
            )
            bindingAdapters
                .flatMap { bindingAdapter ->
                    bindingAdapter.module.callables
                        .filter { it.contributionKind != null }
                        .map { adapterCallable ->
                            // todo find a way to dynamically resolve type parameters
                            val substitutionMap = buildMap<ClassifierRef, TypeRef> {
                                bindingAdapter.type.typeArguments
                                    .zip(adapterCallable.typeParameters)
                                    .forEach { (typeArgument, typeParameter) ->
                                        this[typeParameter] = typeArgument
                                    }
                                this[adapterCallable.typeParameters[bindingAdapter.type.typeArguments.size]] = aliasedType
                            }
                            adapterCallable.copy(
                                type = adapterCallable.type
                                    .substitute(substitutionMap)
                                    .substitute(mapOf(aliasedType.classifier to rawBindingType)),
                                valueParameters = adapterCallable.valueParameters.map {
                                    it.copy(type = it.type.substitute(substitutionMap))
                                }
                            )
                        }
                        .map { bindingAdapter to it }
                }
                .forEach { (bindingAdapter, bindingAdapterCallable) ->
                    when (bindingAdapterCallable.contributionKind) {
                        Callable.ContributionKind.BINDING -> {
                            if (bindingAdapterCallable.isEager)
                                emitLine("@${InjektFqNames.Eager}")
                            emit("@Binding")
                            if (bindingAdapterCallable.targetComponent != null) {
                                emitLine("${bindingAdapterCallable.targetComponent.classifier.fqName}")
                            }
                            emitLine()
                        }
                        Callable.ContributionKind.MAP_ENTRIES -> emitLine("@${InjektFqNames.MapEntries}")
                        Callable.ContributionKind.SET_ELEMENTS -> emitLine("@${InjektFqNames.SetElements}")
                        Callable.ContributionKind.MODULE -> {}
                        null -> {}
                    }
                    val functionName = bindingAdapterCallable.fqName.pathSegments().joinToString("_") +
                            "_${bindingAdapterNameBaseName}"

                    when (bindingAdapterCallable.callableKind) {
                        Callable.CallableKind.DEFAULT -> {}
                        Callable.CallableKind.SUSPEND -> emit("suspend ")
                        Callable.CallableKind.COMPOSABLE -> emitLine("@${InjektFqNames.Composable}")
                    }.let {}

                    emit("inline fun $functionName(")

                    bindingAdapterCallable.valueParameters
                        .filter { it.bindingAdapterArgName == null }
                        .forEachIndexed { index, valueParameter ->
                            val typeRef = valueParameter.type
                            if (valueParameter.inlineKind == ValueParameterRef.InlineKind.CROSSINLINE) {
                                emit("crossinline ")
                            } else if (typeRef.fullyExpandedType.isFunction ||
                                typeRef.fullyExpandedType.isSuspendFunction ||
                                declarationStore.generatedClassifierFor(typeRef.classifier.fqName) != null ||
                                declarationStore.generatedClassifierFor(typeRef.fullyExpandedType.classifier.fqName) != null ||
                                (callable.isFunBinding && typeRef == aliasedType)) {
                                emit("noinline ")
                            }
                            emit("${valueParameter.name}: ${valueParameter.type.render()}")
                            if (index != bindingAdapterCallable.valueParameters.lastIndex) emit(", ")
                        }

                    emit("): ${bindingAdapterCallable.type.render()} ")
                    braced {
                        emit("return ")
                        emitCallableInvocation(
                            bindingAdapterCallable,
                            { emit("${bindingAdapter.module.type.classifier.fqName}") },
                            bindingAdapterCallable.valueParameters
                                .map { parameter ->
                                    {
                                        if (parameter.bindingAdapterArgName != null) {
                                            val arg = bindingAdapter.args[parameter.bindingAdapterArgName]
                                            when {
                                                arg != null -> arg()
                                                parameter.type.isMarkedNullable -> emit("null")
                                                else -> error("No argument provided for non null binding arg ${parameter.name}")
                                            }
                                        } else {
                                            emit(parameter.name)
                                        }
                                    }
                                }
                        )
                    }
                    callables += Callable(
                        packageFqName = packageName,
                        fqName = packageName.child(functionName.asNameId()),
                        name = functionName.asNameId(),
                        type = bindingAdapterCallable.type,
                        typeParameters = emptyList(),
                        valueParameters = bindingAdapterCallable.valueParameters
                            .filter { it.bindingAdapterArgName == null }
                            .map { it.copy(isExtensionReceiver = false) },
                        targetComponent = bindingAdapterCallable.targetComponent,
                        contributionKind = bindingAdapterCallable.contributionKind,
                        isCall = true,
                        callableKind = callableKind,
                        bindingAdapters = emptyList(),
                        isEager = bindingAdapterCallable.isEager,
                        isExternal = false,
                        isInline = true,
                        isFunBinding = false
                    )
                }
        }

        fileManager.generateFile(
            packageFqName = callable.packageFqName,
            fileName = "$bindingAdapterNameBaseName.kt",
            code = code
        )

        callables.forEach {
            declarationStore.addGeneratedCallable(it, file)
            declarationStore.addGeneratedInternalIndex(
                file, Index(
                    it.fqName,
                    if (it.isCall) "function" else "property"
                )
            )
        }
    }

}
