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

package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.ClassPath
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.Path
import com.ivianuu.injekt.compiler.PropertyPath
import com.ivianuu.injekt.compiler.TypeParameterPath
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeWith
import com.ivianuu.injekt.compiler.withAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ModuleDeclarationFactory(
    private val moduleFunction: IrFunction,
    private val originalModuleFunction: IrFunction,
    private val moduleClass: IrClass,
    private val pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore,
    private val nameProvider: NameProvider,
    private val symbols: InjektSymbols
) {

    fun createDeclarations(
        callee: IrFunction,
        call: IrCall
    ): List<ModuleDeclaration> {
        val calleeFqName = callee.descriptor.fqNameSafe.asString()

        return when {
            calleeFqName == "com.ivianuu.injekt.scope" ->
                listOf(createScopeDeclaration(call))
            calleeFqName == "com.ivianuu.injekt.dependency" ->
                listOf(createDependencyDeclaration(call))
            calleeFqName == "com.ivianuu.injekt.childFactory" ->
                listOf(createChildFactoryDeclaration(call))
            calleeFqName == "com.ivianuu.injekt.alias" ->
                listOf(createAliasDeclaration(call))
            calleeFqName == "com.ivianuu.injekt.transient" ||
                    calleeFqName == "com.ivianuu.injekt.scoped" ||
                    calleeFqName == "com.ivianuu.injekt.instance" -> {
                listOf(
                    createBindingDeclaration(
                        call.getTypeArgument(0)!!
                            .remapTypeParameters(originalModuleFunction, moduleFunction)
                            .remapTypeParameters(moduleFunction, moduleClass),
                        if (call.valueArgumentsCount != 0) call.getValueArgument(0) else null,
                        call.symbol.owner.name.asString() == "instance",
                        call.symbol.owner.name.asString() == "scoped"
                    )
                )
            }
            calleeFqName == "com.ivianuu.injekt.map" ->
                createMapDeclarations(call)
            calleeFqName == "com.ivianuu.injekt.set" ->
                createSetDeclarations(call)
            callee.hasAnnotation(InjektFqNames.Module) ->
                createIncludedModuleDeclaration(call, callee)
            else -> emptyList()
        }
    }

    private fun createScopeDeclaration(call: IrCall): ScopeDeclaration =
        ScopeDeclaration(call.getTypeArgument(0)!!)

    private fun createDependencyDeclaration(call: IrCall): DependencyDeclaration {
        val dependencyType = call.getTypeArgument(0)!!
            .remapTypeParameters(moduleFunction, moduleClass)
        val property = InjektDeclarationIrBuilder(pluginContext, moduleClass.symbol)
            .fieldBackedProperty(
                moduleClass,
                nameProvider.allocateForType(dependencyType),
                dependencyType
            )
        return DependencyDeclaration(
            dependencyType,
            PropertyPath(property),
            call.getValueArgument(0)!!
        )
    }

    private fun createChildFactoryDeclaration(call: IrCall): ChildFactoryDeclaration {
        val factoryRef = call.getValueArgument(0)!! as IrFunctionReference
        val factoryModuleClass = declarationStore.getModuleClassForFunction(
            declarationStore.getModuleFunctionForFactory(factoryRef.symbol.owner)
        )
        return ChildFactoryDeclaration(factoryRef, factoryModuleClass)
    }

    private fun createAliasDeclaration(call: IrCall): AliasDeclaration =
        AliasDeclaration(call.getTypeArgument(0)!!, call.getTypeArgument(1)!!)

    private fun createMapDeclarations(call: IrCall): List<ModuleDeclaration> {
        val declarations = mutableListOf<ModuleDeclaration>()

        val mapType = if (call.typeArgumentsCount == 3) {
            call.getTypeArgument(0)!!
        } else {
            pluginContext.referenceClass(KotlinBuiltIns.FQ_NAMES.map)!!
                .typeWith(call.getTypeArgument(0)!!, call.getTypeArgument(1)!!)
        }

        declarations += MapDeclaration(mapType)

        val mapBlock = call.getValueArgument(0) as? IrFunctionExpression
        mapBlock?.function?.body?.transformChildrenVoid(object :
            IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol == symbols.mapDsl.functions.single { it.owner.name.asString() == "put" }) {
                    declarations += MapEntryDeclaration(
                        mapType,
                        expression.getValueArgument(0)!!,
                        expression.getTypeArgument(0)!!
                    )
                }

                return super.visitCall(expression)
            }
        })

        return declarations
    }

    private fun createSetDeclarations(call: IrCall): List<ModuleDeclaration> {
        val declarations = mutableListOf<ModuleDeclaration>()

        val setType = if (call.typeArgumentsCount == 2) {
            call.getTypeArgument(0)!!
        } else {
            pluginContext.referenceClass(KotlinBuiltIns.FQ_NAMES.set)!!
                .typeWith(call.getTypeArgument(0)!!)
        }

        declarations += SetDeclaration(setType)

        val setBlock = call.getValueArgument(0) as? IrFunctionExpression
        setBlock?.function?.body?.transformChildrenVoid(object :
            IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol == symbols.setDsl.functions.single { it.owner.name.asString() == "add" }) {
                    declarations += SetElementDeclaration(
                        setType,
                        expression.getTypeArgument(0)!!
                    )
                }
                return super.visitCall(expression)
            }
        })

        return declarations
    }

    private fun createIncludedModuleDeclaration(
        call: IrCall,
        includedModuleFunction: IrFunction
    ): List<ModuleDeclaration> {
        val declarations = mutableListOf<ModuleDeclaration>()
        val includedClass = includedModuleFunction.returnType.classOrNull!!.owner
        val includedType = includedModuleFunction.returnType
            .typeWith(*call.typeArguments.toTypedArray())
            .remapTypeParameters(originalModuleFunction, moduleFunction)
            .remapTypeParameters(moduleFunction, moduleClass)

        val path = if (includedClass.kind != ClassKind.OBJECT) {
            val property = InjektDeclarationIrBuilder(pluginContext, includedClass.symbol)
                .fieldBackedProperty(
                    moduleClass,
                    Name.identifier(nameProvider.allocateForGroup(includedModuleFunction.name.asString())),
                    includedType
                )
            PropertyPath(property)
        } else {
            ClassPath(includedClass)
        }

        declarations += IncludedModuleDeclaration(
            includedType,
            path,
            if (path is PropertyPath) call else null
        )

        val includedDescriptor = includedClass
            .declarations
            .single { it.descriptor.name.asString() == "Descriptor" } as IrClass

        declarations += includedDescriptor
            .functions
            .filter { it.descriptor.hasAnnotation(InjektFqNames.AstBinding) }
            .filter { it.descriptor.hasAnnotation(InjektFqNames.AstTypeParameterPath) }
            .map { bindingFunction ->
                val bindingType =
                    bindingFunction.getAnnotation(InjektFqNames.AstTypeParameterPath)!!
                        .getValueArgument(0)!!
                        .let { it as IrConst<String> }.value
                        .let { typeParameterName ->
                            val index = includedClass.typeParameters
                                .indexOfFirst { it.name.asString() == typeParameterName }
                            call.getTypeArgument(index)!!
                                .remapTypeParameters(originalModuleFunction, moduleFunction)
                                .remapTypeParameters(moduleFunction, moduleClass)
                        }
                        .withAnnotations(bindingFunction.returnType.annotations)

                createBindingDeclaration(
                    bindingType,
                    null,
                    false,
                    bindingFunction.hasAnnotation(InjektFqNames.AstScoped)
                )
            }

        return declarations
    }

    private fun createBindingDeclaration(
        bindingType: IrType,
        singleArgument: IrExpression?,
        instance: Boolean,
        scoped: Boolean
    ): BindingDeclaration {
        val initializer: IrExpression?
        val path: Path

        if (instance) {
            path = PropertyPath(
                InjektDeclarationIrBuilder(pluginContext, moduleClass.symbol)
                    .fieldBackedProperty(
                        moduleClass,
                        nameProvider.allocateForType(bindingType),
                        bindingType
                    )
            )
            initializer = singleArgument!!
        } else if (singleArgument != null) {
            initializer = singleArgument
            path = PropertyPath(
                InjektDeclarationIrBuilder(pluginContext, moduleClass.symbol)
                    .fieldBackedProperty(
                        moduleClass,
                        nameProvider.allocateForType(bindingType),
                        initializer.type
                            .remapTypeParameters(originalModuleFunction, moduleFunction)
                            .remapTypeParameters(moduleFunction, moduleClass)
                    )
            )
        } else {
            if (bindingType.isTypeParameter()) {
                path = TypeParameterPath(bindingType.classifierOrFail.owner as IrTypeParameter)
                initializer = null
            } else {
                val clazz = bindingType.classOrNull!!.owner
                val providerExpression =
                    InjektDeclarationIrBuilder(pluginContext, moduleFunction.symbol)
                        .classFactoryLambda(clazz)
                initializer = providerExpression
                path = PropertyPath(
                    InjektDeclarationIrBuilder(pluginContext, moduleClass.symbol)
                        .fieldBackedProperty(
                            moduleClass,
                            nameProvider.allocateForType(bindingType),
                            initializer.type
                                .remapTypeParameters(originalModuleFunction, moduleFunction)
                                .remapTypeParameters(moduleFunction, moduleClass)
                        )
                )
            }
        }

        return BindingDeclaration(
            bindingType = bindingType,
            scoped = scoped,
            instance = instance,
            path = path,
            initializer = initializer
        )
    }

}
