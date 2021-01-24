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

import com.ivianuu.injekt.compiler.analysis.GivenFunctionDescriptor
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.js.translate.utils.refineType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
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

fun CallableDescriptor.getGivenParameters(
    declarationStore: DeclarationStore,
    substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap()
): List<ParameterDescriptor> =
    getContributionParameters(declarationStore, substitutionMap)
        .filter { it.contributionKind == ContributionKind.VALUE }
        .map { it.callable as ParameterDescriptor }

fun CallableDescriptor.getContributionParameters(
    declarationStore: DeclarationStore,
    substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap()
): List<CallableRef> =
    allParameters
        .mapNotNull { parameter ->
            val kind = parameter.contributionKind(declarationStore) ?: if (substitutionMap.isNotEmpty()) {
                parameter.type.toTypeRef(declarationStore).substitute(substitutionMap)
                    .contributionKind
            } else parameter.type.contributionKind(declarationStore)
            if (kind != null) parameter.toCallableRef(declarationStore)
                .copy(contributionKind = kind)
            else null
        }

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

fun DeclarationDescriptor.isExternalDeclaration(): Boolean = this is DeserializedDescriptor ||
        (this is PropertyAccessorDescriptor && correspondingProperty.isExternalDeclaration()) ||
        (this is GivenFunctionDescriptor && invokeDescriptor.isExternalDeclaration()) ||
        this is DeserializedTypeParameterDescriptor

val isIde: Boolean = Project::class.java.name == "com.intellij.openapi.project.Project"
val isCli: Boolean =
    !isIde && Project::class.java.name == "org.jetbrains.kotlin.com.intellij.openapi.project.Project"

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

fun IrType.getAnnotatedAnnotations(annotation: FqName): List<IrConstructorCall> =
    annotations.filter {
        val inner = it.type.classOrNull!!.owner
        inner.hasAnnotation(annotation)
    }

fun DeclarationDescriptor.uniqueKey(declarationStore: DeclarationStore): String {
    val original = this.original
    return when (original) {
        is ConstructorDescriptor -> "constructor:${original.constructedClass.fqNameSafe}:${
            original.valueParameters
                .joinToString(",") {
                    it.type
                        .toTypeRef(declarationStore)
                        .fullyExpandedType
                        .uniqueTypeName()
                }
        }"
        is ClassDescriptor -> "class:$fqNameSafe"
        is FunctionDescriptor -> "function:$fqNameSafe:${
            listOfNotNull(
                original.dispatchReceiverParameter, original.extensionReceiverParameter)
                .plus(original.valueParameters)
                .joinToString(",") { 
                    it.type
                        .toTypeRef(declarationStore)
                        .fullyExpandedType
                        .uniqueTypeName()
                }
        }"
        is PropertyDescriptor -> "property:$fqNameSafe:${
            listOfNotNull(
                original.dispatchReceiverParameter, original.extensionReceiverParameter)
                .joinToString(",") { 
                    it.type
                        .toTypeRef(declarationStore)
                        .fullyExpandedType
                        .uniqueTypeName()
                }
        }"
        is TypeAliasDescriptor -> "typealias:$fqNameSafe"
        is TypeParameterDescriptor ->
            "typeparameter:$fqNameSafe:${containingDeclaration!!.uniqueKey(declarationStore)}"
        is ParameterDescriptor -> ""
        is ValueParameterDescriptor -> ""
        is VariableDescriptor -> ""
        else -> error("Unexpected declaration $this")
    }
}

fun ParameterDescriptor.injektName(): String {
    val callable = containingDeclaration as? CallableDescriptor
    return when {
        original == callable?.dispatchReceiverParameter?.original ||
                (this is ReceiverParameterDescriptor && containingDeclaration is ClassDescriptor)-> "_dispatchReceiver"
        original == callable?.extensionReceiverParameter?.original -> "_extensionReceiver"
        else -> {
            if (name.isSpecial)
                type.constructor.declarationDescriptor!!.name
                    .asString().decapitalize()
            else name.asString()
        }
    }
}

fun <P1> hashOf(p1: P1): Int {
    return p1.hashCode()
}

fun <P1, P2> hashOf(p1: P1, p2: P2): Int {
    var result = p1.hashCode()
    result = 31 * result + p2.hashCode()
    return result
}

fun <P1, P2, P3> hashOf(p1: P1, p2: P2, p3: P3): Int {
    var result = p1.hashCode()
    result = 31 * result + p2.hashCode()
    result = 31 * result + p3.hashCode()
    return result
}

fun <P1, P2, P3, P4> hashOf(p1: P1, p2: P2, p3: P3, p4: P4): Int {
    var result = p1.hashCode()
    result = 31 * result + p2.hashCode()
    result = 31 * result + p3.hashCode()
    result = 31 * result + p4.hashCode()
    return result
}

fun <P1, P2, P3, P4, P5> hashOf(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5): Int {
    var result = p1.hashCode()
    result = 31 * result + p2.hashCode()
    result = 31 * result + p3.hashCode()
    result = 31 * result + p4.hashCode()
    result = 31 * result + p5.hashCode()
    return result
}
