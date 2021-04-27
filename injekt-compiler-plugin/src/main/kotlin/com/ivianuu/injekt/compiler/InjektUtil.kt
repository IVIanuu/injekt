/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.all
import kotlin.collections.any
import kotlin.collections.asSequence
import kotlin.collections.emptyList
import kotlin.collections.emptyMap
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.indices
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.collections.listOfNotNull
import kotlin.collections.mapNotNull
import kotlin.collections.mutableMapOf
import kotlin.collections.none
import kotlin.collections.plus
import kotlin.collections.set

val isIde: Boolean get() = Project::class.java.name == "com.intellij.openapi.project.Project"

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
        .asSequence()
        .filter { it.isAllUnder }
        .any { fqName.asString().startsWith(it.fqName.asString()) }
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
    return when (val original = this.original) {
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
            listOfNotNull(original.dispatchReceiverParameter, original.extensionReceiverParameter)
                .plus(original.valueParameters)
                .joinToString(",") { parameter ->
                    buildString { 
                        when  {
                            parameter === original.dispatchReceiverParameter -> append("d:")
                            parameter === original.extensionReceiverParameter -> append("e:")
                            else -> append("p:")
                        }
                        append(
                            parameter.type
                                .fullyAbbreviatedType
                                .uniqueTypeName()
                        )
                    }
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
    if (this.isEmpty()) return emptyMap()
    val map = mutableMapOf<K, V>()
    forEachWith(values) { key, value -> map[key] = value }
    return map
}

private var currentFrameworkKey = 0
fun generateFrameworkKey() = currentFrameworkKey++

data class Tuple1<T>(val value: T)

fun TypeParameterDescriptor.isGivenConstraint(
    context: InjektContext,
    trace: BindingTrace?
): Boolean {
    trace?.get(InjektWritableSlices.IS_GIVEN_CONSTRAINT, this)?.let { return it }
    var isGivenConstraint = hasAnnotation(InjektFqNames.Given) ||
            findPsi()?.safeAs<KtTypeParameter>()?.hasAnnotation(InjektFqNames.Given) == true
    if (!isGivenConstraint &&
        original.isExternalDeclaration() &&
        containingDeclaration is ClassDescriptor) {
        isGivenConstraint = name.asString() in context.classifierInfoFor(containingDeclaration.cast(), trace)
            ?.givenConstraintTypeParameters ?: emptyList()
    }

    trace?.record(InjektWritableSlices.IS_GIVEN_CONSTRAINT, this, isGivenConstraint)
    return isGivenConstraint
}

fun TypeParameterDescriptor.isForTypeKey(
    context: InjektContext,
    trace: BindingTrace?
): Boolean {
    trace?.get(InjektWritableSlices.IS_FOR_TYPE_KEY, this)?.let {
        return it
    }
    var isForTypeKey = hasAnnotation(InjektFqNames.ForTypeKey) ||
            findPsi()?.safeAs<KtTypeParameter>()?.hasAnnotation(InjektFqNames.ForTypeKey) == true
    if (!isForTypeKey &&
        original.isExternalDeclaration() &&
        containingDeclaration is ClassDescriptor) {
        isForTypeKey = name.asString() in context.classifierInfoFor(containingDeclaration.cast(), trace)
            ?.forTypeKeyTypeParameters ?: emptyList()
    }

    trace?.record(InjektWritableSlices.IS_FOR_TYPE_KEY, this, isForTypeKey)
    return isForTypeKey
}

fun ClassifierDescriptor.isSingletonGiven(
    context: InjektContext,
    trace: BindingTrace
): Boolean {
    if (this !is ClassDescriptor) return false
    trace.get(InjektWritableSlices.IS_SINGLETON_GIVEN, this)?.let {
        return it
    }
    var isSingletonGiven = kind == ClassKind.CLASS &&
            getGivenConstructors(context, trace)
                .let { givenConstructors ->
                    givenConstructors.isNotEmpty() &&
                            givenConstructors.all { it.callable.valueParameters.isEmpty() }
                } &&
            declaredTypeParameters.none { it.isForTypeKey(context, trace) } &&
            unsubstitutedMemberScope.getContributedDescriptors()
                .none {
                    (it is ClassDescriptor &&
                            it.isInner) ||
                    (it is PropertyDescriptor &&
                            it.hasBackingField(trace.bindingContext))
                }

    if (!isSingletonGiven && original.isExternalDeclaration()) {
        isSingletonGiven = context.classifierInfoFor(this, trace)
            ?.isOptimizableGiven == true
    }

    trace.record(InjektWritableSlices.IS_SINGLETON_GIVEN, this, isSingletonGiven)
    return isSingletonGiven
}
