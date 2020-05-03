package com.ivianuu.injekt.compiler.transform.graph

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.name.Name

class FactoryMembers(
    private val componentNode: ComponentNode,
    private val context: IrPluginContext
) {

    val componentFields = mutableMapOf<Key, FactoryField>()
    val initializedFields = mutableSetOf<Key>()

    private val getFunctions = mutableListOf<IrFunction>()

    fun getOrCreateComponentField(
        key: Key,
        prefix: String,
        initializer: IrBuilderWithScope.(IrExpression) -> IrExpression?
    ): FactoryField {
        return componentFields.getOrPut(key) {
            val index = componentFields
                .filter { it.value.field.name.asString().startsWith(prefix) }
                .size
            val field = componentNode.component.addField(
                Name.identifier("${prefix}_$index"),
                key.type
            )
            FactoryField(
                field,
                initializer
            )
        }
    }

    fun getGetFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction {
        return componentNode.component.addFunction {
            val currentGetFunctionIndex = getFunctions.size
            this.name = Name.identifier("get_$currentGetFunctionIndex")
            returnType = key.type
        }.apply {
            dispatchReceiverParameter = componentNode.component.thisReceiver!!.copyTo(this)
            this.body = DeclarationIrBuilder(context, symbol).run {
                irExprBody(body(this, this@apply))
            }
        }.also { getFunctions += it }
    }

}

class FactoryField(
    val field: IrField,
    val initializer: IrBuilderWithScope.(IrExpression) -> IrExpression?
)
