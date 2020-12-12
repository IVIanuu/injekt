package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

sealed class GivenNode(val type: TypeRef) {
    abstract val dependencies: List<GivenRequest>
    abstract val origin: FqName
}

class CallableGivenNode(
    type: TypeRef,
    override val dependencies: List<GivenRequest>,
    val callable: CallableDescriptor,
) : GivenNode(type) {
    override val origin: FqName
        get() = callable.fqNameSafe
}

class CollectionGivenNode(
    type: TypeRef,
    override val origin: FqName,
    val elements: List<CallableDescriptor>,
    override val dependencies: List<GivenRequest>,
) : GivenNode(type)

class ProviderGivenNode(
    type: TypeRef,
    override val origin: FqName,
    isRequired: Boolean,
) : GivenNode(type) {
    override val dependencies: List<GivenRequest> =
        listOf(GivenRequest(type.typeArguments.last(), isRequired, origin))
}

fun CallableDescriptor.toGivenNode(
    type: TypeRef,
    declarationStore: DeclarationStore,
): CallableGivenNode {
    return CallableGivenNode(
        type,
        getGivenRequests(type, declarationStore),
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
    return valueParameters
        .filter { it.name in info.allGivens }
        .map {
            GivenRequest(
                it.type.toTypeRef()
                    .substitute(substitutionMap),
                it.name in info.requiredGivens,
                it.fqNameSafe
            )
        }
}

data class GivenGraph(val givensByRequest: Map<GivenRequest, GivenNode>)

data class GivenRequest(
    val type: TypeRef,
    val required: Boolean,
    val origin: FqName,
)
