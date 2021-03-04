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

import com.ivianuu.injekt.compiler.resolution.AnnotationRef
import com.ivianuu.injekt.compiler.resolution.ArrayValue
import com.ivianuu.injekt.compiler.resolution.BooleanValue
import com.ivianuu.injekt.compiler.resolution.ByteValue
import com.ivianuu.injekt.compiler.resolution.CallableRef
import com.ivianuu.injekt.compiler.resolution.CharValue
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.ConstantValue
import com.ivianuu.injekt.compiler.resolution.ContributionKind
import com.ivianuu.injekt.compiler.resolution.DoubleValue
import com.ivianuu.injekt.compiler.resolution.EnumValue
import com.ivianuu.injekt.compiler.resolution.FloatValue
import com.ivianuu.injekt.compiler.resolution.IntValue
import com.ivianuu.injekt.compiler.resolution.KClassValue
import com.ivianuu.injekt.compiler.resolution.LongValue
import com.ivianuu.injekt.compiler.resolution.STAR_PROJECTION_TYPE
import com.ivianuu.injekt.compiler.resolution.ShortValue
import com.ivianuu.injekt.compiler.resolution.StringValue
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.UByteValue
import com.ivianuu.injekt.compiler.resolution.UIntValue
import com.ivianuu.injekt.compiler.resolution.ULongValue
import com.ivianuu.injekt.compiler.resolution.UShortValue
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.defaultType
import com.ivianuu.injekt.compiler.resolution.getSubstitutionMap
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.typeWith
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@JsonClass(generateAdapter = true) data class PersistedCallableInfo(
    val type: PersistedTypeRef,
    val typeParameters: List<PersistedClassifierRef>,
    val parameterTypes: Map<String, PersistedTypeRef>,
    val parameterContributionKinds: Map<String, ContributionKind?>,
    val qualifiers: List<PersistedAnnotationRef>
)

fun CallableRef.toPersistedCallableInfo(declarationStore: DeclarationStore) = PersistedCallableInfo(
    type = type.toPersistedTypeRef(declarationStore),
    typeParameters = typeParameters.map { it.toPersistedClassifierRef(declarationStore) },
    parameterTypes = parameterTypes
        .mapValues { it.value.toPersistedTypeRef(declarationStore) },
    parameterContributionKinds = parameterContributionKinds,
    qualifiers = qualifiers.map { it.toPersistedAnnotationRef(declarationStore) }
)

fun CallableRef.apply(
    declarationStore: DeclarationStore,
    info: PersistedCallableInfo?
): CallableRef {
    return if (info == null || !callable.isExternalDeclaration()) this
    else {
        val original = callable.original.toCallableRef(declarationStore, false)
        val substitutionMap = getSubstitutionMap(
            declarationStore,
            listOf(type to original.type) +
                    parameterTypes.values.zip(original.parameterTypes.values)
        )
        copy(
            type = info.type.toTypeRef(declarationStore),
            originalType = info.type.toTypeRef(declarationStore),
            typeParameters = info.typeParameters.map {
                it.toClassifierRef(declarationStore)
            },
            parameterTypes = info.parameterTypes
                .mapValues { it.value.toTypeRef(declarationStore) },
            parameterContributionKinds = info.parameterContributionKinds,
            qualifiers = info.qualifiers.map { it.toAnnotationRef(declarationStore) }
        ).substitute(substitutionMap)
    }
}

@JsonClass(generateAdapter = true) data class PersistedClassifierInfo(
    val fqName: String,
    val qualifiers: List<PersistedAnnotationRef>,
    val superTypes: List<PersistedTypeRef>,
    val expandedType: PersistedTypeRef?
)

fun ClassifierRef.toPersistedClassifierInfo(declarationStore: DeclarationStore) = PersistedClassifierInfo(
    fqName = descriptor!!.fqNameSafe.asString(),
    qualifiers = qualifiers.map { it.toPersistedAnnotationRef(declarationStore) },
    superTypes = superTypes.map { it.toPersistedTypeRef(declarationStore) },
    expandedType = expandedType?.toPersistedTypeRef(declarationStore)
)

@JsonClass(generateAdapter = true) data class PersistedTypeRef(
    val classifierKey: String,
    val qualifiers: List<PersistedAnnotationRef>,
    val arguments: List<PersistedTypeRef>,
    val isStarProjection: Boolean,
    val isMarkedNullable: Boolean,
    val isComposable: Boolean,
    val contributionKind: ContributionKind?,
    val variance: Variance
)

fun TypeRef.toPersistedTypeRef(declarationStore: DeclarationStore): PersistedTypeRef = PersistedTypeRef(
    classifierKey = classifier.descriptor?.uniqueKey(declarationStore) ?: "",
    qualifiers = qualifiers.map { it.toPersistedAnnotationRef(declarationStore) },
    arguments = arguments.map { it.toPersistedTypeRef(declarationStore) },
    isStarProjection = isStarProjection,
    isMarkedNullable = isMarkedNullable,
    isComposable = isComposable,
    contributionKind = contributionKind,
    variance = variance
)

fun PersistedTypeRef.toTypeRef(declarationStore: DeclarationStore): TypeRef {
    if (isStarProjection) return STAR_PROJECTION_TYPE
    val classifier = declarationStore.classifierDescriptorForKey(classifierKey)
        .toClassifierRef(declarationStore)
    return classifier.defaultType
        .copy(
            qualifiers = qualifiers.map { it.toAnnotationRef(declarationStore) },
            arguments = arguments.map { it.toTypeRef(declarationStore) },
            isMarkedNullable = isMarkedNullable,
            isComposable = isComposable,
            contributionKind = contributionKind,
            variance = variance
        )
}

@JsonClass(generateAdapter = true) data class PersistedClassifierRef(
    val key: String,
    val superTypes: List<PersistedTypeRef>,
    val expandedType: PersistedTypeRef?,
    val qualifiers: List<PersistedAnnotationRef>
)

fun ClassifierRef.toPersistedClassifierRef(
    declarationStore: DeclarationStore
) = PersistedClassifierRef(
    key = if (descriptor is TypeParameterDescriptor) {
        descriptor.containingDeclaration
            .safeAs<ClassConstructorDescriptor>()
            ?.constructedClass
            ?.declaredTypeParameters
            ?.single { it.name == descriptor.name }
            ?.uniqueKey(declarationStore)
            ?: descriptor.uniqueKey(declarationStore)
     } else descriptor!!.uniqueKey(declarationStore),
    superTypes = superTypes.map { it.toPersistedTypeRef(declarationStore) },
    expandedType = expandedType?.toPersistedTypeRef(declarationStore),
    qualifiers = qualifiers.map { it.toPersistedAnnotationRef(declarationStore) }
)

fun PersistedClassifierRef.toClassifierRef(declarationStore: DeclarationStore): ClassifierRef {
    return declarationStore.classifierDescriptorForKey(key)
        .toClassifierRef(declarationStore)
        .copy(
            superTypes = superTypes.map { it.toTypeRef(declarationStore) },
            expandedType = expandedType?.toTypeRef(declarationStore),
            qualifiers = qualifiers.map { it.toAnnotationRef(declarationStore) }
        )
}

fun PersistedClassifierRef.toPersistedClassifierInfo() = PersistedClassifierInfo(
    fqName = key.split(":")[1],
    qualifiers = qualifiers,
    superTypes = superTypes,
    expandedType = expandedType
)

fun ClassifierRef.apply(
    declarationStore: DeclarationStore,
    info: PersistedClassifierInfo?
): ClassifierRef {
    return if (info == null || !descriptor!!.isExternalDeclaration()) this
    else copy(
        qualifiers = info.qualifiers.map { it.toAnnotationRef(declarationStore) },
        superTypes = info.superTypes.map { it.toTypeRef(declarationStore) },
        expandedType = info.expandedType?.toTypeRef(declarationStore)
    )
}


@JsonClass(generateAdapter = true) data class PersistedAnnotationRef(
    val type: PersistedTypeRef,
    val arguments: Map<String, PersistedConstantValue>
)

fun AnnotationRef.toPersistedAnnotationRef(declarationStore: DeclarationStore): PersistedAnnotationRef {
    return PersistedAnnotationRef(
        type = type.toPersistedTypeRef(declarationStore),
        arguments = arguments
            .mapKeys { it.key.asString() }
            .mapValues { it.value.toPersistedConstantValue(declarationStore) }
    )
}

fun PersistedAnnotationRef.toAnnotationRef(declarationStore: DeclarationStore) = AnnotationRef(
    type.toTypeRef(declarationStore),
    arguments
        .mapKeys { it.key.asNameId() }
        .mapValues { it.value.toConstantValue(declarationStore) }
)

fun ConstantValue<*>.toPersistedConstantValue(
    declarationStore: DeclarationStore
): PersistedConstantValue = when (this) {
    is ArrayValue -> PersistedArrayValue(
        value.map { it.toPersistedConstantValue(declarationStore) },
        type.arguments.single().toPersistedTypeRef(declarationStore)
    )
    is BooleanValue -> PersistedBooleanValue(value)
    is ByteValue -> PersistedByteValue(value)
    is CharValue -> PersistedCharValue(value)
    is DoubleValue -> PersistedDoubleValue(value)
    is EnumValue -> PersistedEnumValue(value.first.fqName.asString(), value.second.asString())
    is FloatValue -> PersistedFloatValue(value)
    is IntValue -> PersistedIntValue(value)
    is KClassValue -> PersistedKClassValue(
        value.fqName.asString(),
        type.toPersistedTypeRef(declarationStore)
    )
    is LongValue -> PersistedLongValue(value)
    is ShortValue -> PersistedShortValue(value)
    is StringValue -> PersistedStringValue(value)
    is UByteValue -> PersistedUByteValue(value)
    is UIntValue -> PersistedUIntValue(value)
    is ULongValue -> PersistedULongValue(value)
    is UShortValue -> PersistedUShortValue(value)
}

fun PersistedConstantValue.toConstantValue(declarationStore: DeclarationStore): ConstantValue<*> = when (this) {
    is PersistedArrayValue -> ArrayValue(
        value = value.map { it.toConstantValue(declarationStore) },
        type = declarationStore.module.builtIns.array
            .defaultType
            .toTypeRef(declarationStore)
            .typeWith(listOf(elementType.toTypeRef(declarationStore)))
    )
    is PersistedBooleanValue -> BooleanValue(
        value,
        declarationStore.module.builtIns.booleanType.toTypeRef(declarationStore)
    )
    is PersistedByteValue -> ByteValue(
        value,
        declarationStore.module.builtIns.byteType.toTypeRef(declarationStore)
    )
    is PersistedCharValue -> CharValue(
        value,
        declarationStore.module.builtIns.charType.toTypeRef(declarationStore)
    )
    is PersistedDoubleValue -> DoubleValue(
        value,
        declarationStore.module.builtIns.doubleType.toTypeRef(declarationStore)
    )
    is PersistedEnumValue -> {
        val enumClassifier = declarationStore.classifierDescriptorForFqName(FqName(enumClassifierFqName))
            .toClassifierRef(declarationStore)
        EnumValue(enumClassifier to enumValue.asNameId(), enumClassifier.defaultType)
    }
    is PersistedFloatValue -> FloatValue(
        value,
        declarationStore.module.builtIns.floatType.toTypeRef(declarationStore)
    )
    is PersistedIntValue -> IntValue(
        value,
        declarationStore.module.builtIns.intType.toTypeRef(declarationStore)
    )
    is PersistedKClassValue -> KClassValue(
        declarationStore.classifierDescriptorForFqName(FqName(classifierFqName))
            .toClassifierRef(declarationStore),
        type.toTypeRef(declarationStore)
    )
    is PersistedLongValue -> LongValue(
        value,
        declarationStore.module.builtIns.longType.toTypeRef(declarationStore)
    )
    is PersistedShortValue -> ShortValue(
        value,
        declarationStore.module.builtIns.shortType.toTypeRef(declarationStore)
    )
    is PersistedStringValue -> StringValue(
        value,
        declarationStore.module.builtIns.stringType.toTypeRef(declarationStore)
    )
    is PersistedUByteValue -> UByteValue(
        value,
        declarationStore.classifierDescriptorForFqName(FqName("kotlin.UByte"))
            .toClassifierRef(declarationStore)
            .defaultType
    )
    is PersistedUIntValue -> UIntValue(
        value,
        declarationStore.classifierDescriptorForFqName(FqName("kotlin.UInt"))
            .toClassifierRef(declarationStore)
            .defaultType
    )
    is PersistedULongValue -> ULongValue(
        value,
        declarationStore.classifierDescriptorForFqName(FqName("kotlin.ULong"))
            .toClassifierRef(declarationStore)
            .defaultType
    )
    is PersistedUShortValue -> UShortValue(
        value,
        declarationStore.classifierDescriptorForFqName(FqName("kotlin.UShort"))
            .toClassifierRef(declarationStore)
            .defaultType
    )
}

@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class PersistedConstantValue

@TypeLabel("array")
@JsonClass(generateAdapter = true)
data class PersistedArrayValue(
    val value: List<PersistedConstantValue>,
    val elementType: PersistedTypeRef
) : PersistedConstantValue()

@TypeLabel("boolean")
@JsonClass(generateAdapter = true)
data class PersistedBooleanValue(val value: Boolean
) : PersistedConstantValue()

@TypeLabel("byte")
@JsonClass(generateAdapter = true)
data class PersistedByteValue(val value: Byte) : PersistedConstantValue()

@TypeLabel("char")
@JsonClass(generateAdapter = true)
data class PersistedCharValue(val value: Char) : PersistedConstantValue()

@TypeLabel("double")
@JsonClass(generateAdapter = true)
data class PersistedDoubleValue(val value: Double) : PersistedConstantValue()

@TypeLabel("enum")
@JsonClass(generateAdapter = true)
data class PersistedEnumValue(
    val enumClassifierFqName: String,
    val enumValue: String
) : PersistedConstantValue()

@TypeLabel("float")
@JsonClass(generateAdapter = true)
data class PersistedFloatValue(val value: Float) : PersistedConstantValue()

@TypeLabel("int")
@JsonClass(generateAdapter = true)
data class PersistedIntValue(val value: Int) : PersistedConstantValue()

@TypeLabel("kclass")
@JsonClass(generateAdapter = true)
data class PersistedKClassValue(
    val classifierFqName: String,
    val type: PersistedTypeRef
) : PersistedConstantValue()

@TypeLabel("long")
@JsonClass(generateAdapter = true)
data class PersistedLongValue(val value: Long) : PersistedConstantValue()

@TypeLabel("short")
@JsonClass(generateAdapter = true)
data class PersistedShortValue(val value: Short) : PersistedConstantValue()

@TypeLabel("string")
@JsonClass(generateAdapter = true)
data class PersistedStringValue(val value: String) : PersistedConstantValue()

@TypeLabel("ubyte")
@JsonClass(generateAdapter = true)
data class PersistedUByteValue(val value: Byte) : PersistedConstantValue()

@TypeLabel("uint")
@JsonClass(generateAdapter = true)
data class PersistedUIntValue(val value: Int) : PersistedConstantValue()

@TypeLabel("ulong")
@JsonClass(generateAdapter = true)
data class PersistedULongValue(val value: Long) : PersistedConstantValue()

@TypeLabel("ushort")
@JsonClass(generateAdapter = true)
data class PersistedUShortValue(val value: Short) : PersistedConstantValue()
