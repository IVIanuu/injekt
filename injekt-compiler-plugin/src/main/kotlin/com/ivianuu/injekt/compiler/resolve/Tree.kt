package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.asTypeName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.name.FqName

sealed class TreeElement(
    val pathName: String,
    val type: IrClass,
    val parent: TreeElement?,
    val accessor: () -> IrExpression
) {
    val path = buildList {
        var node: TreeElement? = this@TreeElement
        while (node != null) {
            if (node.pathName.isNotEmpty()) add(node.pathName)
            node = node.parent
        }
    }.reversed().joinToString("/")

    init {
        check(parent == null || pathName.isNotEmpty()) {
            "Invalid path name for ${type.name} parent is ${parent?.type?.name}"
        }
        println("initialize tree element ${type.name} path name is '$pathName' path is '$path'")
    }
}

class RootTreeElement(
    context: IrPluginContext,
    type: IrClass
) : TreeElement("", type, null, {
    DeclarationIrBuilder(context, type.symbol).irGet(type.thisReceiver!!)
})

class FieldTreeElement(
    context: IrPluginContext,
    pathName: String,
    parent: TreeElement
) : TreeElement(
    pathName,
    parent.type.fields.single { it.name.asString() == pathName }.type.classOrNull!!.owner,
    parent,
    {
        val field = parent.type.fields.single { it.name.asString() == pathName }
        println("accessor for $pathName field ${field.name} ${field.dump()} parent ${parent.pathName}")
        DeclarationIrBuilder(context, field.symbol)
            .irGetField(parent.accessor(), field)
    }
)

class ModuleNode(
    val module: IrClass,
    val componentNode: ComponentNode,
    val typeParametersMap: Map<IrTypeParameterSymbol, IrType>,
    val treeElement: TreeElement?
) {
    init {
        typeParametersMap.forEach {
            check(it.value !is IrTypeParameter) {
                "Must be concrete type ${it.key.owner.dump()} -> ${it.value}"
            }
        }
    }
}

class ComponentNode(
    val key: String,
    val component: IrClass,
    val treeElement: TreeElement?
)

sealed class Binding(
    val key: Key,
    val dependencies: List<Key>,
    val provider: IrClass,
    val providerInstance: () -> IrExpression,
    val createInstance: () -> IrExpression,
    val sourceComponent: ComponentNode
)

class StatefulBinding(
    key: Key,
    dependencies: List<Key>,
    provider: IrClass,
    providerInstance: () -> IrExpression,
    createInstance: () -> IrExpression,
    sourceComponent: ComponentNode,
    val treeElement: TreeElement,
    val field: IrField
) : Binding(key, dependencies, provider, providerInstance, createInstance, sourceComponent)

class StatelessBinding(
    key: Key,
    dependencies: List<Key>,
    provider: IrClass,
    providerInstance: () -> IrExpression,
    createInstance: () -> IrExpression,
    sourceComponent: ComponentNode
) : Binding(key, dependencies, provider, providerInstance, createInstance, sourceComponent)

data class Key(
    val type: IrType,
    val qualifiers: List<FqName> = emptyList()
) {
    override fun toString(): String {
        return "Key(type=${type.toKotlinType().asTypeName()!!}, qualifiers=$qualifiers)"
    }
}
