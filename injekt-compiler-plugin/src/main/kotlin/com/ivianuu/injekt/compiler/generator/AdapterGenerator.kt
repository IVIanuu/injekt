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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext

@Binding
class AdapterGenerator(
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
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.Adapter)
                            && !descriptor.hasAnnotation(InjektFqNames.ImplBinding)) {
                            runExitCatching {
                                generateAdapterForCallable(
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
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.Adapter) &&
                            !descriptor.hasAnnotation(InjektFqNames.FunBinding) &&
                            descriptor.containingDeclaration.containingDeclaration
                                ?.hasAnnotation(InjektFqNames.Adapter) != true) {
                            runExitCatching {
                                generateAdapterForCallable(
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
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.Adapter) &&
                            descriptor.containingDeclaration.containingDeclaration
                                ?.hasAnnotation(InjektFqNames.Adapter) != true) {
                            runExitCatching {
                                generateAdapterForCallable(
                                    declarationStore.callableForDescriptor(descriptor.getter!!),
                                    descriptor.findPsi()!!.containingFile as KtFile
                                )
                            }
                        }
                    }
                }
            )
        }

        val processedCallables = mutableSetOf<Pair<Callable, KtFile>>()
        while (true) {
            val unprocessedCallables = declarationStore.generatedCallables - processedCallables
            if (unprocessedCallables.isEmpty()) break
            unprocessedCallables.forEach { (callable, file) ->
                if (callable.adapters.isNotEmpty()) {
                    generateAdapterForCallable(callable, file)
                }
            }
            processedCallables += unprocessedCallables
        }
    }

    private fun generateAdapterForCallable(
        callable: Callable,
        file: KtFile
    ) {
        val adapters = callable.adapters

        val packageName = callable.packageFqName
        val adapterNameBaseName = joinedNameOf(
            packageName,
            FqName("${callable.fqName.asString()}Adapter")
        )

        val rawBindingType = callable.type
        val aliasedType = SimpleTypeRef(
            classifier = ClassifierRef(
                fqName = packageName.child("${adapterNameBaseName}Alias".asNameId()),
                superTypes = listOf(rawBindingType),
                isTypeAlias = true
            ),
            expandedType = rawBindingType
        )

        val callables = mutableListOf<Callable>()

        val code = buildCodeString {
            emitLine("@file:Suppress(\"UNCHECKED_CAST\", \"NOTHING_TO_INLINE\")")
            emitLine("package $packageName")
            val imports = mutableSetOf(
                "import ${callable.fqName}",
                "import ${InjektFqNames.Binding}",
                "import ${InjektFqNames.MergeInto}",
                "import ${InjektFqNames.Module}"
            )
            imports += file
                .importDirectives
                .map { it.text }

            imports.forEach {
                emitLine(it)
            }

            emitLine()
            emitLine("typealias ${aliasedType.classifier.fqName.shortName()} = ${rawBindingType.render()}")
            emitLine()

            val parameters = callable.valueParameters
                .map { valueParameter ->
                    if (callable.isFunBinding &&
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

            if (callable.isFunBinding)
                emitLine("@${InjektFqNames.FunBinding}")
            emitLine("@Binding")

            val callableKind = callable.callableKind

            when (callableKind) {
                Callable.CallableKind.DEFAULT -> {}
                Callable.CallableKind.SUSPEND -> emit("suspend ")
                Callable.CallableKind.COMPOSABLE -> emitLine("@${InjektFqNames.Composable}")
            }.let {}

            val aliasedBindingName = "${adapterNameBaseName}_aliasedBinding".asNameId()

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
                if (valueParameter.defaultExpression != null) {
                    emit(" = ")
                    valueParameter.defaultExpression!!()
                }
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
                adapters = emptyList(),
                isExternal = false,
                isInline = true,
                isFunBinding = callable.isFunBinding,
                visibility = Visibilities.PUBLIC,
                modality = Modality.FINAL,
                receiver = null
            )
            adapters
                .flatMap { adapter ->
                    adapter.module.callables
                        .filter {
                            it.contributionKind != null || it.adapters.isNotEmpty()
                        }
                        .map { adapterCallable ->
                            // todo find a way to dynamically resolve type parameters
                            val substitutionMap = mutableMapOf<ClassifierRef, TypeRef>()

                            val adapterTypeParameters = adapterCallable.typeParameters
                                .take(adapter.type.typeArguments.size)
                                .zip(adapter.type.typeArguments)
                                .toMap()

                            substitutionMap += adapterTypeParameters
                            adapterTypeParameters.forEach { (typeParameter, typeArgument) ->
                                substitutionMap += typeArgument.getSubstitutionMap(typeParameter.defaultType)
                            }

                            val subjectTypeParameter = adapterCallable.typeParameters[adapterTypeParameters.size]
                            substitutionMap += rawBindingType.getSubstitutionMap(subjectTypeParameter.defaultType)

                            check(adapterCallable.typeParameters.all { it in substitutionMap }) {
                                "Couldn't resolve all type arguments ${substitutionMap.map { 
                                    it.key.fqName to it.value
                                }} missing ${adapterCallable.typeParameters.filter { 
                                    it !in substitutionMap
                                }.map { it.fqName }} in ${file.virtualFilePath}"
                            }
                            substitutionMap[subjectTypeParameter] = aliasedType

                            substitutionMap to adapterCallable.copy(
                                type = adapterCallable.type
                                    .substitute(substitutionMap)
                                    // map the aliased to type to the raw binding type
                                    // if the callable returns the alias
                                    .substitute(mapOf(aliasedType.classifier to rawBindingType)),
                                valueParameters = adapterCallable.valueParameters.map {
                                    it.copy(type = it.type.substitute(substitutionMap))
                                }
                            )
                        }
                        .map { Triple(adapter, it.first, it.second) }
                }
                .forEach { (adapter, substitutionMap, adapterCallable) ->
                    when (adapterCallable.contributionKind) {
                        Callable.ContributionKind.BINDING -> {
                            emit("@Binding")
                            if (adapterCallable.targetComponent != null) {
                                emitLine("(${adapterCallable.targetComponent.classifier.fqName}::class)")
                            }
                            emitLine()
                        }
                        Callable.ContributionKind.MAP_ENTRIES -> emitLine("@${InjektFqNames.MapEntries}")
                        Callable.ContributionKind.SET_ELEMENTS -> emitLine("@${InjektFqNames.SetElements}")
                        Callable.ContributionKind.MODULE -> {}
                        null -> {}
                    }
                    adapterCallable.adapters
                        .forEach { innerAdapter ->
                            emit("@${innerAdapter.type.classifier.fqName}(")
                            innerAdapter.args.toList().forEachIndexed { index, (argName, argExpression) ->
                                emit("$argName = ")
                                argExpression()
                                if (index != innerAdapter.args.toList().lastIndex) emit(", ")
                            }
                            emitLine(")")
                        }
                    val functionName = adapterCallable.fqName.pathSegments().joinToString("_") +
                            "_${adapterNameBaseName}"

                    when (adapterCallable.callableKind) {
                        Callable.CallableKind.DEFAULT -> {}
                        Callable.CallableKind.SUSPEND -> emit("suspend ")
                        Callable.CallableKind.COMPOSABLE -> emitLine("@${InjektFqNames.Composable}")
                    }.let {}

                    emit("inline fun $functionName(")

                    adapterCallable.valueParameters
                        .filter { it.adapterArgName == null }
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
                            if (index != adapterCallable.valueParameters.lastIndex) emit(", ")
                        }

                    emit("): ${adapterCallable.type.render()} ")
                    braced {
                        emit("return ")
                        emitCallableInvocation(
                            adapterCallable,
                            { emit("${adapter.module.type.classifier.fqName}") },
                            adapterCallable.valueParameters
                                .map { parameter ->
                                    {
                                        if (parameter.adapterArgName != null) {
                                            val arg = adapter.args[parameter.adapterArgName]
                                            when {
                                                arg != null -> arg()
                                                parameter.type.isMarkedNullable -> emit("null")
                                                else -> error("No argument provided for non null binding arg ${parameter.name} in ${file.virtualFilePath}")
                                            }
                                        } else {
                                            emit(parameter.name)
                                        }
                                    }
                                },
                            adapterCallable.typeParameters
                                .map { substitutionMap.getValue(it) }
                        )
                    }
                    callables += Callable(
                        packageFqName = packageName,
                        fqName = packageName.child(functionName.asNameId()),
                        name = functionName.asNameId(),
                        type = adapterCallable.type,
                        typeParameters = emptyList(),
                        valueParameters = adapterCallable.valueParameters
                            .filter { it.adapterArgName == null }
                            .map { it.copy(isExtensionReceiver = false) },
                        targetComponent = adapterCallable.targetComponent,
                        contributionKind = adapterCallable.contributionKind,
                        isCall = true,
                        callableKind = adapterCallable.callableKind,
                        adapters = adapterCallable.adapters,
                        isExternal = false,
                        isInline = true,
                        isFunBinding = adapterCallable.isFunBinding,
                        visibility = Visibilities.PUBLIC,
                        modality = Modality.FINAL,
                        receiver = null
                    )
                }
        }

        fileManager.generateFile(
            packageFqName = callable.packageFqName,
            fileName = "$adapterNameBaseName.kt",
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
