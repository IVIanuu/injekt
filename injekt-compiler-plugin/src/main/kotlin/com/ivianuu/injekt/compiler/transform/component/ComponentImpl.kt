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

package com.ivianuu.injekt.compiler.transform.component

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentImpl(
    val factoryImpl: ComponentFactoryImpl
) {
    val origin: FqName? = null // todo

    val clazz = buildClass {
        name = Name.special("<impl>")
        visibility = Visibilities.LOCAL
    }.apply clazz@{
        parent = factoryImpl.factoryClass
        createImplicitParameterDeclarationWithWrappedDescriptor()
        superTypes += factoryImpl.node.component.defaultType
        superTypes += factoryImpl.node.entryPoints.map { it.entryPoint.defaultType }
    }

    private val dependencyRequests =
        mutableMapOf<IrFunction, BindingRequest>()
    private val implementedRequests = mutableMapOf<IrFunction, IrFunction>()

    private val componentMembers = ComponentMembers(this, factoryImpl.pluginContext)

    private lateinit var graph: Graph
    private lateinit var componentExpressions: ComponentExpressions

    fun getImplExpression(): IrExpression {
        return DeclarationIrBuilder(
            factoryImpl.pluginContext,
            factoryImpl.factoryClass.symbol
        ).run {
            irBlock {
                graph = Graph(
                    parent = factoryImpl.parent?.componentImpl?.graph,
                    component = this@ComponentImpl,
                    context = factoryImpl.pluginContext,
                    declarationGraph = factoryImpl.declarationGraph,
                    symbols = factoryImpl.symbols
                )

                componentExpressions = ComponentExpressions(
                    graph = graph,
                    pluginContext = factoryImpl.pluginContext,
                    symbols = factoryImpl.symbols,
                    members = componentMembers,
                    parent = factoryImpl.parent?.componentImpl?.componentExpressions,
                    component = this@ComponentImpl
                )

                val constructor = clazz.addConstructor {
                    returnType = clazz.defaultType
                    isPrimary = true
                    visibility = Visibilities.PUBLIC
                }.apply {
                    body = DeclarationIrBuilder(factoryImpl.pluginContext, symbol).irBlockBody {
                        componentMembers.blockBuilder = this
                        val superType = clazz.superTypes.first()
                        +irDelegatingConstructorCall(
                            if (superType.classOrNull!!.owner.kind == ClassKind.CLASS)
                                superType.classOrNull!!.owner.constructors.single { it.valueParameters.isEmpty() }
                            else context.irBuiltIns.anyClass.constructors.single().owner
                        )
                        +IrInstanceInitializerCallImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            clazz.symbol,
                            context.irBuiltIns.unitType
                        )
                    }
                }

                collectDependencyRequests()

                dependencyRequests.forEach { graph.validate(it.value) }

                implementDependencyRequests()

                +clazz
                +irCall(constructor)
            }
        }
    }

    private fun implementDependencyRequests(): Unit = clazz.run clazz@{
        dependencyRequests.forEach { componentExpressions.getBindingExpression(it.value) }

        dependencyRequests
            .filter { it.key !in implementedRequests }
            .forEach { (declaration, request) ->
                val binding = graph.getBinding(request)
                implementedRequests[declaration] =
                    addDependencyRequestImplementation(declaration) { function ->
                        val bindingExpression = componentExpressions.getBindingExpression(
                            BindingRequest(
                                binding.key,
                                requestingKey = null,
                                request.requestOrigin
                            )
                        )

                        bindingExpression(ComponentExpressionContext(this@ComponentImpl) {
                            irGet(function.dispatchReceiverParameter!!)
                        })
                    }
            }

        val implementedSuperTypes = mutableSetOf<IrType>()

        while (true) {
            val contexts = graph.resolvedBindings.values
                .mapNotNull { it.context }
                .filter { it.defaultType !in implementedSuperTypes }

            if (contexts.isEmpty()) {
                break
            }

            fun implementFunctions(
                superClass: IrClass,
                typeArguments: List<IrType>
            ) {
                if (superClass.defaultType in implementedSuperTypes) return
                implementedSuperTypes += superClass.defaultType
                for (declaration in superClass.declarations.toList()) {
                    if (declaration !is IrFunction) continue
                    if (declaration is IrConstructor) continue
                    if (declaration.isFakeOverride) continue
                    if (declaration.dispatchReceiverParameter?.type == factoryImpl.pluginContext.irBuiltIns.anyType) break
                    addDependencyRequestImplementation(declaration) { function ->
                        val bindingExpression = componentExpressions.getBindingExpression(
                            BindingRequest(
                                function.returnType
                                    .substitute(
                                        superClass.typeParameters
                                            .map { it.symbol }
                                            .zip(typeArguments)
                                            .toMap()
                                    )
                                    .asKey(),
                                null,
                                superClass.getAnnotation(InjektFqNames.Name)
                                    ?.getValueArgument(0)
                                    ?.let { it as IrConst<String> }
                                    ?.value
                                    ?.let { FqName(it) }
                            )
                        )

                        bindingExpression(ComponentExpressionContext(this@ComponentImpl) {
                            irGet(function.dispatchReceiverParameter!!)
                        })
                    }
                }

                superClass.superTypes
                    .map { it to it.classOrNull?.owner }
                    .forEach { (superType, clazz) ->
                        if (clazz != null)
                            implementFunctions(
                                clazz,
                                superType.typeArguments.map { it.typeOrFail }
                            )
                    }
            }

            contexts.forEach { context ->
                clazz.superTypes += context.defaultType
                implementFunctions(
                    context,
                    context.defaultType.typeArguments.map { it.typeOrFail }
                )
            }
        }
    }

    private fun collectDependencyRequests() {
        fun IrClass.collectDependencyRequests(typeArguments: List<IrType>) {
            for (declaration in declarations.filterIsInstance<IrFunction>()) {
                fun reqisterRequest(type: IrType) {
                    dependencyRequests[declaration] =
                        BindingRequest(
                            type
                                .substitute(
                                    typeParameters.map { it.symbol }.associateWith {
                                        typeArguments[it.owner.index]
                                    }
                                )
                                .asKey(),
                            requestingKey = null,
                            declaration.descriptor.fqNameSafe
                        )
                }

                if (declaration !is IrConstructor &&
                    declaration.dispatchReceiverParameter?.type != factoryImpl.pluginContext.irBuiltIns.anyType &&
                    !declaration.isFakeOverride
                ) reqisterRequest(declaration.returnType)
            }

            superTypes
                .map { it to it.classOrNull?.owner }
                .forEach { (superType, clazz) ->
                    clazz?.collectDependencyRequests(
                        superType.typeArguments.map { it.typeOrFail }
                    )
                }
        }

        factoryImpl.node.entryPoints.forEach {
            it.entryPoint.collectDependencyRequests(emptyList())
        }
    }

    private fun addDependencyRequestImplementation(
        declaration: IrFunction,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ) = clazz.addFunction {
        name = declaration.name
        returnType = declaration.returnType
        visibility = declaration.visibility
    }.apply function@{
        overriddenSymbols += declaration.symbol as IrSimpleFunctionSymbol
        dispatchReceiverParameter = clazz.thisReceiver!!.copyTo(this)
        this.body = DeclarationIrBuilder(factoryImpl.pluginContext, symbol).run {
            irExprBody(body(this, this@function))
        }
    }

}
