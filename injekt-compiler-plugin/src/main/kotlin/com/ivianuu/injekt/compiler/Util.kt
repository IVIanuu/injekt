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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

object InjektClassNames {
    private fun FqName.child(name: String) = child(Name.identifier(name))
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InjektInternalPackage = InjektPackage.child("internal")
    val InjektComponentsPackage = InjektInternalPackage.child("components")

    val Component = InjektPackage.child("Component")
    val ComponentMetadata = InjektInternalPackage.child("ComponentMetadata")
    val Qualifier = InjektPackage.child("Qualifier")
    val Module = InjektPackage.child("Module")
    val ModuleMetadata = InjektInternalPackage.child("ModuleMetadata")
    val Provider = InjektPackage.child("Provider")
    val ProviderDsl = InjektPackage.child("ProviderDsl")
    val ProviderMetadata = InjektInternalPackage.child("ProviderMetadata")
    val SingleProvider = InjektInternalPackage.child("SingleProvider")
}

fun ModuleDescriptor.getTopLevelClass(fqName: FqName) =
    findClassAcrossModuleDependencies(ClassId.topLevel(fqName))
        ?: error("No class found for $fqName")

internal lateinit var messageCollector: MessageCollector

fun message(
    message: String,
    tag: String = "ddd",
    severity: CompilerMessageSeverity = CompilerMessageSeverity.WARNING
) {
    messageCollector.report(severity, "$tag: $message")
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

fun <T : IrSymbol> T.ensureBound(irProviders: List<IrProvider>): T {
    if (!this.isBound) irProviders.forEach { it.getDeclaration(this) }
    check(this.isBound) { "$this is not bound" }
    return this
}

fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
    any { it.symbol.descriptor.constructedClass.fqNameSafe == fqName }

fun getComponentFqName(
    expression: IrExpression,
    file: IrFile
): FqName {
    return FqName(
        "${file.fqName.takeIf { it != FqName.ROOT }?.asString()?.let { "$it." }
            .orEmpty()}Component${
        (file.name.removeSuffix(".kt") + expression.startOffset).hashCode()
            .toString()
            .removeIllegalChars()
        }"
    )
}

fun getModuleName(
    function: FunctionDescriptor
): FqName {
    return FqName(function.fqNameSafe.asString() + "\$Impl")
}

fun makeModuleAnnotation(module: ModuleDescriptor): AnnotationDescriptor =
    object : AnnotationDescriptor {
        override val type: KotlinType
            get() = module.findClassAcrossModuleDependencies(
                ClassId.topLevel(InjektClassNames.Module)
            )!!.defaultType
        override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
        override val source: SourceElement get() = SourceElement.NO_SOURCE
        override fun toString() = "[@Module]"
    }

fun KotlinType.makeModule(module: ModuleDescriptor): KotlinType {
    if (hasModuleAnnotation()) return this
    val annotation = makeModuleAnnotation(module)
    return replaceAnnotations(Annotations.create(annotations + annotation))
}

fun KotlinType.hasModuleAnnotation(): Boolean =
    !isSpecialType && annotations.findAnnotation(InjektClassNames.Module) != null

fun Annotated.hasModuleAnnotation(): Boolean =
    annotations.findAnnotation(InjektClassNames.Module) != null

internal val KotlinType.isSpecialType: Boolean
    get() =
        this === TypeUtils.NO_EXPECTED_TYPE || this === TypeUtils.UNIT_EXPECTED_TYPE

val AnnotationDescriptor.isModuleAnnotation: Boolean get() = fqName == InjektClassNames.Module

fun KotlinType.asTypeName(): TypeName? {
    if (isError) return null
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
