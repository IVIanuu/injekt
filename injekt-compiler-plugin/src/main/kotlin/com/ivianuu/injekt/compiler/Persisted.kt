/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.CallableRef
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.STAR_PROJECTION_TYPE
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.forTypeKeyTypeParameters
import com.ivianuu.injekt.compiler.resolution.givenConstraintTypeParameters
import com.ivianuu.injekt.compiler.resolution.notGivens
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

inline fun <reified T> T.encode(): String = Json.encodeToString(this)
inline fun <reified T> String.decode(): T = Json.decodeFromString(this)

@Serializable
data class PersistedCallableInfo(
    @SerialName("0") val type: PersistedTypeRef,
    @SerialName("1") val typeParameters: List<PersistedClassifierInfo> = emptyList(),
    @SerialName("2") val parameterTypes: Map<String, PersistedTypeRef> = emptyMap(),
    @SerialName("3") val givenParameters: List<String> = emptyList(),
    @SerialName("4") val notGivens: List<PersistedTypeRef> = emptyList()
)

fun CallableRef.toPersistedCallableInfo(
    context: InjektContext,
    trace: BindingTrace
) = PersistedCallableInfo(
    type = type.toPersistedTypeRef(context),
    typeParameters = typeParameters.map { it.toPersistedClassifierInfo(context, trace) },
    parameterTypes = parameterTypes
        .mapValues { it.value.toPersistedTypeRef(context) },
    givenParameters = givenParameters,
    notGivens = notGivens.map { it.toPersistedTypeRef(context) }
)

@Serializable
data class PersistedClassifierInfo(
    @SerialName("0") val key: String,
    @SerialName("1") val superTypes: List<PersistedTypeRef> = emptyList(),
    @SerialName("2") val qualifiers: List<PersistedTypeRef> = emptyList(),
    @SerialName("3") val notGivens: List<PersistedTypeRef> = emptyList(),
    @SerialName("4") val primaryConstructorPropertyParameters: List<String> = emptyList(),
    @SerialName("5") val forTypeKeyTypeParameters: List<String> = emptyList(),
    @SerialName("6") val givenConstraintTypeParameters: List<String> = emptyList(),
    @SerialName("7") val isOptimizableGiven: Boolean = false
)

fun ClassifierRef.toPersistedClassifierInfo(
    context: InjektContext,
    trace: BindingTrace
) = PersistedClassifierInfo(
    key = if (descriptor is TypeParameterDescriptor) {
        descriptor.containingDeclaration
            .safeAs<ClassConstructorDescriptor>()
            ?.constructedClass
            ?.declaredTypeParameters
            ?.single { it.name == descriptor.name }
            ?.uniqueKey(context)
            ?: descriptor.uniqueKey(context)
    } else descriptor!!.uniqueKey(context),
    superTypes = superTypes.map { it.toPersistedTypeRef(context) },
    qualifiers = qualifiers.map { it.toPersistedTypeRef(context) },
    notGivens = notGivens(context, trace).map { it.toPersistedTypeRef(context) },
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters
        .map { it.asString() },
    forTypeKeyTypeParameters = forTypeKeyTypeParameters
        .map { it.asString() },
    givenConstraintTypeParameters = givenConstraintTypeParameters
        .map { it.asString() },
    isOptimizableGiven = descriptor.isSingletonGiven(context, trace)
)

@Serializable
data class PersistedTypeRef(
    @SerialName("0") val classifierKey: String,
    @SerialName("1") val arguments: List<PersistedTypeRef> = emptyList(),
    @SerialName("2") val qualifiers: List<PersistedTypeRef> = emptyList(),
    @SerialName("3") val isStarProjection: Boolean,
    @SerialName("4") val isMarkedNullable: Boolean,
    @SerialName("5") val isMarkedComposable: Boolean,
    @SerialName("6") val isGiven: Boolean
)

fun TypeRef.toPersistedTypeRef(context: InjektContext): PersistedTypeRef = PersistedTypeRef(
    classifierKey = classifier.descriptor?.uniqueKey(context) ?: "",
    arguments = arguments.map { it.toPersistedTypeRef(context) },
    qualifiers = qualifiers.map { it.toPersistedTypeRef(context) },
    isStarProjection = isStarProjection,
    isMarkedNullable = isMarkedNullable,
    isMarkedComposable = isMarkedComposable,
    isGiven = isGiven
)

fun PersistedTypeRef.toTypeRef(context: InjektContext, trace: BindingTrace?): TypeRef {
    if (isStarProjection) return STAR_PROJECTION_TYPE
    val classifier = context.classifierDescriptorForKey(classifierKey, trace)
        .toClassifierRef(context, trace)
    return classifier.defaultType
        .copy(
            qualifiers = qualifiers.map { it.toTypeRef(context, trace) },
            arguments = arguments.map { it.toTypeRef(context, trace) },
            isMarkedNullable = isMarkedNullable,
            isMarkedComposable = isMarkedComposable,
            isGiven = isGiven
        )
}

fun PersistedClassifierInfo.toClassifierRef(
    context: InjektContext,
    trace: BindingTrace?
): ClassifierRef {
    return context.classifierDescriptorForKey(key, trace)
        .toClassifierRef(context, trace)
        .let { raw ->
            if (superTypes.isNotEmpty() || qualifiers.isNotEmpty() ||
                primaryConstructorPropertyParameters.isNotEmpty())
                raw.copy(
                    superTypes = superTypes.map { it.toTypeRef(context, trace) },
                    qualifiers = qualifiers.map { it.toTypeRef(context, trace) },
                    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters
                        .map { it.asNameId() }
                )
            else raw
        }
}
