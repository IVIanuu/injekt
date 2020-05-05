package com.ivianuu.injekt.compiler.transform.factory

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.name.Name

class FactoryMembers(
    private val factoryImplementationNode: FactoryImplementationNode,
    private val context: IrPluginContext
) {

    val fields = mutableMapOf<Key, FactoryField>()
    val initializedFields = mutableSetOf<Key>()

    private val getFunctions = mutableListOf<IrFunction>()

    private val groupNames = mutableMapOf<String, Int>()

    fun nameForGroup(prefix: String): Name {
        val index = groupNames.getOrPut(prefix) { 0 }
        val name = Name.identifier("${prefix}_$index")
        groupNames[prefix] = index + 1
        return name
    }

    fun addClass(clazz: IrClass) {
        factoryImplementationNode.factoryImplementation.clazz.addChild(clazz)
    }

    fun getOrCreateField(
        key: Key,
        prefix: String,
        initializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression?
    ): FactoryField {
        return fields.getOrPut(key) {
            val field = factoryImplementationNode.factoryImplementation.clazz.addField(
                nameForGroup(prefix),
                key.type
            )
            FactoryField(
                factoryImplementationNode.factoryImplementation.clazz,
                field,
                initializer
            )
        }
    }

    fun getGetFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction {
        return factoryImplementationNode.factoryImplementation.clazz.addFunction {
            val currentGetFunctionIndex = getFunctions.size
            this.name = Name.identifier("get_$currentGetFunctionIndex")
            returnType = key.type
        }.apply {
            dispatchReceiverParameter =
                factoryImplementationNode.factoryImplementation.clazz.thisReceiver!!.copyTo(this)
            this.body = DeclarationIrBuilder(context, symbol).run {
                irExprBody(body(this, this@apply))
            }
        }.also { getFunctions += it }
    }

}

class FactoryField(
    val owner: IrClass,
    val field: IrField,
    val initializer: IrBuilderWithScope.(FactoryExpressionContext) -> IrExpression?
)
