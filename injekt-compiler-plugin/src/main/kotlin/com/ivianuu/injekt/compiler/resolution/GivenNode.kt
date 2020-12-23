package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.analysis.hasDefaultValueIgnoringGiven
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.transform.toKotlinType
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

sealed class GivenNode {
    abstract val type: TypeRef
    abstract val originalType: TypeRef
    abstract val dependencies: List<GivenRequest>
    abstract val callableFqName: FqName
    abstract val callContext: CallContext
    abstract val depth: Int
    abstract val providedGivens: List<GivenNode>
    abstract val providedGivenSetElements: List<CallableRef>
}

data class CallableGivenNode(
    override val type: TypeRef,
    override val dependencies: List<GivenRequest>,
    override val depth: Int,
    val callable: CallableRef,
) : GivenNode() {
    override val callableFqName: FqName = if (callable.callable is ClassConstructorDescriptor)
        callable.callable.constructedClass.fqNameSafe
    else callable.callable.fqNameSafe
    override val callContext: CallContext
        get() = callable.callContext
    override val providedGivens: List<GivenNode>
        get() = emptyList()
    override val providedGivenSetElements: List<CallableRef>
        get() = emptyList()
    override val originalType: TypeRef
        get() = callable.originalType
}

data class SetGivenNode(
    override val type: TypeRef,
    override val depth: Int,
    val elements: List<CallableRef>,
    override val dependencies: List<GivenRequest>,
) : GivenNode() {
    override val callableFqName: FqName = FqName("GivenSet")
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val providedGivens: List<GivenNode>
        get() = emptyList()
    override val providedGivenSetElements: List<CallableRef>
        get() = emptyList()
    override val originalType: TypeRef
        get() = type
}

data class DefaultGivenNode(override val type: TypeRef) : GivenNode() {
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val callableFqName: FqName
        get() = FqName.ROOT
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val originalType: TypeRef
        get() = type
    override val providedGivens: List<GivenNode>
        get() = emptyList()
    override val providedGivenSetElements: List<CallableRef>
        get() = emptyList()
    override val depth: Int
        get() = -1
}

data class FunGivenNode(
    override val type: TypeRef,
    override val depth: Int,
    val callable: CallableRef,
) : GivenNode() {
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val callableFqName: FqName
        get() = type.classifier.fqName
    override val dependencies: List<GivenRequest> = callable.getGivenRequests(true)
    override val originalType: TypeRef
        get() = type.classifier.defaultType
    override val providedGivens: List<GivenNode>
        get() = emptyList()
    override val providedGivenSetElements: List<CallableRef>
        get() = emptyList()
}

data class ObjectGivenNode(override val type: TypeRef) : GivenNode() {
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val callableFqName: FqName
        get() = type.classifier.fqName
    override val dependencies: List<GivenRequest>
        get() = emptyList()
    override val depth: Int
        get() = -1
    override val originalType: TypeRef
        get() = type
    override val providedGivens: List<GivenNode>
        get() = emptyList()
    override val providedGivenSetElements: List<CallableRef>
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
            callContext = type.callContext
        )
    )
    override val providedGivens = mutableListOf<GivenNode>()
    override val providedGivenSetElements = mutableListOf<CallableRef>()
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val originalType: TypeRef
        get() = type
    init {
        type
            .toKotlinType()
            .memberScope
            .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
            .first()
            .valueParameters
            .map { ProviderParameterDescriptor(this, it) }
            .map {
                CallableRef(it, givenKind = type.typeArguments[it.index].givenKind)
            }
            .forEach { parameter ->
                parameter.collectGivens(
                    path = listOf(callableFqName, type),
                    addGiven = {
                        providedGivens += it.toGivenNode(it.type, -1)
                    },
                    addGivenSetElement = { providedGivenSetElements += it }
                )
            }
    }

    class ProviderParameterDescriptor(
        val given: ProviderGivenNode,
        private val delegate: ValueParameterDescriptor
    ) : ValueParameterDescriptor by delegate
}

fun CallableRef.toGivenNode(type: TypeRef, depth: Int): CallableGivenNode {
    val finalCallable = substitute(getSubstitutionMap(listOf(type to this.type)))
    return CallableGivenNode(
        type,
        finalCallable.getGivenRequests(false),
        depth,
        finalCallable
    )
}

fun CallableRef.getGivenRequests(forFunExpression: Boolean): List<GivenRequest> {
    return callable.allParameters
        .filter {
            callable !is ClassConstructorDescriptor || it.name.asString() != "<this>"
        }
        .filter {
            it === callable.dispatchReceiverParameter ||
                    it.givenKind() == GivenKind.VALUE ||
                    parameterTypes[it]!!.givenKind == GivenKind.VALUE
        }
        .map {
            val name = when {
                it === callable.dispatchReceiverParameter -> "_dispatchReceiver".asNameId()
                it === callable.extensionReceiverParameter -> "_extensionReceiver".asNameId()
                else -> it.name
            }
            GivenRequest(
                type = parameterTypes[it]!!,
                required = it !is ValueParameterDescriptor || !it.hasDefaultValueIgnoringGiven,
                callableFqName = callable.fqNameSafe,
                parameterName = name,
                callContext = if (forFunExpression) callContext else null
            )
        }
}

data class GivenRequest(
    val type: TypeRef,
    val required: Boolean,
    val callableFqName: FqName,
    val parameterName: Name,
    val callContext: CallContext?,
) {
    val forDispatchReceiver: Boolean
        get() = parameterName.asString() == "_dispatchReceiver"
}
