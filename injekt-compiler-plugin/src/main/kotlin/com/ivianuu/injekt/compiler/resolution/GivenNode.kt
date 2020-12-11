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

fun CallableDescriptor.toGivenNode(
    type: TypeRef,
    declarationStore: DeclarationStore,
): CallableGivenNode {
    val info = declarationStore.givenInfoFor(this)
    val substitutionMap = getSubstitutionMap(
        listOf(type to returnType!!.toTypeRef())
    )
    return CallableGivenNode(
        type,
        valueParameters
            .filter { it.name in info.allGivens }
            .map {
                GivenRequest(
                    it.type.toTypeRef()
                        .substitute(substitutionMap),
                    it.name in info.requiredGivens,
                    it.fqNameSafe
                )
            },
        this
    )
}

data class GivenRequest(
    val type: TypeRef,
    val required: Boolean,
    val origin: FqName,
)
