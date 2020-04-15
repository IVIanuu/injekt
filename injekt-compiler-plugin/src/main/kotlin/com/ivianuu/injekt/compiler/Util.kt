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
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object InjektClassNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InjektInternalPackage = FqName("com.ivianuu.injekt.internal")

    val ApplicationScope = FqName("com.ivianuu.injekt.ApplicationScope")
    val Behavior = FqName("com.ivianuu.injekt.Behavior")
    val Component = FqName("com.ivianuu.injekt.Component")
    val Injekt = FqName("com.ivianuu.injekt.Injekt")
    val Key = FqName("com.ivianuu.injekt.Key")
    val ComponentDsl = FqName("com.ivianuu.injekt.ComponentDsl")
    val Module = FqName("com.ivianuu.injekt.Module")
    val ModuleImpl = FqName("com.ivianuu.injekt.ModuleImpl")
    val Param = FqName("com.ivianuu.injekt.Param")
    val Parameters = FqName("com.ivianuu.injekt.Parameters")
    val Qualifier = FqName("com.ivianuu.injekt.Qualifier")
    val Scope = FqName("com.ivianuu.injekt.Scope")
    val ModuleDsl = FqName("com.ivianuu.injekt.Module")
    val Provider = FqName("com.ivianuu.injekt.Provider")
    val ProviderDsl = FqName("com.ivianuu.injekt.ProviderDsl")
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

fun AnnotationDescriptor.getCompanionForTypeAnnotation(): ClassDescriptor? {
    return annotationClass?.companionObjectDescriptor ?: type.constructor.declarationDescriptor
        .safeAs<ClassDescriptor>()?.companionObjectDescriptor
}

fun DeclarationDescriptor.getTypeAnnotationCompanionsOfType(
    type: KotlinType
): List<ClassDescriptor> {
    return annotations
        .mapNotNull { it.getCompanionForTypeAnnotation() }
        .filter { it.defaultType.isSubtypeOf(type) }
}

fun DeclarationDescriptor.getTypeAnnotationsForType(
    type: KotlinType
): List<AnnotationDescriptor> {
    return annotations
        .filter {
            it.getCompanionForTypeAnnotation()
                ?.defaultType?.isSubtypeOf(type) == true
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

fun keyHash(
    type: KotlinType,
    qualifierType: KotlinType? = null
): Int {
    return type.hashCode() + (qualifierType?.hashCode() ?: 0)
}