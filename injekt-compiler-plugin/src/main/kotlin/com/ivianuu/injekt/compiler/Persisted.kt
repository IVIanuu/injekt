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
import com.ivianuu.injekt.compiler.resolution.getSubstitutionMap
import com.ivianuu.injekt.compiler.resolution.givenConstraintTypeParameters
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.squareup.moshi.JsonClass
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@JsonClass(generateAdapter = true)
data class PersistedCallableInfo(
    val type: PersistedTypeRef,
    val typeParameters: List<PersistedClassifierRef>,
    val parameterTypes: Map<String, PersistedTypeRef>,
    val givenParameters: Set<String>
)

fun CallableRef.toPersistedCallableInfo(context: InjektContext) = PersistedCallableInfo(
    type = type.toPersistedTypeRef(context),
    typeParameters = typeParameters.map { it.toPersistedClassifierRef(context) },
    parameterTypes = parameterTypes
        .mapValues { it.value.toPersistedTypeRef(context) },
    givenParameters = givenParameters
)

fun CallableRef.apply(
    context: InjektContext,
    trace: BindingTrace?,
    info: PersistedCallableInfo?
): CallableRef {
    return if (info == null) this
    else {
        val original = callable.original
        val originalType = original.returnType!!.toTypeRef(context, trace)
        val substitutionMap = getSubstitutionMap(
            context,
            listOf(type to originalType) +
                    parameterTypes.values
                        .zip(
                            (if (original is ConstructorDescriptor) original.valueParameters else original.allParameters)
                                .map { it.type.toTypeRef(context, trace) }
                        )
        )
        copy(
            type = info.type.toTypeRef(context, trace),
            originalType = info.type.toTypeRef(context, trace),
            typeParameters = info.typeParameters.map {
                it.toClassifierRef(context, trace)
            },
            parameterTypes = info.parameterTypes
                .mapValues { it.value.toTypeRef(context, trace) },
            givenParameters = info.givenParameters
        ).substitute(substitutionMap)
    }
}

@JsonClass(generateAdapter = true)
data class PersistedClassifierInfo(
    val fqName: String,
    val qualifier: PersistedTypeRef?,
    val superTypes: List<PersistedTypeRef>,
    val primaryConstructorPropertyParameters: List<String>,
    val forTypeKeyTypeParameters: List<String>,
    val givenConstraintTypeParameters: List<String>
)

fun ClassifierRef.toPersistedClassifierInfo(context: InjektContext) = PersistedClassifierInfo(
    fqName = descriptor!!.fqNameSafe.asString(),
    qualifier = qualifier?.toPersistedTypeRef(context),
    superTypes = superTypes.map { it.toPersistedTypeRef(context) },
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters
        .map { it.asString() },
    forTypeKeyTypeParameters = forTypeKeyTypeParameters
        .map { it.asString() },
    givenConstraintTypeParameters = givenConstraintTypeParameters
        .map { it.asString() }
)

@JsonClass(generateAdapter = true)
data class PersistedTypeRef(
    val classifierKey: String,
    val arguments: List<PersistedTypeRef>,
    val qualifier: PersistedTypeRef?,
    val isStarProjection: Boolean,
    val isMarkedNullable: Boolean,
    val isMarkedComposable: Boolean,
    val isGiven: Boolean
)

fun TypeRef.toPersistedTypeRef(context: InjektContext): PersistedTypeRef = PersistedTypeRef(
    classifierKey = classifier.descriptor?.uniqueKey(context) ?: "",
    arguments = arguments.map { it.toPersistedTypeRef(context) },
    qualifier = qualifier?.toPersistedTypeRef(context),
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
            qualifier = qualifier?.toTypeRef(context, trace),
            arguments = arguments.map { it.toTypeRef(context, trace) },
            isMarkedNullable = isMarkedNullable,
            isMarkedComposable = isMarkedComposable,
            isGiven = isGiven
        )
}

@JsonClass(generateAdapter = true)
data class PersistedClassifierRef(
    val key: String,
    val superTypes: List<PersistedTypeRef>,
    val qualifier: PersistedTypeRef?,
    val primaryConstructorPropertyParameters: List<String>,
    val forTypeKeyTypeParameters: List<String>,
    val givenConstraintTypeParameters: List<String>
)

fun ClassifierRef.toPersistedClassifierRef(
    context: InjektContext
) = PersistedClassifierRef(
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
    qualifier = qualifier?.toPersistedTypeRef(context),
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters
        .map { it.asString() },
    forTypeKeyTypeParameters = forTypeKeyTypeParameters
        .map { it.asString() },
    givenConstraintTypeParameters = givenConstraintTypeParameters
        .map { it.asString() }
)

fun PersistedClassifierRef.toClassifierRef(
    context: InjektContext,
    trace: BindingTrace?
): ClassifierRef {
    return context.classifierDescriptorForKey(key, trace)
        .toClassifierRef(context, trace)
        .let { raw ->
            if (superTypes.isNotEmpty() || qualifier != null ||
                primaryConstructorPropertyParameters.isNotEmpty())
                raw.copy(
                    superTypes = superTypes.map { it.toTypeRef(context, trace) },
                    qualifier = qualifier?.toTypeRef(context, trace),
                    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters
                        .map { it.asNameId() }
                )
            else raw
        }
}

fun PersistedClassifierRef.toPersistedClassifierInfo() = PersistedClassifierInfo(
    fqName = key.split(":")[1],
    qualifier = qualifier,
    superTypes = superTypes,
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
    forTypeKeyTypeParameters = forTypeKeyTypeParameters,
    givenConstraintTypeParameters = givenConstraintTypeParameters
)

fun ClassifierRef.apply(
    context: InjektContext,
    trace: BindingTrace?,
    info: PersistedClassifierInfo?
): ClassifierRef {
    return if (info == null || !descriptor!!.isExternalDeclaration()) this
    else copy(
        qualifier = info.qualifier?.toTypeRef(context, trace),
        superTypes = info.superTypes.map { it.toTypeRef(context, trace) },
        primaryConstructorPropertyParameters = info.primaryConstructorPropertyParameters.map { it.asNameId() }
    )
}
