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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

object InjektClassNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val Behavior = FqName("com.ivianuu.injekt.Behavior")
    val Component = FqName("com.ivianuu.injekt.Component")
    val ComponentBuilder = FqName("com.ivianuu.injekt.ComponentBuilder")
    val DuplicateStrategy = FqName("com.ivianuu.injekt.DuplicateStrategy")
    val Injekt = FqName("com.ivianuu.injekt.Injekt")
    val InjektConstructor = FqName("com.ivianuu.injekt.InjektConstructor")
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

fun KotlinType.isFullyResolved(): Boolean =
    constructor.declarationDescriptor is ClassDescriptor && arguments.all { it.type.isFullyResolved() }

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
        val parameters = arguments.map { it.type.asTypeName() }
        if (parameters.any { it == null }) return null
        type.parameterizedBy(*parameters.toTypedArray().requireNoNulls())
    } else type).copy(
        nullable = isMarkedNullable
    )
}

fun AnnotationDescriptor.getPropertyForSyntheticAnnotation(
    module: ModuleDescriptor
): PropertyDescriptor {
    return module.getPackage(fqName!!.parent().parent())
        .memberScope
        .getContributedVariables(
            fqName!!.shortName(),
            NoLookupLocation.FROM_BACKEND
        )
        .single()
}

fun DeclarationDescriptor.getSyntheticAnnotationPropertiesOfType(
    type: KotlinType
): List<PropertyDescriptor> {
    return getAnnotatedAnnotations(InjektClassNames.SyntheticAnnotation)
        .map { it.getPropertyForSyntheticAnnotation(module) }
        .filter { it.type.isSubtypeOf(type) }
}
