package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.MapKey
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

typealias InitializerAccessor = IrBuilderWithScope.(() -> IrExpression) -> IrExpression

fun InitializerAccessor.child(
    child: InitializerAccessor
): InitializerAccessor = {
    child {
        invoke(this, it)
    }
}

fun InitializerAccessor.child(
    field: IrField
): InitializerAccessor = child { irGetField(it(), field) }

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
    val dependencies: List<DependencyRequest>,
    val targetScope: FqName?,
    val scoped: Boolean,
    val module: ModuleNode?
) : Node

data class DependencyRequest(
    val key: Key,
    val requestType: RequestType = when (key.type.classOrNull?.descriptor?.fqNameSafe) {
        InjektFqNames.Provider -> RequestType.Provider
        else -> RequestType.Instance
    }
) {
    fun asBindingRequest() = BindingRequest(key, requestType)
}

class DelegateBindingNode(
    key: Key,
    val originalKey: Key,
) : BindingNode(key, listOf(DependencyRequest(originalKey)), null, false, null)

class FactoryImplementationBindingNode(
    val factoryImplementationNode: FactoryImplementationNode
) : BindingNode(factoryImplementationNode.key, emptyList(), null, false, null)

class InstanceBindingNode(
    key: Key,
    val requirementNode: InstanceNode
) : BindingNode(key, emptyList(), null, false, null)

class DependencyBindingNode(
    key: Key,
    val provider: IrClass,
    val requirementNode: DependencyNode
) : BindingNode(key, emptyList(), null, false, null)

class LazyBindingNode(key: Key) : BindingNode(
    key,
    listOf(DependencyRequest(key = key.unwrapSingleArgKey())),
    null,
    false,
    null
)

class MapBindingNode(
    key: Key,
    val entries: Map<MapKey, DependencyRequest>
) : BindingNode(key, entries.values.toList(), null, false, null) {
    val keyKey = Key(
        (key.type as IrSimpleType)
            .arguments[0]
            .typeOrNull!!
    )
    val valueKey = Key(
        (key.type as IrSimpleType)
            .arguments[1]
            .typeOrNull!!
    )
}

class ProviderBindingNode(key: Key) : BindingNode(
    key,
    listOf(DependencyRequest(key = key.unwrapSingleArgKey())),
    null,
    false,
    null
)

class ProvisionBindingNode(
    key: Key,
    dependencies: List<DependencyRequest>,
    targetScope: FqName?,
    scoped: Boolean,
    module: ModuleNode?,
    val provider: IrClass
) : BindingNode(key, dependencies, targetScope, scoped, module)

class SetBindingNode(
    key: Key,
    val elements: List<DependencyRequest>
) : BindingNode(key, elements, null, false, null) {
    val elementKey = Key(
        (key.type as IrSimpleType)
            .arguments
            .single()
            .typeOrNull!!
    )
}

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

fun Key.unwrapSingleArgKey() = Key(
    type.let { it as IrSimpleType }.arguments.single().typeOrNull!!
        .let { it as IrSimpleType }
        .buildSimpleType {
            annotations += type.annotations
        },
)
