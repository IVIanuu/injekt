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

import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.js.translate.utils.refineType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.upperIfFlexible

fun <D : DeclarationDescriptor> KtDeclaration.descriptor(
    bindingContext: BindingContext,
) = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? D

fun KotlinType.prepare(): KotlinType {
    var tmp = refineType()
    if (constructor is IntersectionTypeConstructor) {
        tmp = CommonSupertypes.commonSupertype(constructor.supertypes)
    }
    tmp = tmp.upperIfFlexible()
    return tmp
}

fun DeclarationDescriptor.hasAnnotationWithPropertyAndClass(
    fqName: FqName
): Boolean = hasAnnotation(fqName) ||
    (this is PropertyAccessorDescriptor && correspondingProperty.hasAnnotation(fqName)) ||
    (this is ConstructorDescriptor && constructedClass.hasAnnotation(fqName))

fun ClassDescriptor.getInjectConstructor(): ConstructorDescriptor? {
    if (hasAnnotation(InjektFqNames.Binding) ||
        hasAnnotation(InjektFqNames.ImplBinding) ||
        hasAnnotatedAnnotations(InjektFqNames.BindingAdapter)) return unsubstitutedPrimaryConstructor
    constructors
        .firstOrNull {
            it.hasAnnotation(InjektFqNames.Binding) ||
                    it.hasAnnotation(InjektFqNames.ImplBinding) ||
                    it.hasAnnotatedAnnotations(InjektFqNames.BindingAdapter)
        }?.let { return it }
    return null
}

fun String.asNameId() = Name.identifier(this)

fun FqName.toComponentImplFqName() =
    FqName("${asString()}Impl")

fun <T> unsafeLazy(init: () -> T) = lazy(LazyThreadSafetyMode.NONE, init)

fun String.removeIllegalChars() =
    replace(".", "")
        .replace("<", "")
        .replace(">", "")
        .replace(" ", "")
        .replace("[", "")
        .replace("]", "")
        .replace("@", "")
        .replace(",", "")
        .replace(" ", "")
        .replace("-", "")

fun Annotated.hasAnnotation(fqName: FqName): Boolean =
    annotations.hasAnnotation(fqName)

fun FunctionDescriptor.getBindingFunctionType(): KotlinType {
    val assistedParameters =
        (listOfNotNull(extensionReceiverParameter) + valueParameters)
            .filter { it.type.hasAnnotation(InjektFqNames.Assisted) }
            .map { it.type }
    return (
            if (isSuspend) builtIns.getSuspendFunction(assistedParameters.size)
            else builtIns.getFunction(assistedParameters.size)
            )
        .defaultType
        .replace(newArguments = assistedParameters.map { it.asTypeProjection() } + returnType!!.asTypeProjection())
}

fun AnnotationDescriptor.hasAnnotation(annotation: FqName): Boolean =
    type.constructor.declarationDescriptor!!.hasAnnotation(annotation)

fun Annotated.hasAnnotatedAnnotations(
    annotation: FqName
): Boolean = annotations.any { it.hasAnnotation(annotation) }

fun Annotated.getAnnotatedAnnotations(annotation: FqName): List<AnnotationDescriptor> =
    annotations.filter { it.hasAnnotation(annotation) }

fun joinedNameOf(
    packageFqName: FqName,
    fqName: FqName
): Name {
    val joinedSegments = fqName.asString()
        .removePrefix(packageFqName.asString() + ".")
        .split(".")
    return joinedSegments.joinToString("_").asNameId()
}
