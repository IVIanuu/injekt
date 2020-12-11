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

import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.js.translate.utils.refineType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
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
    fqName: FqName,
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
    fqName: FqName,
): Boolean = hasAnnotation(fqName) ||
        (this is PropertyAccessorDescriptor && correspondingProperty.hasAnnotation(fqName)) ||
        (this is ConstructorDescriptor && constructedClass.hasAnnotation(fqName))

fun ClassDescriptor.getGivenConstructor(): ConstructorDescriptor? {
    constructors
        .firstOrNull {
            it.hasAnnotation(InjektFqNames.Given)
        }?.let { return it }
    if (hasAnnotation(InjektFqNames.Given)) return unsubstitutedPrimaryConstructor
    return null
}

fun KtClassOrObject.getGivenConstructor(): KtConstructor<*>? {
    (listOfNotNull(primaryConstructor) + secondaryConstructors)
        .firstOrNull {
            it.hasAnnotation(InjektFqNames.Given)
        }?.let { return it }
    if (hasAnnotation(InjektFqNames.Given)) return primaryConstructor
    return null
}

fun DeclarationDescriptor.isExternalDeclaration(): Boolean = this is DeserializedDescriptor ||
        (this is PropertyAccessorDescriptor && correspondingProperty.isExternalDeclaration())

fun String.asNameId() = Name.identifier(this)

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

fun ClassDescriptor.extractGivensOfDeclaration(
    bindingContext: BindingContext,
    declarationStore: DeclarationStore,
): List<Pair<TypeRef, CallableDescriptor>> {
    val primaryConstructorGivens = (unsubstitutedPrimaryConstructor
        ?.let { primaryConstructor ->
            val info = declarationStore.givenInfoFor(primaryConstructor)
            primaryConstructor.valueParameters
                .filter {
                    it.hasAnnotation(InjektFqNames.Given) ||
                            it.name in info.allGivens
                }
                .mapNotNull { bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, it] }
                .map { it.type.toTypeRef() to it }
        }
        ?: emptyList())

    val memberGivens = unsubstitutedMemberScope.getContributedDescriptors()
        .flatMap { declaration ->
            when (declaration) {
                is ClassDescriptor -> declaration.getGivenConstructor()
                    ?.let { constructor ->
                        declaration.allGivenTypes()
                            .map { it to constructor }
                    } ?: emptyList()
                is PropertyDescriptor -> if (declaration.hasAnnotation(InjektFqNames.Given))
                    listOf(declaration.returnType!!.toTypeRef() to declaration) else emptyList()
                is FunctionDescriptor -> if (declaration.hasAnnotation(InjektFqNames.Given))
                    listOf(declaration.returnType!!.toTypeRef() to declaration) else emptyList()
                else -> emptyList()
            }
        }

    return primaryConstructorGivens + memberGivens
}

fun ClassDescriptor.extractGivenCollectionElementsOfDeclaration(bindingContext: BindingContext): List<Pair<TypeRef, CallableDescriptor>> {
    val primaryConstructorGivens = (unsubstitutedPrimaryConstructor
        ?.let { primaryConstructor ->
            primaryConstructor.valueParameters
                .filter {
                    it.hasAnnotation(InjektFqNames.GivenMap) ||
                            it.hasAnnotation(InjektFqNames.GivenSet)
                }
                .mapNotNull { bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, it] }
                .map { it.type.toTypeRef() to it }
        }
        ?: emptyList())

    val memberGivens = unsubstitutedMemberScope.getContributedDescriptors()
        .flatMap { declaration ->
            when (declaration) {
                is PropertyDescriptor -> if (declaration.hasAnnotation(InjektFqNames.GivenMap) ||
                    declaration.hasAnnotation(InjektFqNames.GivenSet)
                )
                    listOf(declaration.returnType!!.toTypeRef() to declaration) else emptyList()
                is FunctionDescriptor -> if (declaration.hasAnnotation(InjektFqNames.GivenMap) ||
                    declaration.hasAnnotation(InjektFqNames.GivenSet)
                )
                    listOf(declaration.returnType!!.toTypeRef() to declaration) else emptyList()
                else -> emptyList()
            }
        }

    return primaryConstructorGivens + memberGivens
}

fun ConstructorDescriptor.allGivenTypes(): List<TypeRef> =
    constructedClass.allGivenTypes()

fun ClassDescriptor.allGivenTypes(): List<TypeRef> = buildList<TypeRef> {
    this += defaultType.toTypeRef()
    this += defaultType.constructor.supertypes
        .filter { it.hasAnnotation(InjektFqNames.Given) }
        .map { it.toTypeRef() }
}

fun CallableDescriptor.extractGivensOfCallable(
    declarationStore: DeclarationStore,
): List<CallableDescriptor> {
    val info = declarationStore.givenInfoFor(this)
    return allParameters
        .filter {
            it.hasAnnotation(InjektFqNames.Given) ||
                    it.type.hasAnnotation(InjektFqNames.Given) ||
                    it.name in info.allGivens
        }
}

fun CallableDescriptor.extractGivenCollectionElementsOfCallable(): List<CallableDescriptor> =
    allParameters
        .filter {
            it.hasAnnotation(InjektFqNames.GivenMap) ||
                    it.hasAnnotation(InjektFqNames.GivenSet) ||
                    it.hasAnnotation(InjektFqNames.GivenSet) ||
                    it.type.hasAnnotation(InjektFqNames.GivenSet)
        }
