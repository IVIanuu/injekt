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
import com.ivianuu.injekt.compiler.GenerateComponents
import com.ivianuu.injekt.compiler.GenerateMergeComponents
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.generator.componentimpl.CallableBindingNode
import com.ivianuu.injekt.compiler.generator.componentimpl.ComponentFactoryType
import com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl
import com.ivianuu.injekt.compiler.generator.componentimpl.ComponentType
import com.ivianuu.injekt.compiler.generator.componentimpl.Parent
import com.ivianuu.injekt.compiler.generator.componentimpl.ScopeType
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.callExpressionRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Binding class ComponentGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
    private val componentImplFactory: (
        ComponentType?,
        Callable?,
        ScopeType?,
        ComponentFactoryType?,
        Name,
        List<TypeRef>,
        Boolean,
        @Parent ComponentImpl?,
    ) -> ComponentImpl,
    private val generateComponents: GenerateComponents,
    private val generateMergeComponents: GenerateMergeComponents,
    private val lazyTopDownAnalyzer: LazyTopDownAnalyzer,
    private val moduleDescriptor: ModuleDescriptor,
) : Generator {
    override fun generate(files: List<KtFile>) {
        if (generateComponents) {
            files.forEach { file ->
                file.accept(
                    callExpressionRecursiveVisitor { expression ->
                        if (expression.calleeExpression?.text == "create") {
                            val expressionScope = expression.parents
                                .first {
                                    (it is KtNamedFunction ||
                                            it is KtProperty ||
                                            it is KtClass) &&
                                            (it !is KtProperty || !it.isLocal)
                                } as KtNamedDeclaration
                            lazyTopDownAnalyzer.analyzeDeclarations(
                                TopDownAnalysisMode.LocalDeclarations,
                                listOf(expressionScope)
                            )
                            val resolvedCall = expression.getResolvedCall(bindingContext)!!
                            if (resolvedCall.resultingDescriptor.fqNameSafe ==
                                InjektFqNames.create
                            ) {
                                val returnType =
                                    resolvedCall.typeArguments.values.single().toTypeRef()
                                val scopeType = returnType.classifier.targetScope
                                    ?: returnType.targetScope

                                val inputs = resolvedCall.valueArguments
                                    .values
                                    .flatMap { it.arguments }
                                    .map {
                                        it.getArgumentExpression()!!.getType(bindingContext)!!
                                            .toTypeRef()
                                    }

                                val name = "${
                                    expressionScope.fqName!!.pathSegments().joinToString("_")
                                }_${expression.startOffset}"

                                generateComponent(
                                    if (returnType.classifier.isComponent ||
                                        returnType.classifier.isMergeComponent
                                    ) returnType
                                    else moduleDescriptor.builtIns.getFunction(0)
                                        .defaultType.toTypeRef()
                                        .typeWith(listOf(returnType)),
                                    scopeType,
                                    inputs,
                                    name.asNameId(),
                                    expression.containingKtFile
                                )
                            }
                        }
                    }
                )
            }
        }
        /*if (generateMergeComponents) {
            declarationStore.mergeComponents
                .forEach { generateComponent(it) }
        }*/
    }

    private fun generateComponent(
        componentType: TypeRef,
        scopeType: TypeRef?,
        inputs: List<TypeRef>,
        name: Name,
        originatingFile: KtFile,
    ) {
        val componentImpl = componentImplFactory(
            componentType,
            null,
            scopeType,
            null,
            name,
            inputs,
            false,
            null
        )

        componentImpl.initialize()

        // extensions functions cannot be called by their fully qualified name
        // because of that we collect all extension function calls and import them
        val imports = mutableSetOf<FqName>()

        fun ComponentImpl.collectImports() {
            imports += graph.resolvedBindings.values
                .mapNotNull {
                    it to ((if (it is CallableBindingNode) it.callable
                    else null) ?: return@mapNotNull null)
                }
                .filter {
                    it.second.valueParameters.none {
                        it.parameterKind == ValueParameterRef.ParameterKind.DISPATCH_RECEIVER
                    } && it.second.valueParameters.any {
                        it.parameterKind == ValueParameterRef.ParameterKind.EXTENSION_RECEIVER
                    }
                }
                .map { it.second.fqName }
            if (graph.resolvedBindings.values
                    .filter {
                        it.targetComponent != null && it.callableKind == Callable.CallableKind.SUSPEND
                    }
                    .any()
            ) {
                imports += FqName("kotlinx.coroutines.sync.withLock")
            }
            children.forEach { it.collectImports() }
        }

        componentImpl.collectImports()

        val allComponents = buildList<ComponentImpl> {
            fun ComponentImpl.collectComponents() {
                this@buildList += this
                children.forEach { it.collectComponents() }
            }
            componentImpl.collectComponents()
        }

        // it's important to first emit the children because the may add
        // additional properties to the parent in the code building step
        // this should be changed in the future
        val componentsCode = allComponents.reversed().map { component ->
            buildCodeString {
                with(component) {
                    emit()
                }
            }
        }.reversed()

        val code = buildCodeString {
            emitLine("@file:Suppress(\"UNCHECKED_CAST\", \"NOTHING_TO_INLINE\")")
            emitLine("package ${originatingFile.packageFqName}")
            imports.forEach { emitLine("import $it") }

            componentsCode.forEach {
                emit(it)
                emitLine()
            }
        }

        fileManager.generateFile(
            originatingFile = null,
            packageFqName = originatingFile.packageFqName,
            fileName = "$name.kt",
            code = code
        )
    }
}
