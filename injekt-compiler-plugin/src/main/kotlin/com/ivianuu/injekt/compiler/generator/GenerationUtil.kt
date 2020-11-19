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
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.js.translate.utils.refineType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
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

fun DeclarationDescriptor.hasAnnotatedAnnotationsWithPropertyAndClass(
    fqName: FqName
): Boolean = hasAnnotatedAnnotations(fqName) ||
        (this is PropertyAccessorDescriptor && correspondingProperty.hasAnnotatedAnnotations(fqName)) ||
        (this is ConstructorDescriptor && constructedClass.hasAnnotatedAnnotations(fqName))

fun ClassDescriptor.getInjectConstructor(): ConstructorDescriptor? {
    if (hasAnnotation(InjektFqNames.Binding) ||
        hasAnnotation(InjektFqNames.ImplBinding) ||
        hasAnnotatedAnnotations(InjektFqNames.Decorator) ||
        hasAnnotatedAnnotations(InjektFqNames.Effect)) return unsubstitutedPrimaryConstructor
    constructors
        .firstOrNull {
            it.hasAnnotation(InjektFqNames.Binding) ||
                    it.hasAnnotation(InjektFqNames.ImplBinding) ||
                    it.hasAnnotatedAnnotations(InjektFqNames.Decorator) ||
                    it.hasAnnotatedAnnotations(InjektFqNames.Effect)
        }?.let { return it }
    return null
}

fun DeclarationDescriptor.getArgName(): Name? =
    (annotations.findAnnotation(InjektFqNames.Arg)
        ?.allValueArguments?.values?.single()?.value as? String)?.asNameId()

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

fun AnnotationDescriptor.hasAnnotation(annotation: FqName): Boolean =
    type.constructor.declarationDescriptor!!.hasAnnotation(annotation)

fun Annotated.hasAnnotatedAnnotations(
    annotation: FqName
): Boolean {
    return annotations.any {
        val inner = it.type.constructor.declarationDescriptor as ClassDescriptor
        inner.hasAnnotation(annotation)
    }
}

fun Annotated.getAnnotatedAnnotations(annotation: FqName): List<AnnotationDescriptor> =
    annotations.filter {
        val inner = it.type.constructor.declarationDescriptor as ClassDescriptor
        inner.hasAnnotation(annotation)
    }

fun joinedNameOf(
    packageFqName: FqName,
    fqName: FqName
): Name {
    val joinedSegments = fqName.asString()
        .removePrefix(packageFqName.asString() + ".")
        .split(".")
    return joinedSegments.joinToString("_").asNameId()
}

val TypeRef.callableKind: Callable.CallableKind get() = when {
    fullyExpandedType.isSuspendFunction -> Callable.CallableKind.SUSPEND
    fullyExpandedType.isComposable -> Callable.CallableKind.COMPOSABLE
    else -> Callable.CallableKind.DEFAULT
}

fun Callable.substitute(map: Map<TypeRef, TypeRef>): Callable {
    return copy(
        type = type.substitute(map),
        effectType = effectType.substitute(map),
        valueParameters = valueParameters.map {
            it.copy(type = it.type.substitute(map))
        },
        effects = effects.map { it.substitute(map) },
        typeArgs = typeArgs.mapValues { it.value.substitute(map) }
    )
}

fun Callable.substituteInputs(map: Map<TypeRef, TypeRef>): Callable {
    return copy(
        valueParameters = valueParameters.map {
            it.copy(type = it.type.substitute(map))
        }
    )
}

fun Callable.newEffect(effect: Int): Callable {
    val newType = type.copy(effect = effect)
    val newEffectType = effectType.copy(effect = effect)
    val map = mapOf(type to newType, effectType to newEffectType)
    return copy(
        effectType = newEffectType,
        effects = effects.map { it.substituteInputs(map) }
    )
}

fun QualifierDescriptor.substitute(
    map: Map<TypeRef, TypeRef>
): QualifierDescriptor = copy(type = type.substitute(map))
