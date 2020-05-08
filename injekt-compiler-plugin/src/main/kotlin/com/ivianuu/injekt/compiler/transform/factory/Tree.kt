package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.MapKey
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.equalsWithQualifiers
import com.ivianuu.injekt.compiler.getQualifierFqNames
import com.ivianuu.injekt.compiler.hashCodeWithQualifiers
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isFunction
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
    accessor: IrFunction
): InitializerAccessor = child {
    irCall(accessor).apply {
        dispatchReceiver = it()
    }
}

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
    override val initializerAccessor: InitializerAccessor,
    val typeParametersMap: Map<IrTypeParameterSymbol, IrType>
) : RequirementNode {
    override val prefix: String
        get() = "module"

    init {
        typeParametersMap.forEach {
            check(it.value !is IrTypeParameter) {
                "Must be concrete type ${it.key.owner.dump()} -> ${it.value}"
            }
        }
    }
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
    val requestOrigin: FqName?,
    val requestType: RequestType = key.inferRequestType()
)

fun Key.inferRequestType() = when {
    type.isFunction() && type.typeArguments.size == 1 && InjektFqNames.Provider in type.getQualifierFqNames() -> RequestType.Provider
    else -> RequestType.Instance
}

enum class RequestType {
    Instance,
    Provider
}

sealed class BindingNode(
    val key: Key,
    val dependencies: List<BindingRequest>,
    val targetScope: FqName?,
    val scoped: Boolean,
    val module: ModuleNode?,
    val owner: AbstractFactoryProduct,
    val origin: FqName?
) : Node

class AssistedProvisionBindingNode(
    key: Key,
    dependencies: List<BindingRequest>,
    targetScope: FqName?,
    scoped: Boolean,
    module: ModuleNode?,
    owner: AbstractFactoryProduct,
    origin: FqName?,
    val provider: IrClass
) : BindingNode(key, dependencies, targetScope, scoped, module, owner, origin)

class ChildFactoryBindingNode(
    key: Key,
    owner: FactoryImplementation,
    origin: FqName?,
    val childFactoryImplementation: FactoryImplementation,
    val childFactory: IrClass
) : BindingNode(
    key, listOf(
        BindingRequest(
            childFactoryImplementation.parent!!.clazz.defaultType
                .asKey(owner.pluginContext),
            null
        )
    ),
    null, false, null, owner, origin
)

class DelegateBindingNode(
    key: Key,
    owner: AbstractFactoryProduct,
    origin: FqName?,
    val originalKey: Key,
    val requestOrigin: FqName
) : BindingNode(
    key, listOf(
        BindingRequest(originalKey, requestOrigin)
    ), null, false, null, owner, origin
)

class DependencyBindingNode(
    key: Key,
    owner: AbstractFactoryProduct,
    origin: FqName?,
    val provider: IrClass,
    val requirementNode: DependencyNode
) : BindingNode(key, emptyList(), null, false, null, owner, origin)

class FactoryImplementationBindingNode(
    val factoryImplementationNode: FactoryImplementationNode,
) : BindingNode(
    factoryImplementationNode.key,
    emptyList(),
    null,
    false,
    null,
    factoryImplementationNode.factoryImplementation,
    factoryImplementationNode.key.type.classOrFail.descriptor.fqNameSafe
)

class InstanceBindingNode(
    key: Key,
    owner: AbstractFactoryProduct,
    origin: FqName?,
    val requirementNode: InstanceNode,
) : BindingNode(key, emptyList(), null, false, null, owner, origin)

class LazyBindingNode(
    key: Key,
    origin: FqName?,
    owner: AbstractFactoryProduct
) : BindingNode(
    key,
    listOf(
        BindingRequest(
            key = key.type.typeArguments.single().asKey(owner.pluginContext),
            origin
        )
    ),
    null,
    false,
    null,
    owner,
    origin
)

class MapBindingNode(
    key: Key,
    owner: AbstractFactoryProduct,
    origin: FqName?,
    val entries: Map<MapKey, BindingRequest>
) : BindingNode(key, entries.values.toList(), null, false, null, owner, origin) {
    val keyKey = key.type.typeArguments[0].asKey(owner.pluginContext)
    val valueKey = key.type.typeArguments[1].asKey(owner.pluginContext)
}

class MembersInjectorBindingNode(
    key: Key,
    owner: AbstractFactoryProduct,
    origin: FqName?,
    val membersInjector: IrClass
) : BindingNode(
    key,
    membersInjector.constructors.single()
        .valueParameters
        .map { BindingRequest(it.type.asKey(owner.pluginContext), null) },
    null,
    false,
    null,
    owner,
    origin
)

class ProviderBindingNode(
    key: Key,
    owner: AbstractFactoryProduct,
    origin: FqName?
) : BindingNode(
    key,
    listOf(BindingRequest(key.type.typeArguments.single().asKey(owner.pluginContext), origin)),
    null,
    false,
    null,
    owner,
    origin
)

class ProvisionBindingNode(
    key: Key,
    dependencies: List<BindingRequest>,
    targetScope: FqName?,
    scoped: Boolean,
    module: ModuleNode?,
    owner: AbstractFactoryProduct,
    origin: FqName?,
    val provider: IrClass
) : BindingNode(key, dependencies, targetScope, scoped, module, owner, origin)

class SetBindingNode(
    key: Key,
    owner: AbstractFactoryProduct,
    origin: FqName?,
    val elements: List<BindingRequest>
) : BindingNode(key, elements, null, false, null, owner, origin) {
    val elementKey = key.type.typeArguments.single().asKey(owner.pluginContext)
}

fun IrType.asKey(context: IrPluginContext): Key {
    annotations.forEach { it.symbol.ensureBound(context.irProviders) }
    return Key(this)
}

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
