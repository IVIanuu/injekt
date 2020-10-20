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
                            generateBindingModuleForCallable(
                                declarationStore.callableForDescriptor(
                                    descriptor.getInjectConstructor()!!
                                ),
                                file
                            )
                        }
                    }

                    override fun visitNamedFunction(function: KtNamedFunction) {
                        super.visitNamedFunction(function)
                        val descriptor = function.descriptor<FunctionDescriptor>(bindingContext)
                            ?: return
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.BindingModule) &&
                                !descriptor.hasAnnotation(InjektFqNames.FunBinding)) {
                            generateBindingModuleForCallable(
                                declarationStore.callableForDescriptor(descriptor),
                                file
                            )
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
            .let { typeTranslator.toTypeRef(it) }

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
                emitLine("@Binding")

                when (callable.callableKind) {
                    Callable.CallableKind.DEFAULT -> {
                    }
                    Callable.CallableKind.SUSPEND -> emit("suspend ")
                    Callable.CallableKind.COMPOSABLE -> emitLine("@${InjektFqNames.Composable}")
                }.let {}

                emit("fun aliasedBinding(")
                callable.valueParameters.forEachIndexed { index, valueParameter ->
                    emit("${valueParameter.name}: ${valueParameter.type.render()}")
                    if (index != callable.valueParameters.lastIndex) emit(", ")
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
                    fqName = packageName.child(bindingModuleName)
                        .child("aliasedBinding".asNameId()),
                    name = "aliasedBinding".asNameId(),
                    type = aliasedType,
                    typeParameters = emptyList(),
                    valueParameters = callable.valueParameters,
                    targetComponent = null,
                    contributionKind = Callable.ContributionKind.BINDING,
                    bindingModules = emptyList(),
                    isCall = true,
                    callableKind = callable.callableKind,
                    isExternal = false
                )
                bindingModules
                    .forEach { bindingModule ->
                        val propertyType = bindingModule.defaultType
                            .let { typeTranslator.toTypeRef(it) }
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
                                .let { typeTranslator.toTypeRef(it) }
                                .typeWith(listOf(aliasedType)),
                            typeParameters = emptyList(),
                            valueParameters = emptyList(),
                            targetComponent = null,
                            contributionKind = Callable.ContributionKind.MODULE,
                            bindingModules = emptyList(),
                            isCall = false,
                            callableKind = Callable.CallableKind.DEFAULT,
                            isExternal = false
                        )
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
        }
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
