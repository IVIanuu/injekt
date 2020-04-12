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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes.isUnsignedType
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

object InjektClassNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InjektInternalPackage = FqName("com.ivianuu.injekt.internal")

    val Behavior = FqName("com.ivianuu.injekt.Behavior")
    val BehaviorMarker = FqName("com.ivianuu.injekt.BehaviorMarker")
    val BindingProvider = FqName("com.ivianuu.injekt.BindingProvider")
    val Component = FqName("com.ivianuu.injekt.Component")
    val ComponentBuilder = FqName("com.ivianuu.injekt.ComponentBuilder")
    val DeclarationName = FqName("com.ivianuu.injekt.internal.DeclarationName")
    val DuplicateStrategy = FqName("com.ivianuu.injekt.DuplicateStrategy")
    val GenerateDsl = FqName("com.ivianuu.injekt.GenerateDsl")
    val Injekt = FqName("com.ivianuu.injekt.Injekt")
    val Key = FqName("com.ivianuu.injekt.Key")
    val KeyOverload = FqName("com.ivianuu.injekt.KeyOverload")
    val KeyOverloadStub = FqName("com.ivianuu.injekt.internal.KeyOverloadStub")
    val Module = FqName("com.ivianuu.injekt.Module")
    val ModuleMarker = FqName("com.ivianuu.injekt.ModuleMarker")
    val Param = FqName("com.ivianuu.injekt.Param")
    val Parameters = FqName("com.ivianuu.injekt.Parameters")
    val Qualifier = FqName("com.ivianuu.injekt.Qualifier")
    val QualifierMarker = FqName("com.ivianuu.injekt.QualifierMarker")
    val Scope = FqName("com.ivianuu.injekt.Scope")
    val ScopeMarker = FqName("com.ivianuu.injekt.ScopeMarker")
    val SyntheticAnnotation = FqName("com.ivianuu.injekt.internal.SyntheticAnnotation")
    val SyntheticAnnotationMarker = FqName("com.ivianuu.injekt.internal.SyntheticAnnotationMarker")
}

fun DeclarationDescriptor.hasAnnotatedAnnotations(annotation: FqName): Boolean =
    annotations.any { it.hasAnnotation(annotation, module) }

fun DeclarationDescriptor.getAnnotatedAnnotations(annotation: FqName): List<AnnotationDescriptor> =
    annotations.filter {
        it.hasAnnotation(annotation, module)
    }

fun AnnotationDescriptor.hasAnnotation(annotation: FqName, module: ModuleDescriptor): Boolean {
    val thisFqName = this.fqName ?: return false
    val descriptor =
        module.findClassAcrossModuleDependencies(ClassId.topLevel(thisFqName)) ?: return false
    return descriptor.annotations.hasAnnotation(annotation)
}

fun IrType.isFullyResolved(): Boolean =
    this is IrSimpleType && this.classifier is IrClassSymbol && arguments.all {
        it.typeOrNull?.isFullyResolved() == true
    }

internal lateinit var messageCollector: MessageCollector

fun message(
    message: String,
    tag: String = "ddd",
    severity: CompilerMessageSeverity = CompilerMessageSeverity.WARNING
) {
    messageCollector.report(severity, "$tag: $message")
}

fun FqName.asClassName(): ClassName = ClassName(parent().asString(), shortName().asString())

fun ClassDescriptor.asClassName(): ClassName? = try {
    ClassName.bestGuess(fqNameSafe.asString())
} catch (e: Exception) {
    null
}

fun KotlinType.asTypeName(): TypeName? {
    if (isError) return null
    if (this.isFunctionType) {
        return LambdaTypeName.get(
            receiver = getReceiverTypeFromFunctionType()?.asTypeName(),
            parameters = *getValueParameterTypesFromFunctionType()
                .map { it.type.asTypeName()!! }
                .toTypedArray(),
            returnType = getReturnTypeFromFunctionType().asTypeName()!!
        ).copy(nullable = isMarkedNullable)
    }
    if (this.isTypeParameter()) {
        val descriptor = constructor.declarationDescriptor as TypeParameterDescriptor
        return TypeVariableName(
            descriptor.name.asString(),
            *descriptor
                .upperBounds
                .map { it.asTypeName()!! }
                .toTypedArray(),
            variance = when (descriptor.variance) {
                Variance.INVARIANT -> null
                Variance.IN_VARIANCE -> KModifier.IN
                Variance.OUT_VARIANCE -> KModifier.OUT
            }
        ).copy(nullable = isMarkedNullable)
    }
    val type = try {
        ClassName.bestGuess(
            constructor.declarationDescriptor?.fqNameSafe?.asString() ?: return null
        )
    } catch (e: Exception) {
        return null
    }
    return (if (arguments.isNotEmpty()) {
        val parameters = arguments.map {
            if (it.isStarProjection) STAR else it.type.asTypeName()
        }
        if (parameters.any { it == null }) return null
        type.parameterizedBy(*parameters.toTypedArray().requireNoNulls())
    } else type).copy(
        nullable = isMarkedNullable
    )
}

fun AnnotationDescriptor.getDeclarationForSyntheticAnnotation(
    module: ModuleDescriptor
): DeclarationDescriptor {
    return module.getPackage(fqName!!.parent().parent())
        .memberScope
        .getContributedDescriptors(DescriptorKindFilter.ALL) { it == fqName!!.shortName() }
        .single { it.hasAnnotatedAnnotations(InjektClassNames.SyntheticAnnotationMarker) }
}

fun DeclarationDescriptor.getSyntheticAnnotationDeclarationsOfType(
    type: KotlinType
): List<DeclarationDescriptor> {
    return getAnnotatedAnnotations(InjektClassNames.SyntheticAnnotation)
        .map { it.getDeclarationForSyntheticAnnotation(module) }
        .filter {
            when (it) {
                is PropertyDescriptor -> it.type.isSubtypeOf(type)
                is FunctionDescriptor -> it.returnType!!.isSubtypeOf(type)
                else -> false
            }
        }
}

fun DeclarationDescriptor.getSyntheticAnnotationsForType(
    type: KotlinType
): List<AnnotationDescriptor> {
    return getAnnotatedAnnotations(InjektClassNames.SyntheticAnnotation)
        .filter {
            when (val declaration = it.getDeclarationForSyntheticAnnotation(module)) {
                is ClassDescriptor -> declaration.defaultType.isSubtypeOf(type)
                is PropertyDescriptor -> declaration.type.isSubtypeOf(type)
                is FunctionDescriptor -> declaration.returnType!!.isSubtypeOf(type)
                else -> false
            }
        }
}

fun String.removeIllegalChars(): String {
    return replace("<", "")
        .replace(">", "")
        .replace(" ", "")
        .replace(",", "")
        .replace("*", "")
        .replace(".", "")
        .replace("-", "")

}

fun KotlinType.isAcceptableTypeForAnnotationParameter(): Boolean {
    if (isError) return true
    val typeDescriptor =
        TypeUtils.getClassDescriptor(this) ?: return false
    if (DescriptorUtils.isEnumClass(typeDescriptor) ||
        DescriptorUtils.isAnnotationClass(typeDescriptor) ||
        KotlinBuiltIns.isKClass(typeDescriptor) ||
        KotlinBuiltIns.isPrimitiveArray(this) ||
        KotlinBuiltIns.isPrimitiveType(this) ||
        KotlinBuiltIns.isString(this) ||
        isUnsignedType(this)
    ) return true

    if (KotlinBuiltIns.isArray(this)) {
        val arguments = arguments
        if (arguments.size == 1) {
            val arrayType = arguments[0].type
            if (arrayType.isMarkedNullable) {
                return false
            }
            val arrayTypeDescriptor =
                TypeUtils.getClassDescriptor(arrayType)
            if (arrayTypeDescriptor != null) {
                return DescriptorUtils.isEnumClass(arrayTypeDescriptor) ||
                        DescriptorUtils.isAnnotationClass(
                            arrayTypeDescriptor
                        ) ||
                        KotlinBuiltIns.isKClass(arrayTypeDescriptor) ||
                        KotlinBuiltIns.isString(arrayType)
            }
        }
    }
    return false
}
