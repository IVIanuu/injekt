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
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
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
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentImpl(val factoryImpl: ComponentFactoryImpl) {
    val origin: FqName? = factoryImpl.node.component.descriptor.fqNameSafe

    val clazz = buildClass {
        name = Name.special("<impl>")
        visibility = Visibilities.LOCAL
    }.apply clazz@{
        parent = factoryImpl.factoryClass
        createImplicitParameterDeclarationWithWrappedDescriptor()
        superTypes += factoryImpl.node.component.defaultType
        superTypes += factoryImpl.node.entryPoints.map { it.entryPoint.defaultType }
    }

    private val componentMembers = ComponentMembers(this, factoryImpl.pluginContext)

    private lateinit var graph: Graph
    private lateinit var componentExpressions: ComponentExpressions

    fun getImplExpression(inputParameters: List<IrValueParameter>): IrExpression {
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
                    symbols = factoryImpl.symbols,
                    inputParameters = inputParameters
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

                implementDependencyRequests()

                +clazz
                +irCall(constructor)
            }
        }
    }

    private fun implementDependencyRequests(): Unit = clazz.run clazz@{
        val implementedSuperTypes = mutableSetOf<IrType>()
        val declarationNames = mutableSetOf<Name>()
        var firstRound = true
        while (true) {
            val entryPoints = if (firstRound) {
                factoryImpl.node.entryPoints.map { it.entryPoint }
            } else {
                graph.resolvedBindings.values
                    .mapNotNull { it.context }
                    .filter { it.defaultType !in implementedSuperTypes }
            }

            if (entryPoints.isEmpty()) {
                break
            }

            fun implementFunctions(superClass: IrClass) {
                if (superClass.defaultType in implementedSuperTypes) return
                implementedSuperTypes += superClass.defaultType
                for (declaration in superClass.declarations.toList()) {
                    if (declaration !is IrFunction) continue
                    if (declaration is IrConstructor) continue
                    if (declaration.isFakeOverride) continue
                    if (declaration.name in declarationNames) continue
                    if (declaration.dispatchReceiverParameter?.type == factoryImpl.pluginContext.irBuiltIns.anyType) break
                    declarationNames += declaration.name

                    clazz.addFunction {
                        name = declaration.name
                        returnType = declaration.returnType
                        visibility = declaration.visibility
                    }.apply function@{
                        overriddenSymbols += declaration.symbol as IrSimpleFunctionSymbol
                        dispatchReceiverParameter = clazz.thisReceiver!!.copyTo(this)
                        this.body = DeclarationIrBuilder(factoryImpl.pluginContext, symbol).run {
                            val request = BindingRequest(
                                returnType.asKey(),
                                null,
                                superClass.getAnnotation(InjektFqNames.Name)
                                    ?.getValueArgument(0)
                                    ?.let { it as IrConst<String> }
                                    ?.value
                                    ?.let { FqName(it) }
                            )

                            graph.validate(request)

                            val expression =
                                componentExpressions.getBindingExpression(request)

                            irExprBody(
                                expression(ComponentExpressionContext(this@ComponentImpl) {
                                    irGet(dispatchReceiverParameter!!)
                                })
                            )
                        }
                    }
                }

                superClass.superTypes
                    .map { it.classOrNull!!.owner }
                    .forEach { implementFunctions(it) }

                if (firstRound) firstRound = false
            }

            entryPoints.forEach { context ->
                clazz.superTypes += context.defaultType
                implementFunctions(context)
            }
        }
    }

}
