package com.ivianuu.injekt.compiler.resolution

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

data class GivenRequest(
    val type: TypeRef,
    val required: Boolean,
    val origin: FqName,
)
