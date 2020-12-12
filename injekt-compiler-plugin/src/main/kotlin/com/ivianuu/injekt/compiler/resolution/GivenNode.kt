package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.asNameId
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

sealed class GivenNode {
    abstract val type: TypeRef
    abstract val dependencies: List<GivenRequest>
    abstract val callableFqName: FqName
}

data class CallableGivenNode(
    override val type: TypeRef,
    override val dependencies: List<GivenRequest>,
    val callable: CallableDescriptor,
) : GivenNode() {
    override val callableFqName: FqName = if (callable is ClassConstructorDescriptor)
        callable.constructedClass.fqNameSafe
    else callable.fqNameSafe
}

data class CollectionGivenNode(
    override val type: TypeRef,
    val elements: List<CallableDescriptor>,
    override val dependencies: List<GivenRequest>,
) : GivenNode() {
    override val callableFqName: FqName = FqName("givenCollectionOf")
}

data class ProviderGivenNode(
    override val type: TypeRef,
    val isRequired: Boolean,
) : GivenNode() {
    override val callableFqName: FqName = FqName("Provider")
    override val dependencies: List<GivenRequest> = listOf(
        GivenRequest(
            type = type.typeArguments.last(),
            required = isRequired,
            callableFqName = callableFqName,
            parameterName = "instance".asNameId()
        )
    )
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
                type = it.type.toTypeRef()
                    .substitute(substitutionMap),
                required = it.name in info.requiredGivens,
                callableFqName = fqNameSafe,
                parameterName = it.name
            )
        }
}

data class GivenRequest(
    val type: TypeRef,
    val required: Boolean,
    val callableFqName: FqName,
    val parameterName: Name,
)
