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

package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.generator.componentimpl.ComponentExpression
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.ByteValue
import org.jetbrains.kotlin.resolve.constants.CharValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.DoubleValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.FloatValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.constants.LongValue
import org.jetbrains.kotlin.resolve.constants.ShortValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.constants.UByteValue
import org.jetbrains.kotlin.resolve.constants.UIntValue
import org.jetbrains.kotlin.resolve.constants.ULongValue
import org.jetbrains.kotlin.resolve.constants.UShortValue

data class Callable(
    val packageFqName: FqName,
    val fqName: FqName,
    val name: Name,
    val type: TypeRef,
    val originalType: TypeRef = type,
    val typeParameters: List<ClassifierRef>,
    val valueParameters: List<ValueParameterRef>,
    val targetComponent: TypeRef?,
    val scoped: Boolean,
    val eager: Boolean,
    val default: Boolean,
    val contributionKind: ContributionKind?,
    val isCall: Boolean,
    val callableKind: CallableKind,
    val isInline: Boolean,
    val visibility: DescriptorVisibility,
    val modality: Modality,
) {
    enum class ContributionKind {
        BINDING, INTERCEPTOR, MAP_ENTRIES, MODULE, SET_ELEMENTS
    }

    enum class CallableKind {
        DEFAULT, SUSPEND, COMPOSABLE
    }
}

data class ValueParameterRef(
    val type: TypeRef,
    val originalType: TypeRef,
    val parameterKind: ParameterKind,
    val name: Name,
    val hasDefault: Boolean,
    val defaultExpression: ComponentExpression?,
) {
    enum class ParameterKind {
        VALUE_PARAMETER, DISPATCH_RECEIVER, EXTENSION_RECEIVER
    }
}

data class ModuleDescriptor(
    val type: TypeRef,
    val callables: List<Callable>,
)

data class QualifierDescriptor(
    val type: TypeRef,
    val args: Map<Name, String>,
)

private fun AnnotationDescriptor.valueArgsForAnnotation(): Map<Name, ComponentExpression> {
    return allValueArguments.mapValues { (_, arg) ->
        {
            fun ConstantValue<*>.emit() {
                when (this) {
                    is ArrayValue -> {
                        // todo avoid boxing
                        emit("arrayOf(")
                        value.forEachIndexed { index, itemValue ->
                            itemValue.emit()
                            if (index != value.lastIndex) emit(", ")
                        }
                        emit(")")
                    }
                    is BooleanValue -> emit(value)
                    is ByteValue -> emit("$value")
                    is CharValue -> emit("'${value}'")
                    is DoubleValue -> emit("$value")
                    is EnumValue -> emit("${enumClassId.asSingleFqName()}.${enumEntryName}")
                    is FloatValue -> emit("${value}f")
                    is IntValue -> emit("$value")
                    is KClassValue -> emit("${(value as KClassValue.Value.NormalClass).classId.asSingleFqName()}::class")
                    is LongValue -> emit("${value}L")
                    is ShortValue -> emit("$value")
                    is StringValue -> emit("\"${value}\"")
                    is UByteValue -> emit("${value}u")
                    is UIntValue -> emit("${value}u")
                    is ULongValue -> emit("(${value}UL)")
                    is UShortValue -> emit("${value}u")
                    else -> error("Unsupported bindingArg type $value")
                }.let {}
            }

            arg.emit()
        }
    }
}

fun AnnotationDescriptor.toQualifierDescriptor(): QualifierDescriptor {
    return QualifierDescriptor(
        type = type.toTypeRef(),
        args = valueArgsForAnnotation()
            .mapValues { buildCodeString { it.value(this) } }
    )
}

data class Index(val fqName: FqName, val type: String)
