/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*

data class CallableRef(
    val callable: CallableDescriptor,
    val type: TypeRef,
    val originalType: TypeRef,
    val typeParameters: List<ClassifierRef>,
    val parameterTypes: Map<String, TypeRef>,
    val givenParameters: Set<String>,
    val defaultOnAllErrorParameters: Set<String>,
    val typeArguments: Map<ClassifierRef, TypeRef>,
    val isGiven: Boolean,
    val source: CallableRef?,
    val callContext: CallContext,
    val owner: ClassifierRef?,
    val overriddenDepth: Int
)

fun CallableRef.substitute(map: Map<ClassifierRef, TypeRef>): CallableRef {
    if (map.isEmpty()) return this
    return copy(
        type = type.substitute(map),
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
    val info = if (isDeserializedDeclaration()) context.callableInfoFor(this, trace)
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
        .filter { it.isGiven(context, trace) }
        .mapTo(mutableSetOf()) { it.injektName() }
    val defaultOnAllErrorsParameters = info?.useDefaultOnAllErrorsParameters ?: valueParameters
        .asSequence()
        .filter { it.annotations.hasAnnotation(InjektFqNames.DefaultOnAllErrors) }
        .mapTo(mutableSetOf()) { it.injektName() }
    return CallableRef(
        callable = this,
        type = type,
        originalType = type,
        typeParameters = typeParameters,
        parameterTypes = parameterTypes,
        givenParameters = givenParameters,
        defaultOnAllErrorParameters = defaultOnAllErrorsParameters,
        typeArguments = typeParameters
            .map { it to it.defaultType }
            .toMap(),
        isGiven = isGiven(context, trace),
        source = null,
        callContext = callContext(trace.bindingContext),
        owner = null,
        overriddenDepth = 0
    ).also {
        trace.record(InjektWritableSlices.CALLABLE_REF_FOR_DESCRIPTOR, this, it)
    }
}
