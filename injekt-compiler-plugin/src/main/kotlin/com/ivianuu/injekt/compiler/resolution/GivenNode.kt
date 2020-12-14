package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

sealed class GivenNode {
    abstract val type: TypeRef
    abstract val dependencies: List<GivenRequest>
    abstract val callableFqName: FqName
    abstract val callContext: CallContext
    abstract val depth: Int
    abstract val providedGivens: List<GivenNode>
}

data class CallableGivenNode(
    override val type: TypeRef,
    override val dependencies: List<GivenRequest>,
    override val depth: Int,
    val callable: CallableDescriptor,
) : GivenNode() {
    override val callableFqName: FqName = if (callable is ClassConstructorDescriptor)
        callable.constructedClass.fqNameSafe
    else callable.fqNameSafe
    override val callContext: CallContext
        get() = callable.callContext
    override val providedGivens: List<GivenNode>
        get() = emptyList()
}

data class CollectionGivenNode(
    override val type: TypeRef,
    override val depth: Int,
    val elements: List<CallableDescriptor>,
    override val dependencies: List<GivenRequest>,
) : GivenNode() {
    override val callableFqName: FqName = FqName("GivenSet")
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val providedGivens: List<GivenNode>
        get() = emptyList()
}

data class ProviderGivenNode(
    override val type: TypeRef,
    override val depth: Int,
    val declarationStore: DeclarationStore,
    val isRequired: Boolean,
) : GivenNode() {
    override val callableFqName: FqName = FqName("Provider")
    override val dependencies: List<GivenRequest> = listOf(
        GivenRequest(
            type = type.typeArguments.last(),
            required = isRequired,
            callableFqName = callableFqName,
            parameterName = "instance".asNameId(),
            callableKey = "Provider",
            callContext = type.callContext
        )
    )
    override val providedGivens: List<GivenNode>
        get() = type.typeArguments.dropLast(1)
            .mapIndexed { index, parameterType ->
                ProviderParameterGivenNode(parameterType, index, this)
            }
    override val callContext: CallContext
        get() = CallContext.DEFAULT
}

data class ProviderParameterGivenNode(
    override val type: TypeRef,
    val index: Int,
    val provider: ProviderGivenNode,
) : GivenNode() {
    override val depth: Int
        get() = -1
    override val callableFqName: FqName
        get() = FqName("Provider.p$index")
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val providedGivens: List<GivenNode>
        get() = emptyList()
    override val callContext: CallContext
        get() = CallContext.DEFAULT
}

fun CallableDescriptor.toGivenNode(
    type: TypeRef,
    declarationStore: DeclarationStore,
    depth: Int,
): CallableGivenNode {
    return CallableGivenNode(
        type,
        getGivenRequests(type, declarationStore),
        depth,
        this
    )
}

fun CallableDescriptor.getGivenRequests(
    type: TypeRef,
    declarationStore: DeclarationStore,
): List<GivenRequest> {
    val info = declarationStore.givenInfoFor(this)
    val substitutionMap = getSubstitutionMap(
        listOf(type to returnType!!.toTypeRef())
    )
    val callableKey = uniqueKey()
    return allParameters
        .filter {
            val name = if (it === extensionReceiverParameter) "_receiver".asNameId() else it.name
            name in info.allGivens
        }
        .map {
            val name = if (it === extensionReceiverParameter) "_receiver".asNameId() else it.name
            GivenRequest(
                type = it.type.toTypeRef()
                    .substitute(substitutionMap),
                required = name in info.requiredGivens,
                callableFqName = fqNameSafe,
                parameterName = name,
                callableKey = callableKey,
                callContext = callContext
            )
        }
}

data class GivenRequest(
    val type: TypeRef,
    val required: Boolean,
    val callableFqName: FqName,
    val parameterName: Name,
    val callableKey: String,
    val callContext: CallContext,
)
