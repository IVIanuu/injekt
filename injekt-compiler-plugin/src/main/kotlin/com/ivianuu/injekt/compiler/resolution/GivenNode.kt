package com.ivianuu.injekt.compiler.resolution

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.name.FqName

sealed class GivenNode(
    val type: TypeRef
) {
    abstract val dependencies: List<GivenRequest>
}

class CallableGivenNode(
    type: TypeRef,
    override val dependencies: List<GivenRequest>,
    val callable: CallableDescriptor
) : GivenNode(type)

data class GivenRequest(
    val type: TypeRef,
    val required: Boolean,
    val origin: FqName,
)
