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
import org.jetbrains.kotlin.backend.jvm.ir.propertyIfAccessor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.IrProvider
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.utils.addToStdlib.cast

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

fun <T : IrSymbol> T.ensureBound(irProviders: List<IrProvider>): T {
    if (!this.isBound) irProviders.forEach { it.getDeclaration(this) }
    check(this.isBound) { "$this is not bound" }
    return this
}

fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
    any { it.symbol.descriptor.constructedClass.fqNameSafe == fqName }

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
