package com.ivianuu.injekt.compiler.transform.factory

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.name.Name

interface FactoryMembers {

    val fields: Map<Key, FactoryField>
    val initializedFields: Set<Key>

    fun nameForGroup(prefix: String): Name

    fun addClass(clazz: IrClass)

    fun getOrCreateField(
        key: Key,
        prefix: String,
        initializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression?
    ): FactoryField

    fun getGetFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction
}

class ClassFactoryMembers(
    private val pluginContext: IrPluginContext,
    private val clazz: IrClass,
    private val factoryImplementation: FactoryImplementation
) : FactoryMembers {

    override val fields = mutableMapOf<Key, FactoryField>()
    override val initializedFields = mutableSetOf<Key>()

    private val getFunctions = mutableListOf<IrFunction>()

    private val groupNames = mutableMapOf<String, Int>()

    private val functionsByKey = mutableMapOf<Key, IrFunction>()

    override fun nameForGroup(prefix: String): Name {
        val index = groupNames.getOrPut(prefix) { 0 }
        val name = Name.identifier("${prefix}_$index")
        groupNames[prefix] = index + 1
        return name
    }

    override fun addClass(clazz: IrClass) {
        this.clazz.addChild(clazz)
    }

    override fun getOrCreateField(
        key: Key,
        prefix: String,
        initializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression?
    ): FactoryField {
        return fields.getOrPut(key) {
            val field = clazz.addField(
                nameForGroup(prefix),
                key.type
            )
            FactoryField(clazz, initializer, field).also {
                it.initialize {
                    irGetField(
                        it[factoryImplementation],
                        field
                    )
                }
            }
        }
    }

    override fun getGetFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction {
        functionsByKey[key]?.let { return it }

        val matchingDependencyRequestFunction = factoryImplementation.dependencyRequests
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
                    factoryImplementation.implementedRequests[matchingDependencyRequestFunction] =
                        it
                }
        } else {
            clazz.addFunction {
                val currentGetFunctionIndex = getFunctions.size
                this.name = Name.identifier("get_$currentGetFunctionIndex")
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
    private val function: IrFunction
) : FactoryMembers {
    override val fields = mutableMapOf<Key, FactoryField>()
    override val initializedFields = mutableSetOf<Key>()

    val getFunctions = mutableListOf<IrFunction>()

    private val groupNames = mutableMapOf<String, Int>()

    override fun nameForGroup(prefix: String): Name {
        val index = groupNames.getOrPut(prefix) { 0 }
        val name = Name.identifier("${prefix}_$index")
        groupNames[prefix] = index + 1
        return name
    }

    override fun addClass(clazz: IrClass) {
        throw UnsupportedOperationException("Cannot add classes")
    }

    override fun getOrCreateField(
        key: Key,
        prefix: String,
        initializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression?
    ): FactoryField {
        return fields.getOrPut(key) {
            FactoryField(function, initializer, null)
        }
    }

    override fun getGetFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction {
        return buildFun {
            val currentGetFunctionIndex = getFunctions.size
            this.name = Name.identifier("get_$currentGetFunctionIndex")
            returnType = key.type
            visibility = Visibilities.LOCAL
        }.apply {
            this.body = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(body(this, this@apply))
            }
        }.also { getFunctions += it }
    }

}

class FactoryField(
    val owner: IrElement,
    val initializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression?,
    val backingField: IrField?
) {
    private var _accessor: (IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression)? = null
    val accessor: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression =
        { _accessor!!(this, it) }

    fun initialize(accessor: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression) {
        _accessor = accessor
    }
}
