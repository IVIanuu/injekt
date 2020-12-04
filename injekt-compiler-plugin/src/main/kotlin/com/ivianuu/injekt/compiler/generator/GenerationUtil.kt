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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.js.translate.utils.refineType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.upperIfFlexible

internal fun KtAnnotated.hasAnnotation(fqName: FqName): Boolean = findAnnotation(fqName) != null

fun KtAnnotated.findAnnotation(fqName: FqName): KtAnnotationEntry? {
    val annotationEntries = annotationEntries
    if (annotationEntries.isEmpty()) return null

    // Check if the fully qualified name is used, e.g. `@dagger.Module`.
    val annotationEntry = annotationEntries.firstOrNull {
        it.text.startsWith("@${fqName.asString()}")
    }
    if (annotationEntry != null) return annotationEntry

    // Check if the simple name is used, e.g. `@Module`.
    val annotationEntryShort = annotationEntries
        .firstOrNull {
            it.shortName == fqName.shortName()
        }
        ?: return null

    val importPaths = containingKtFile.importDirectives.mapNotNull { it.importPath }

    // If the simple name is used, check that the annotation is imported.
    val hasImport = importPaths.any { it.fqName == fqName }
    if (hasImport) return annotationEntryShort

    // Look for star imports and make a guess.
    val hasStarImport = importPaths
        .filter { it.isAllUnder }
        .any {
            fqName.asString().startsWith(it.fqName.asString())
        }
    if (hasStarImport) return annotationEntryShort

    val isSamePackage = fqName.parent() == annotationEntryShort.containingKtFile.packageFqName
    if (isSamePackage) return annotationEntryShort

    return null
}

fun KtAnnotated.hasAnnotationWithPropertyAndClass(
    fqName: FqName
): Boolean = hasAnnotation(fqName) ||
        (this is KtPropertyAccessor && property.hasAnnotation(fqName)) ||
        (this is KtConstructor<*> && containingClassOrObject!!.hasAnnotation(fqName))

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

fun Annotated.hasAnnotationWithPropertyAndClass(
    fqName: FqName
): Boolean = hasAnnotation(fqName) ||
    (this is PropertyAccessorDescriptor && correspondingProperty.hasAnnotation(fqName)) ||
    (this is ConstructorDescriptor && constructedClass.hasAnnotation(fqName))

fun ClassDescriptor.getInjectConstructor(): ConstructorDescriptor? {
    if (hasAnnotation(InjektFqNames.Binding) ||
        hasAnnotation(InjektFqNames.Module)) return unsubstitutedPrimaryConstructor
    constructors
        .firstOrNull {
            it.hasAnnotation(InjektFqNames.Binding) ||
                    it.hasAnnotation(InjektFqNames.Module)
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

fun Callable.substitute(map: Map<ClassifierRef, TypeRef>): Callable {
    return copy(
        type = type.substitute(map),
        valueParameters = valueParameters.map {
            it.copy(type = it.type.substitute(map))
        }
    )
}

fun QualifierDescriptor.substitute(
    map: Map<ClassifierRef, TypeRef>
): QualifierDescriptor = copy(type = type.substitute(map))

fun Annotated.contributionKind() = when {
    hasAnnotationWithPropertyAndClass(InjektFqNames.Binding) -> Callable.ContributionKind.BINDING
    hasAnnotationWithPropertyAndClass(InjektFqNames.Interceptor) -> Callable.ContributionKind.INTERCEPTOR
    hasAnnotationWithPropertyAndClass(InjektFqNames.MapEntries) -> Callable.ContributionKind.MAP_ENTRIES
    hasAnnotationWithPropertyAndClass(InjektFqNames.SetElements) -> Callable.ContributionKind.SET_ELEMENTS
    hasAnnotationWithPropertyAndClass(InjektFqNames.Module) -> Callable.ContributionKind.MODULE
    else -> null
}

fun Annotated.targetComponent(
    module: ModuleDescriptor,
    typeTranslator: TypeTranslator
) = (annotations
    .findAnnotation(InjektFqNames.Scoped)
    ?: annotations.findAnnotation(InjektFqNames.Bound))
    ?.allValueArguments
    ?.let { it["component".asNameId()] }
    ?.let { it as KClassValue }
    ?.getArgumentType(module)
    ?.let { typeTranslator.toTypeRef(it, null as KtFile?, Variance.INVARIANT) }
