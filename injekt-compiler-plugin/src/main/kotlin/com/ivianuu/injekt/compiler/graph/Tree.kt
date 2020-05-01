package com.ivianuu.injekt.compiler.graph

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.render

typealias TreeElement = IrBuilderWithScope.(IrExpression) -> IrExpression

fun TreeElement.child(
    treeElement: TreeElement
): TreeElement = { treeElement(invoke(this, it)) }

fun TreeElement.child(
    field: IrField
): TreeElement = { irGetField(invoke(this, it), field) }

class ModuleNode(
    val module: IrClass,
    val treeElement: TreeElement
)

class ComponentNode(
    val component: IrClass,
    val treeElement: TreeElement
)

class Binding(
    val key: Key,
    val dependencies: List<Key>,
    val providerInstance: IrBuilderWithScope.(IrExpression) -> IrExpression,
    val getFunction: IrBuilderWithScope.() -> IrFunction,
    val field: IrField?
)

data class Key(val type: IrType) {
    override fun toString(): String = "Key(type=${type.render()})"
}
