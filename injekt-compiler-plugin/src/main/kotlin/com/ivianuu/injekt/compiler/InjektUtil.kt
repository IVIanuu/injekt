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
import com.ivianuu.injekt.compiler.resolution.uniqueTypeName
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
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
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.types.upperIfFlexible

fun KtAnnotated.hasAnnotation(fqName: FqName): Boolean = findAnnotation(fqName) != null

fun KtAnnotated.findAnnotation(fqName: FqName): KtAnnotationEntry? {
    val annotationEntries = annotationEntries
    if (annotationEntries.isEmpty()) return null

    // Check if the fully qualified name is used, e.g. `@dagger.Module`.
    val annotationEntry = annotationEntries.firstOrNull {
        it.text.startsWith("@${fqName.asString()}")
    }
    if (annotationEntry != null) return annotationEntry

    // Check if the simple name is used, e.g. `@Given`.
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

fun <D : DeclarationDescriptor> KtDeclaration.descriptor(
    bindingContext: BindingContext,
) = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? D

fun DeclarationDescriptor.isExternalDeclaration(): Boolean = this is DeserializedDescriptor ||
        (this is PropertyAccessorDescriptor && correspondingProperty.isExternalDeclaration()) ||
        (this is GivenFunctionDescriptor && invokeDescriptor.isExternalDeclaration()) ||
        this is DeserializedTypeParameterDescriptor

fun String.asNameId() = Name.identifier(this)

val isIde: Boolean = Project::class.java.name == "com.intellij.openapi.project.Project"

fun <T> unsafeLazy(init: () -> T) = lazy(LazyThreadSafetyMode.NONE, init)

fun Annotated.hasAnnotation(fqName: FqName): Boolean =
    annotations.hasAnnotation(fqName)

fun Annotated.getAnnotatedAnnotations(annotation: FqName): List<AnnotationDescriptor> =
    annotations.filter {
        val inner = it.type.constructor.declarationDescriptor as ClassDescriptor
        inner.hasAnnotation(annotation)
    }

fun IrAnnotationContainer.getAnnotatedAnnotations(annotation: FqName): List<IrConstructorCall> =
    annotations.filter {
        val inner = it.type.classOrNull!!.owner
        inner.hasAnnotation(annotation)
    }

fun DeclarationDescriptor.uniqueKey(context: InjektContext): String {
    val original = this.original
    return when (original) {
        is ConstructorDescriptor -> "constructor:${original.constructedClass.fqNameSafe}:${
            original.valueParameters
                .joinToString(",") {
                    it.type
                        .fullyAbbreviatedType
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
                        .fullyAbbreviatedType
                        .uniqueTypeName()
                }
        }"
        is PropertyDescriptor -> "property:$fqNameSafe:${
            listOfNotNull(
                original.dispatchReceiverParameter, original.extensionReceiverParameter)
                .joinToString(",") {
                    it.type
                        .fullyAbbreviatedType
                        .uniqueTypeName()
                }
        }"
        is TypeAliasDescriptor -> "typealias:$fqNameSafe"
        is TypeParameterDescriptor ->
            "typeparameter:$fqNameSafe:${containingDeclaration!!.uniqueKey(context)}"
        is ParameterDescriptor -> ""
        is ValueParameterDescriptor -> ""
        is VariableDescriptor -> ""
        else -> error("Unexpected declaration $this")
    }
}

private val KotlinType.fullyAbbreviatedType: KotlinType
    get() {
        val abbreviatedType = getAbbreviatedType()
        return if (abbreviatedType != null && abbreviatedType != this) abbreviatedType.fullyAbbreviatedType else this
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

inline fun <A, B> List<A>.forEachWith(
    other: List<B>,
    action: (A, B) -> Unit
) {
    check(size == other.size) {
        "Size not equal this: $this other: $other"
    }
    for (i in indices) action(this[i], other[i])
}

fun <K, V> List<K>.toMap(values: List<V>): Map<K, V> {
    val map = mutableMapOf<K, V>()
    forEachWith(values) { key, value -> map[key] = value }
    return map
}

private var currentFrameworkKey = 0
fun generateFrameworkKey() = currentFrameworkKey++.toString()

data class Tuple1<T>(val value: T)
