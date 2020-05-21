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

package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.NameProvider
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irTemporaryVar
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.name.Name

interface FactoryMembers {

    val membersNameProvider: NameProvider

    fun addClass(clazz: IrClass)

    fun cachedValue(
        key: Key,
        initializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression
    ): FactoryExpression

    fun getGetFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction
}

class ClassFactoryMembers(
    private val pluginContext: IrPluginContext,
    private val clazz: IrClass,
    private val implFactory: ImplFactory
) : FactoryMembers {

    override val membersNameProvider = NameProvider()

    data class FieldWithInitializer(
        val field: IrField,
        val initializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression
    )

    val fields =
        mutableMapOf<Key, FieldWithInitializer>()

    private val getFunctions = mutableListOf<IrFunction>()
    private val getFunctionsNameProvider = NameProvider()

    private val functionsByKey = mutableMapOf<Key, IrFunction>()

    override fun addClass(clazz: IrClass) {
        this.clazz.addChild(clazz)
    }

    override fun cachedValue(
        key: Key,
        initializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression
    ): FactoryExpression {
        val fieldWithInitializer = fields.getOrPut(key) {
            FieldWithInitializer(
                clazz.addField(
                    membersNameProvider.allocateForType(key.type),
                    key.type
                ), initializer
            )
        }
        return { irGetField(it[implFactory], fieldWithInitializer.field) }
    }

    override fun getGetFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction {
        functionsByKey[key]?.let { return it }

        val matchingDependencyRequestFunction = implFactory.dependencyRequests
            .mapNotNull { (declaration, request) ->
                when (declaration) {
                    is IrFunction -> {
                        if (request.key == key) declaration else null
                    }
                    is IrProperty -> {
                        if (request.key == key) {
                            declaration
                        } else {
                            null
                        }
                    }
                    else -> error("Unexpected dependency request ${declaration.dump()}")
                }
            }
            .singleOrNull()

        val function = if (matchingDependencyRequestFunction != null) {
            addDependencyRequestImplementation(matchingDependencyRequestFunction, body)
                .let { if (it is IrFunction) it else (it as IrProperty).getter!! }
                .also {
                    implFactory.implementedRequests[matchingDependencyRequestFunction] =
                        it
                }
        } else {
            clazz.addFunction {
                this.name = Name.identifier(
                    getFunctionsNameProvider.allocate(
                        "get${key.type.classifierOrFail.descriptor.name.asString()}"
                    )
                )
                returnType = key.type
                visibility = Visibilities.PRIVATE
            }.apply {
                dispatchReceiverParameter = clazz.thisReceiver!!.copyTo(this)
                this.body = DeclarationIrBuilder(pluginContext, symbol).run {
                    irExprBody(body(this, this@apply))
                }
            }.also { getFunctions += it }
        }

        functionsByKey[key] = function

        return function
    }

    fun addDependencyRequestImplementation(
        declaration: IrDeclaration,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrDeclaration {
        fun IrFunctionImpl.implement(symbol: IrSimpleFunctionSymbol) {
            overriddenSymbols += symbol
            dispatchReceiverParameter = clazz.thisReceiver!!.copyTo(this)
            DeclarationIrBuilder(pluginContext, symbol).apply {
                this@implement.body = irExprBody(body(this, this@implement))
            }
        }

        return when (declaration) {
            is IrFunction -> {
                clazz.addFunction {
                    name = declaration.name
                    returnType = declaration.returnType
                    visibility = declaration.visibility
                }.apply { implement(declaration.symbol as IrSimpleFunctionSymbol) }
            }
            is IrProperty -> {
                clazz.addProperty {
                    name = declaration.name
                    visibility = declaration.visibility
                }.apply {
                    addGetter {
                        returnType = declaration.getter!!.returnType
                    }.apply { (this as IrFunctionImpl).implement(declaration.getter!!.symbol) }
                }
            }
            else -> error("Unexpected declaration ${declaration.dump()}")
        }
    }

}

class FunctionFactoryMembers(
    private val pluginContext: IrPluginContext,
    private val declarationContainer: IrDeclarationContainer
) : FactoryMembers {

    lateinit var blockBuilder: IrBlockBuilder

    val getFunctions = mutableListOf<IrFunction>()
    private val getFunctionsNameProvider = NameProvider()

    override val membersNameProvider = NameProvider()

    override fun addClass(clazz: IrClass) {
        clazz.parent = declarationContainer
        declarationContainer.addChild(clazz)
    }

    override fun cachedValue(
        key: Key,
        initializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression
    ): FactoryExpression {
        val tmp = blockBuilder.irTemporaryVar(
            value = initializer(blockBuilder, EmptyFactoryExpressionContext),
            nameHint = membersNameProvider.allocateForType(key.type).asString()
        )
        return { irGet(tmp) }
    }

    override fun getGetFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction {
        return buildFun {
            this.name = Name.identifier(
                getFunctionsNameProvider.allocate(
                    "get${key.type.classifierOrFail.descriptor.name.asString()}"
                )
            )
            returnType = key.type
            visibility = Visibilities.LOCAL
        }.apply {
            metadata = MetadataSource.Function(descriptor)

            with(blockBuilder) {
                +this@apply
                parent = scope.getLocalDeclarationParent()
            }
            this.body = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(body(this, this@apply))
            }
        }.also { getFunctions += it }
    }

}
