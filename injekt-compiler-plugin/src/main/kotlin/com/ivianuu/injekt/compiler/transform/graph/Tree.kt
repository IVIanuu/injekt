package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

typealias InitializerAccessor = IrBuilderWithScope.(IrExpression) -> IrExpression

fun InitializerAccessor.child(
    initializerAccessor: InitializerAccessor
): InitializerAccessor = { initializerAccessor(invoke(this, it)) }

fun InitializerAccessor.child(
    field: IrField
): InitializerAccessor = child { irGetField(it, field) }

interface Node

interface RequirementNode : Node {
    val key: Key
    val prefix: String
    val initializerAccessor: InitializerAccessor
}

class InstanceNode(
    override val key: Key,
    override val initializerAccessor: InitializerAccessor
) : RequirementNode {
    override val prefix: String
        get() = "instance"
}

class ModuleNode(
    val module: IrClass,
    override val key: Key,
    override val initializerAccessor: InitializerAccessor
) : RequirementNode {
    override val prefix: String
        get() = "module"
}

class FactoryImplementationNode(
    val factoryImplementation: IrClass,
    override val key: Key,
    override val initializerAccessor: InitializerAccessor
) : RequirementNode {
    override val prefix: String
        get() = "component"
}

class DependencyNode(
    val dependency: IrClass,
    override val key: Key,
    override val initializerAccessor: InitializerAccessor
) : RequirementNode {
    override val prefix: String
        get() = "dependency"
}

data class BindingRequest(
    val key: Key,
    val requestType: RequestType
)

enum class RequestType {
    Instance,
    Provider
}

sealed class BindingNode(
    val key: Key,
    val dependencies: List<Key>,
    val targetScope: FqName?,
    val scoped: Boolean,
    val module: ModuleNode?
) : Node

class DelegateBindingNode(
    key: Key,
    val originalKey: Key,
) : BindingNode(key, listOf(originalKey), null, false, null)

class InstanceBindingNode(
    key: Key,
    val requirementNode: InstanceNode
) : BindingNode(key, emptyList(), null, false, null)

class DependencyBindingNode(
    key: Key,
    val provider: IrClass,
    val requirementNode: DependencyNode
) : BindingNode(key, emptyList(), null, false, null)

class ProvisionBindingNode(
    key: Key,
    dependencies: List<Key>,
    targetScope: FqName?,
    scoped: Boolean,
    module: ModuleNode?,
    val provider: IrClass
) : BindingNode(key, dependencies, targetScope, scoped, module)

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
