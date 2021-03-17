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
import com.ivianuu.injekt.compiler.resolution.getSubstitutionMap
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toAnnotationRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.typeWith
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@JsonClass(generateAdapter = true)
data class PersistedCallableInfo(
    val type: PersistedTypeRef,
    val typeParameters: List<PersistedClassifierRef>,
    val parameterTypes: Map<String, PersistedTypeRef>,
    val givenParameters: Set<String>,
    val qualifiers: List<PersistedAnnotationRef>
)

fun CallableRef.toPersistedCallableInfo(context: InjektContext) = PersistedCallableInfo(
    type = type.toPersistedTypeRef(context),
    typeParameters = typeParameters.map { it.toPersistedClassifierRef(context) },
    parameterTypes = parameterTypes
        .mapValues { it.value.toPersistedTypeRef(context) },
    givenParameters = givenParameters,
    qualifiers = qualifiers.map { it.toPersistedAnnotationRef(context) }
)

fun CallableRef.apply(
    context: InjektContext,
    trace: BindingTrace?,
    info: PersistedCallableInfo?
): CallableRef {
    return if (info == null) this
    else {
        val original = callable.original
        val originalQualifiers = original.getAnnotatedAnnotations(InjektFqNames.Qualifier)
            .map { it.toAnnotationRef(context, trace) }
        val originalType = original.returnType!!.toTypeRef(context, trace).let {
            if (originalQualifiers.isNotEmpty()) it.copy(qualifiers = originalQualifiers + it.qualifiers)
            else it
        }
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
            givenParameters = info.givenParameters,
            qualifiers = info.qualifiers.map { it.toAnnotationRef(context, trace) }
        ).substitute(substitutionMap)
    }
}

@JsonClass(generateAdapter = true)
data class PersistedClassifierInfo(
    val fqName: String,
    val qualifiers: List<PersistedAnnotationRef>,
    val superTypes: List<PersistedTypeRef>,
    val primaryConstructorPropertyParameters: List<String>,
    val forTypeKeyTypeParameters: List<String>
)

fun ClassifierRef.toPersistedClassifierInfo(context: InjektContext) = PersistedClassifierInfo(
    fqName = descriptor!!.fqNameSafe.asString(),
    qualifiers = qualifiers.map { it.toPersistedAnnotationRef(context) },
    superTypes = superTypes.map { it.toPersistedTypeRef(context) },
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters
        .map { it.asString() },
    forTypeKeyTypeParameters = forTypeKeyTypeParameters
        .map { it.asString() }
)

@JsonClass(generateAdapter = true)
data class PersistedTypeRef(
    val classifierKey: String,
    val qualifiers: List<PersistedAnnotationRef>,
    val arguments: List<PersistedTypeRef>,
    val isStarProjection: Boolean,
    val isMarkedNullable: Boolean,
    val isMarkedComposable: Boolean,
    val isGiven: Boolean
)

fun TypeRef.toPersistedTypeRef(context: InjektContext): PersistedTypeRef = PersistedTypeRef(
    classifierKey = classifier.descriptor?.uniqueKey(context) ?: "",
    qualifiers = qualifiers.map { it.toPersistedAnnotationRef(context) },
    arguments = arguments.map { it.toPersistedTypeRef(context) },
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
            qualifiers = qualifiers.map { it.toAnnotationRef(context, trace) },
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
    val qualifiers: List<PersistedAnnotationRef>,
    val primaryConstructorPropertyParameters: List<String>,
    val forTypeKeyTypeParameters: List<String>
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
    qualifiers = qualifiers.map { it.toPersistedAnnotationRef(context) },
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters
        .map { it.asString() },
    forTypeKeyTypeParameters = forTypeKeyTypeParameters
        .map { it.asString() }
)

fun PersistedClassifierRef.toClassifierRef(
    context: InjektContext,
    trace: BindingTrace?
): ClassifierRef {
    return context.classifierDescriptorForKey(key, trace)
        .toClassifierRef(context, trace)
        .let { raw ->
            if (superTypes.isNotEmpty() || qualifiers.isNotEmpty() || primaryConstructorPropertyParameters.isNotEmpty())
                raw.copy(
                    superTypes = superTypes.map { it.toTypeRef(context, trace) },
                    qualifiers = qualifiers.map { it.toAnnotationRef(context, trace) },
                    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters
                        .map { it.asNameId() }
                )
            else raw
        }
}

fun PersistedClassifierRef.toPersistedClassifierInfo() = PersistedClassifierInfo(
    fqName = key.split(":")[1],
    qualifiers = qualifiers,
    superTypes = superTypes,
    primaryConstructorPropertyParameters = primaryConstructorPropertyParameters,
    forTypeKeyTypeParameters = forTypeKeyTypeParameters
)

fun ClassifierRef.apply(
    context: InjektContext,
    trace: BindingTrace?,
    info: PersistedClassifierInfo?
): ClassifierRef {
    return if (info == null || !descriptor!!.isExternalDeclaration()) this
    else copy(
        qualifiers = info.qualifiers.map { it.toAnnotationRef(context, trace) },
        superTypes = info.superTypes.map { it.toTypeRef(context, trace) },
        primaryConstructorPropertyParameters = info.primaryConstructorPropertyParameters.map { it.asNameId() },
        forTypeKeyTypeParameters = info.forTypeKeyTypeParameters.map { it.asNameId() }
    )
}


@JsonClass(generateAdapter = true) data class PersistedAnnotationRef(
    val type: PersistedTypeRef,
    val arguments: Map<String, PersistedConstantValue>
)

fun AnnotationRef.toPersistedAnnotationRef(context: InjektContext): PersistedAnnotationRef {
    return PersistedAnnotationRef(
        type = type.toPersistedTypeRef(context),
        arguments = arguments
            .mapKeys { it.key.asString() }
            .mapValues { it.value.toPersistedConstantValue(context) }
    )
}

fun PersistedAnnotationRef.toAnnotationRef(
    context: InjektContext,
    trace: BindingTrace?
) = AnnotationRef(
    type.toTypeRef(context, trace),
    arguments
        .mapKeys { it.key.asNameId() }
        .mapValues { it.value.toConstantValue(context, trace) }
)

fun ConstantValue<*>.toPersistedConstantValue(
    context: InjektContext
): PersistedConstantValue = when (this) {
    is ArrayValue -> PersistedArrayValue(
        value.map { it.toPersistedConstantValue(context) },
        type.arguments.single().toPersistedTypeRef(context)
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
        type.toPersistedTypeRef(context)
    )
    is LongValue -> PersistedLongValue(value)
    is ShortValue -> PersistedShortValue(value)
    is StringValue -> PersistedStringValue(value)
    is UByteValue -> PersistedUByteValue(value)
    is UIntValue -> PersistedUIntValue(value)
    is ULongValue -> PersistedULongValue(value)
    is UShortValue -> PersistedUShortValue(value)
}

fun PersistedConstantValue.toConstantValue(
    context: InjektContext,
    trace: BindingTrace?
): ConstantValue<*> = when (this) {
    is PersistedArrayValue -> ArrayValue(
        value = value.map { it.toConstantValue(context, trace) },
        type = context.module.builtIns.array
            .defaultType
            .toTypeRef(context, trace)
            .typeWith(listOf(elementType.toTypeRef(context, trace)))
    )
    is PersistedBooleanValue -> BooleanValue(
        value,
        context.module.builtIns.booleanType.toTypeRef(context, trace)
    )
    is PersistedByteValue -> ByteValue(
        value,
        context.module.builtIns.byteType.toTypeRef(context, trace)
    )
    is PersistedCharValue -> CharValue(
        value,
        context.module.builtIns.charType.toTypeRef(context, trace)
    )
    is PersistedDoubleValue -> DoubleValue(
        value,
        context.module.builtIns.doubleType.toTypeRef(context, trace)
    )
    is PersistedEnumValue -> {
        val enumClassifier = context.classifierDescriptorForFqName(FqName(enumClassifierFqName))!!
            .toClassifierRef(context, trace)
        EnumValue(enumClassifier to enumValue.asNameId(), enumClassifier.defaultType)
    }
    is PersistedFloatValue -> FloatValue(
        value,
        context.module.builtIns.floatType.toTypeRef(context, trace)
    )
    is PersistedIntValue -> IntValue(
        value,
        context.module.builtIns.intType.toTypeRef(context, trace)
    )
    is PersistedKClassValue -> KClassValue(
        context.classifierDescriptorForFqName(FqName(classifierFqName))!!
            .toClassifierRef(context, trace),
        type.toTypeRef(context, trace)
    )
    is PersistedLongValue -> LongValue(
        value,
        context.module.builtIns.longType.toTypeRef(context, trace)
    )
    is PersistedShortValue -> ShortValue(
        value,
        context.module.builtIns.shortType.toTypeRef(context, trace)
    )
    is PersistedStringValue -> StringValue(
        value,
        context.module.builtIns.stringType.toTypeRef(context, trace)
    )
    is PersistedUByteValue -> UByteValue(
        value,
        context.classifierDescriptorForFqName(FqName("kotlin.UByte"))!!
            .toClassifierRef(context, trace)
            .defaultType
    )
    is PersistedUIntValue -> UIntValue(
        value,
        context.classifierDescriptorForFqName(FqName("kotlin.UInt"))!!
            .toClassifierRef(context, trace)
            .defaultType
    )
    is PersistedULongValue -> ULongValue(
        value,
        context.classifierDescriptorForFqName(FqName("kotlin.ULong"))!!
            .toClassifierRef(context, trace)
            .defaultType
    )
    is PersistedUShortValue -> UShortValue(
        value,
        context.classifierDescriptorForFqName(FqName("kotlin.UShort"))!!
            .toClassifierRef(context, trace)
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
