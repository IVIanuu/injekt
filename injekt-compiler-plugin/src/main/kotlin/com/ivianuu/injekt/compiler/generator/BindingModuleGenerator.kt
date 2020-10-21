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
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

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
                                generateBindingModuleForCallable(
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
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.BindingModule) &&
                            !descriptor.hasAnnotation(InjektFqNames.FunBinding)) {
                            runExitCatching {
                                generateBindingModuleForCallable(
                                    declarationStore.callableForDescriptor(descriptor),
                                    descriptor.findPsi()!!.containingFile as KtFile
                                )
                            }
                        }
                    }
                }
            )
        }

        declarationStore.generatedBindings
            .filter { it.first.bindingModules.isNotEmpty() }
            .forEach { generateBindingModuleForCallable(it.first, it.second) }
    }

    private fun generateBindingModuleForCallable(
        callable: Callable,
        file: KtFile
    ) {
        val bindingModules = callable.bindingModules
            .map { declarationStore.classDescriptorForFqName(it) }
            .map {
                it.unsubstitutedMemberScope.getContributedDescriptors()
                    .filterIsInstance<ClassDescriptor>()
                    .single()
            }

        val targetComponent = bindingModules
            .first()
            .containingDeclaration
            .annotations
            .findAnnotation(InjektFqNames.BindingModule)!!
            .allValueArguments["component".asNameId()]!!
            .let { it as KClassValue }
            .getArgumentType(moduleDescriptor)
            .let { typeTranslator.toTypeRef(it, file) }


        val packageName = callable.packageFqName
        val bindingModuleName = joinedNameOf(
            packageName,
            FqName("${callable.fqName.asString()}BindingModule")
        )

        val rawBindingType = callable.type
        val aliasedType = SimpleTypeRef(
            classifier = ClassifierRef(
                fqName = packageName.child("${bindingModuleName}Alias".asNameId())
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
            emitLine("@MergeInto(${targetComponent.classifier.fqName}::class)")
            emitLine("@Module")
            emit("object $bindingModuleName ")
            braced {
                val (assistedParameters, nonAssistedParameters) = callable.valueParameters
                    .partition { it.isAssisted }

                if (assistedParameters.isNotEmpty() || callable.isEager)
                    emitLine("@${InjektFqNames.Eager}")
                emitLine("@Binding")

                val callableKind = callable.callableKind

                when (callableKind) {
                    Callable.CallableKind.DEFAULT -> {}
                    Callable.CallableKind.SUSPEND -> emit("suspend ")
                    Callable.CallableKind.COMPOSABLE -> emitLine("@${InjektFqNames.Composable}")
                }.let {}

                emit("inline fun aliasedBinding(")
                nonAssistedParameters.forEachIndexed { index, valueParameter ->
                    val typeRef = valueParameter.type
                    if (valueParameter.inlineKind == ValueParameterRef.InlineKind.CROSSINLINE) {
                        emit("crossinline ")
                    } else if (((typeRef.isFunction || typeRef.isSuspendFunction) ||
                                (typeRef.expandedType?.isFunction == true || typeRef.expandedType?.isSuspendFunction == true) ||
                                declarationStore.generatedClassifierFor(typeRef.classifier.fqName) != null)) {
                        emit("noinline ")
                    }
                    emit("${valueParameter.name}: ${valueParameter.type.render()}")
                    if (index != nonAssistedParameters.lastIndex) emit(", ")
                }
                emit("): ${aliasedType.render()} ")
                braced {
                    emit("return ")
                    if (callable.valueParameters.any { it.isAssisted }) {
                        emit("{ ")
                        callable.valueParameters
                            .filter { it.isAssisted }
                            .forEachIndexed { index, parameter ->
                                emit("p$index: ${parameter.type.renderExpanded()}")
                                if (index != callable.valueParameters.lastIndex) emit(", ")
                            }
                        emitLine(" ->")
                        var assistedIndex = 0
                        var nonAssistedIndex = 0
                        emitCallableInvocation(
                            callable,
                            null,
                            callable.valueParameters.map { parameter ->
                                if (parameter.isAssisted) {
                                    { emit("p${assistedIndex++}") }
                                } else {
                                    { emit(parameter.name) }
                                }
                            }
                        )
                        emitLine()
                        emitLine("}")
                    } else {
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
                }
                callables += Callable(
                    packageFqName = packageName,
                    fqName = packageName.child(bindingModuleName)
                        .child("aliasedBinding".asNameId()),
                    name = "aliasedBinding".asNameId(),
                    type = aliasedType,
                    typeParameters = emptyList(),
                    valueParameters = nonAssistedParameters,
                    targetComponent = null,
                    contributionKind = Callable.ContributionKind.BINDING,
                    isCall = true,
                    callableKind = callableKind,
                    bindingModules = emptyList(),
                    isEager = assistedParameters.isNotEmpty() || callable.isEager,
                    isExternal = false,
                    isInline = true
                )
                bindingModules
                    .forEach { bindingModule ->
                        val propertyType = bindingModule.defaultType
                            .let { typeTranslator.toTypeRef(it, file) }
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
                                .let { typeTranslator.toTypeRef(it, file) }
                                .typeWith(listOf(aliasedType)),
                            typeParameters = emptyList(),
                            valueParameters = emptyList(),
                            targetComponent = null,
                            contributionKind = Callable.ContributionKind.MODULE,
                            bindingModules = emptyList(),
                            isCall = false,
                            callableKind = Callable.CallableKind.DEFAULT,
                            isEager = false,
                            isExternal = false,
                            isInline = false
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
            packageFqName = callable.packageFqName,
            fileName = "$bindingModuleName.kt",
            code = code
        )

        declarationStore.addGeneratedInternalIndex(
            file,
            Index(packageName.child(bindingModuleName), "class")
        )
    }

}
