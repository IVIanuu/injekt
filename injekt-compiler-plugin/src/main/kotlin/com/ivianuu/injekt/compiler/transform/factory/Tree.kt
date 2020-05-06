package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.MapKey
import com.ivianuu.injekt.compiler.ensureQualifiers
import com.ivianuu.injekt.compiler.equalsWithQualifiers
import com.ivianuu.injekt.compiler.getQualifiers
import com.ivianuu.injekt.compiler.hashCodeWithQualifiers
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName

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
    val factoryImplementation: FactoryImplementation,
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
    val requestType: RequestType = key.inferRequestType()
)

fun Key.inferRequestType() = when {
    type.isFunction() && type.typeArguments.size == 1 && InjektFqNames.Provider in type.getQualifiers() -> RequestType.Provider
    else -> RequestType.Instance
}

enum class RequestType {
    Instance,
    Provider
}

data class DependencyRequest(
    val key: Key,
    val requestType: RequestType = key.inferRequestType()
) {
    fun asBindingRequest() =
        BindingRequest(
            key,
            requestType
        )
}

sealed class BindingNode(
    val key: Key,
    val dependencies: List<DependencyRequest>,
    val targetScope: FqName?,
    val scoped: Boolean,
    val module: ModuleNode?,
    val owner: FactoryImplementation
) : Node

class AssistedProvisionBindingNode(
    key: Key,
    dependencies: List<DependencyRequest>,
    targetScope: FqName?,
    scoped: Boolean,
    module: ModuleNode?,
    owner: FactoryImplementation,
    val provider: IrClass
) : BindingNode(key, dependencies, targetScope, scoped, module, owner)

class ChildFactoryBindingNode(
    key: Key,
    owner: FactoryImplementation,
    val childFactoryImplementation: FactoryImplementation,
    val childFactory: IrClass
) : BindingNode(
    key, listOf(
        DependencyRequest(
            childFactoryImplementation.parent!!.clazz.defaultType
                .asKey()
        )
    ),
    null, false, null, owner
)

class DelegateBindingNode(
    key: Key,
    owner: FactoryImplementation,
    val originalKey: Key,
) : BindingNode(
    key, listOf(
        DependencyRequest(originalKey)
    ), null, false, null, owner
)

class DependencyBindingNode(
    key: Key,
    owner: FactoryImplementation,
    val provider: IrClass,
    val requirementNode: DependencyNode
) : BindingNode(key, emptyList(), null, false, null, owner)

class FactoryImplementationBindingNode(
    val factoryImplementationNode: FactoryImplementationNode,
) : BindingNode(
    factoryImplementationNode.key,
    emptyList(),
    null,
    false,
    null,
    factoryImplementationNode.factoryImplementation
)

class InstanceBindingNode(
    key: Key,
    owner: FactoryImplementation,
    val requirementNode: InstanceNode,
) : BindingNode(key, emptyList(), null, false, null, owner)

class LazyBindingNode(
    key: Key,
    owner: FactoryImplementation
) : BindingNode(
    key,
    listOf(
        DependencyRequest(
            key = Key(
                key.type.typeArguments.single()
                    .ensureQualifiers(owner.symbols)
            )
        )
    ),
    null,
    false,
    null,
    owner
)

class MapBindingNode(
    key: Key,
    owner: FactoryImplementation,
    val entries: Map<MapKey, DependencyRequest>
) : BindingNode(key, entries.values.toList(), null, false, null, owner) {
    val keyKey =
        Key(key.type.typeArguments[0])
    val valueKey =
        Key(key.type.typeArguments[1])
}

class MembersInjectorBindingNode(
    key: Key,
    owner: FactoryImplementation,
    val membersInjector: IrClass
) : BindingNode(
    key,
    membersInjector.constructors.single()
        .valueParameters
        .map { DependencyRequest(it.type.ensureQualifiers(owner.symbols).asKey()) },
    null,
    false,
    null,
    owner
)

class ProviderBindingNode(
    key: Key,
    owner: FactoryImplementation
) : BindingNode(
    key,
    listOf(
        DependencyRequest(
            key = Key(
                key.type.typeArguments.single()
                    .ensureQualifiers(owner.symbols)
            )
        )
    ),
    null,
    false,
    null,
    owner
)

class ProvisionBindingNode(
    key: Key,
    dependencies: List<DependencyRequest>,
    targetScope: FqName?,
    scoped: Boolean,
    module: ModuleNode?,
    owner: FactoryImplementation,
    val provider: IrClass
) : BindingNode(key, dependencies, targetScope, scoped, module, owner)

class SetBindingNode(
    key: Key,
    owner: FactoryImplementation,
    val elements: List<DependencyRequest>
) : BindingNode(key, elements, null, false, null, owner) {
    val elementKey =
        Key(key.type.typeArguments.single())
}

fun IrType.asKey() = Key(this)

class Key(val type: IrType) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Key

        if (!type.equalsWithQualifiers(other.type)) return false

        return true
    }

    override fun hashCode(): Int = type.hashCodeWithQualifiers()

    override fun toString(): String = type.render()

}