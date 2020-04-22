package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.asTypeName
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.name.FqName

class TreeElement(
    val path: String,
    val accessor: IrBuilderWithScope.(IrExpression) -> IrExpression
) {
    fun child(
        pathName: String,
        accessor: IrBuilderWithScope.(IrExpression) -> IrExpression
    ) = TreeElement(
        path = if (path.isNotEmpty()) "$path/$pathName"
        else pathName
    ) {
        accessor(this@TreeElement.accessor(this, it))
    }

    fun childField(fieldName: String) = child(fieldName) {
        val owner = it.type.classOrNull!!.owner
        val field = owner.fields.single { it.name.asString() == fieldName }
        irGetField(it, field)
    }
}

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
    val duplicateStrategy: DuplicateStrategy,
    val providerInstance: IrBuilderWithScope.(IrExpression) -> IrExpression,
    val getFunction: IrBuilderWithScope.() -> IrFunction
)

class SpecialBinding(
    key: Key,
    dependencies: List<Key>,
    duplicateStrategy: DuplicateStrategy,
    providerInstance: IrBuilderWithScope.(IrExpression) -> IrExpression,
    getFunction: IrBuilderWithScope.() -> IrFunction
) : Binding(key, dependencies, duplicateStrategy, providerInstance, getFunction)

sealed class UserBinding(
    key: Key,
    dependencies: List<Key>,
    duplicateStrategy: DuplicateStrategy,
    providerInstance: IrBuilderWithScope.(IrExpression) -> IrExpression,
    getFunction: IrBuilderWithScope.() -> IrFunction,
    val sourceComponent: ComponentNode?,
    val provider: IrClass
) : Binding(key, dependencies, duplicateStrategy, providerInstance, getFunction)

class StatefulBinding(
    key: Key,
    dependencies: List<Key>,
    duplicateStrategy: DuplicateStrategy,
    providerInstance: IrBuilderWithScope.(IrExpression) -> IrExpression,
    getFunction: IrBuilderWithScope.() -> IrFunction,
    sourceComponent: ComponentNode,
    provider: IrClass,
    val treeElement: TreeElement,
    val field: IrField
) : UserBinding(
    key,
    dependencies,
    duplicateStrategy,
    providerInstance,
    getFunction,
    sourceComponent,
    provider
)

class StatelessBinding(
    key: Key,
    dependencies: List<Key>,
    duplicateStrategy: DuplicateStrategy,
    providerInstance: IrBuilderWithScope.(IrExpression) -> IrExpression,
    getFunction: IrBuilderWithScope.() -> IrFunction,
    sourceComponent: ComponentNode?,
    provider: IrClass
) : UserBinding(
    key,
    dependencies,
    duplicateStrategy,
    providerInstance,
    getFunction,
    sourceComponent,
    provider
)


data class Key(
    val type: IrType,
    val qualifiers: List<FqName> = emptyList()
) {
    override fun toString(): String {
        return "Key(type=${type.toKotlinType().asTypeName()!!}, qualifiers=$qualifiers)"
    }
}

enum class DuplicateStrategy {
    Drop, Fail, Override
}