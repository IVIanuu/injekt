package com.ivianuu.injekt.compiler.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

typealias TreeElement = IrBuilderWithScope.(IrExpression) -> IrExpression

fun TreeElement.child(
    treeElement: TreeElement
): TreeElement = { treeElement(invoke(this, it)) }

fun TreeElement.child(
    field: IrField
): TreeElement = child { irGetField(it, field) }

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
    val targetScope: FqName?,
    val providerInstance: IrBuilderWithScope.(IrExpression) -> IrExpression,
    val getFunction: IrBuilderWithScope.() -> IrFunction,
    val providerField: () -> IrField?
)

class Key(val type: IrType) {
    val qualifiers = type.annotations
        .filter {
            it.type.classOrNull!!
                .descriptor
                .annotations
                .hasAnnotation(InjektFqNames.Qualifier)
        }
        .map { it.type.classifierOrFail.descriptor.fqNameSafe }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Key

        if (type != other.type) return false
        if (qualifiers != other.qualifiers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + qualifiers.hashCode()
        return result
    }

    override fun toString(): String = "Key(type=${type.render()})"

}
