package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
    val context: IrPluginContext,
    val component: IrClass,
    val treeElement: TreeElement,
    val symbols: InjektSymbols
) {

    class ComponentField(
        val field: IrField,
        val initializer: IrBuilderWithScope.(IrExpression) -> IrExpression?
    )

    val componentFields = mutableMapOf<Key, ComponentField>()
    val initializedFields = mutableSetOf<Key>()

    private val getFunctions = mutableListOf<IrFunction>()

    fun getOrCreateComponentField(
        key: Key,
        initializer: IrBuilderWithScope.(IrExpression) -> IrExpression?
    ): ComponentField {
        return componentFields.getOrPut(key) {
            val currentProviderIndex = componentFields.size
            val field = component.addField(
                Name.identifier("provider_$currentProviderIndex"),
                symbols.provider.owner.typeWith(key.type)
            )
            ComponentField(
                field,
                initializer
            )
        }
    }

    fun getFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction {
        return component.addFunction {
            val currentGetFunctionIndex = getFunctions.size
            this.name = Name.identifier("get_$currentGetFunctionIndex")
            returnType = key.type
        }.apply {
            dispatchReceiverParameter = component.thisReceiver!!.copyTo(this)
            this.body = DeclarationIrBuilder(context, symbol).run {
                irExprBody(body(this, this@apply))
            }
        }.also { getFunctions += it }
    }

}

sealed class Binding(
    val key: Key,
    val dependencies: List<Key>,
    val targetScope: FqName?,
    val scoped: Boolean,
    val module: ModuleNode?
)

class InstanceBinding(
    key: Key,
    targetScope: FqName?,
    scoped: Boolean,
    module: ModuleNode?,
    val treeElement: TreeElement
) : Binding(key, emptyList(), targetScope, scoped, module)

class ProvisionBinding(
    key: Key,
    dependencies: List<Key>,
    targetScope: FqName?,
    scoped: Boolean,
    module: ModuleNode?,
    val provider: IrClass
) : Binding(key, dependencies, targetScope, scoped, module)

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
