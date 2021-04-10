package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.toClassifierRef
import com.ivianuu.injekt.compiler.toTypeRef
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

data class CallableRef(
    val callable: CallableDescriptor,
    val type: TypeRef,
    val originalType: TypeRef,
    val typeParameters: List<ClassifierRef>,
    val parameterTypes: Map<String, TypeRef>,
    val givenParameters: List<String>,
    val typeArguments: Map<ClassifierRef, TypeRef>,
    val isGiven: Boolean,
    val notGivens: List<TypeRef>,
    val constrainedGivenSource: CallableRef?,
    val callContext: CallContext,
    val owner: ClassifierRef?,
    val overriddenDepth: Int
)

fun CallableRef.substitute(map: Map<ClassifierRef, TypeRef>): CallableRef {
    if (map.isEmpty()) return this
    return copy(
        type = type.substitute(map),
        notGivens = notGivens.map { it.substitute(map) },
        parameterTypes = parameterTypes
            .mapValues { it.value.substitute(map) },
        typeArguments = typeArguments
            .mapValues { it.value.substitute(map) }
    )
}

fun CallableRef.substituteInputs(map: Map<ClassifierRef, TypeRef>): CallableRef {
    if (map.isEmpty()) return this
    return copy(
        parameterTypes = parameterTypes.mapValues { it.value.substitute(map) },
        typeArguments = typeArguments
            .mapValues { it.value.substitute(map) }
    )
}

fun CallableRef.makeGiven(): CallableRef = if (isGiven) this else copy(isGiven = true)

fun CallableDescriptor.toCallableRef(
    context: InjektContext,
    trace: BindingTrace
): CallableRef {
    trace.get(InjektWritableSlices.CALLABLE_REF_FOR_DESCRIPTOR, this)?.let { return it }
    val info = if (original.isExternalDeclaration()) context.callableInfoFor(this, trace)
    else null
    val type = info?.type?.toTypeRef(context, trace)
        ?: kotlin.run {
            val psi = findPsi()
            if (psi is KtProperty && psi.initializer != null) {
                trace.get(InjektWritableSlices.EXPECTED_TYPE, psi.initializer)
            } else if (psi is KtFunction && psi.bodyExpression != null) {
                trace.get(InjektWritableSlices.EXPECTED_TYPE, psi.bodyExpression)
            } else null
        }
        ?: returnType!!.toTypeRef(context, trace)
    val notGivens = info?.notGivens
        ?.map { it.toTypeRef(context, trace) }
        ?: (annotations + returnType!!.annotations)
            .filter { it.type.constructor.declarationDescriptor!!.fqNameSafe == InjektFqNames.NotGiven }
            .map { it.type.arguments.single().type.toTypeRef(context, trace) }
    val typeParameters = info
        ?.typeParameters
        ?.map { it.toClassifierRef(context, trace) } ?: typeParameters
        .map { it.toClassifierRef(context, trace) }
    val parameterTypes = info
        ?.parameterTypes
        ?.mapValues { it.value.toTypeRef(context, trace) }
        ?: (if (this is ConstructorDescriptor) valueParameters else allParameters)
            .map { it.injektName() to it.type.toTypeRef(context, trace) }
            .toMap()
    val givenParameters = info?.givenParameters ?: (if (this is ConstructorDescriptor) valueParameters else allParameters)
        .asSequence()
        .filter { it.isGiven(context, trace) }
        .map { it.injektName() }
        .toList()
    return CallableRef(
        callable = this,
        type = type,
        originalType = type,
        typeParameters = typeParameters,
        parameterTypes = parameterTypes,
        givenParameters = givenParameters,
        notGivens = notGivens,
        typeArguments = typeParameters
            .map { it to it.defaultType }
            .toMap(),
        isGiven = isGiven(context, trace),
        constrainedGivenSource = null,
        callContext = callContext(trace.bindingContext),
        owner = null,
        overriddenDepth = 0
    ).also {
        trace.record(InjektWritableSlices.CALLABLE_REF_FOR_DESCRIPTOR, this, it)
    }
}
