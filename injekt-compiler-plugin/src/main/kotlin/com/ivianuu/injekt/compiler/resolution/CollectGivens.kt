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

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.Tuple1
import com.ivianuu.injekt.compiler.apply
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.lazy.LazyImportScope
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class CallableRef(
    val callable: CallableDescriptor,
    val type: TypeRef,
    val originalType: TypeRef,
    val typeParameters: List<ClassifierRef>,
    val parameterTypes: Map<String, TypeRef>,
    val givenParameters: Set<String>,
    val qualifiers: List<AnnotationRef>,
    val typeArguments: Map<ClassifierRef, TypeRef>,
    val isGiven: Boolean,
    val fromGivenConstraint: Boolean,
    val callContext: CallContext
)

fun CallableRef.substitute(map: Map<ClassifierRef, TypeRef>): CallableRef {
    if (map.isEmpty()) return this
    return copy(
        type = type.substitute(map),
        parameterTypes = parameterTypes
            .mapValues { it.value.substitute(map) },
        typeArguments = typeArguments
            .mapValues { it.value.substitute(map) },
        qualifiers = qualifiers
            .map { it.substitute(map) }
    )
}

fun CallableRef.substituteInputs(map: Map<ClassifierRef, TypeRef>): CallableRef {
    if (map.isEmpty()) return this
    return copy(
        parameterTypes = parameterTypes.mapValues { it.value.substitute(map) },
        typeArguments = typeArguments
            .mapValues { it.value.substitute(map) }
    )
}

fun CallableRef.makeGiven(): CallableRef = if (isGiven) this
else copy(isGiven = true)

fun CallableDescriptor.toCallableRef(
    context: InjektContext,
    trace: BindingTrace?
): CallableRef {
    trace?.get(InjektWritableSlices.CALLABLE_REF_FOR_DESCRIPTOR, this)?.let { return it }
    val qualifiers = getAnnotatedAnnotations(InjektFqNames.Qualifier)
        .map { it.toAnnotationRef(context, trace) }
    val type = returnType!!.toTypeRef(context, trace).let {
        if (qualifiers.isNotEmpty()) it.copy(qualifiers = (qualifiers + it.qualifiers)
            .distinctBy { it.type.classifier })
        else it
    }
    val typeParameters = typeParameters
        .map { it.toClassifierRef(context, trace) }
    return CallableRef(
        callable = this,
        type = type,
        originalType = type,
        typeParameters = typeParameters,
        parameterTypes = (if (this is ConstructorDescriptor) valueParameters else allParameters)
            .map { it.injektName() to it.type.toTypeRef(context, trace) }
            .toMap(),
        givenParameters = (if (this is ConstructorDescriptor) valueParameters else allParameters)
            .filter { it.isGiven(context, trace) }
            .mapTo(mutableSetOf()) { it.injektName() },
        qualifiers = qualifiers,
        typeArguments = typeParameters
            .map { it to it.defaultType }
            .toMap(),
        isGiven = isGiven(context, trace),
        fromGivenConstraint = false,
        callContext = callContext
    ).let {
        if (original.isExternalDeclaration()) it.apply(
            context,
            trace,
            context.callableInfoFor(it.callable, trace)
        ) else it
    }.also {
        trace?.record(InjektWritableSlices.CALLABLE_REF_FOR_DESCRIPTOR, this, it)
    }
}

fun MemberScope.collectGivens(
    context: InjektContext,
    type: TypeRef?,
    ownerDescriptor: DeclarationDescriptor?,
    trace: BindingTrace?,
    substitutionMap: Map<ClassifierRef, TypeRef>
): List<CallableRef> {
    val givenPrimaryConstructorParameters = (type?.classifier?.descriptor
        ?.safeAs<ClassDescriptor>()
        ?.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.filter { it.isGiven(context, trace) }
        ?.map { it.name } ?: emptyList())
    return getContributedDescriptors()
        .flatMap { declaration ->
            when (declaration) {
                is ClassDescriptor -> listOfNotNull(
                    declaration.getGivenConstructor(context, trace)
                        ?.substitute(substitutionMap)
                )
                is PropertyDescriptor -> if (declaration.isGiven(context, trace) ||
                        declaration.name in givenPrimaryConstructorParameters) {
                    listOf(
                        declaration.toCallableRef(context, trace)
                            .substitute(substitutionMap)
                    )
                } else emptyList()
                is FunctionDescriptor -> if (declaration.isGiven(context, trace)) {
                    listOf(
                        declaration.toCallableRef(context, trace)
                            .substitute(substitutionMap)
                    )
                } else emptyList()
                else -> emptyList()
            }
        }
        .filter { callable ->
            callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
                    callable.callable.visibility == DescriptorVisibilities.LOCAL ||
                    (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
                            !callable.callable.original.isExternalDeclaration()) ||
                    (callable.callable is ClassConstructorDescriptor &&
                            callable.type.classifier.isObject) ||
                    callable.callable.parents.any {
                        it == ownerDescriptor
                    }
        }
}

fun Annotated.isGiven(context: InjektContext, trace: BindingTrace?): Boolean {
    @Suppress("IMPLICIT_CAST_TO_ANY")
    val key = if (this is KotlinType) System.identityHashCode(this) else this
    trace?.get(InjektWritableSlices.IS_GIVEN, key)?.let { return it }
    var isGiven = hasAnnotation(InjektFqNames.Given)
    if (!isGiven && this is ParameterDescriptor) {
        isGiven = type.isGiven(context, trace) ||
                containingDeclaration.safeAs<FunctionDescriptor>()
                    ?.takeIf { it.isExternalDeclaration() }
                    ?.let { context.callableInfoFor(it, trace) }
                    ?.let { name.asString() in it.givenParameters } == true
    }
    if (!isGiven && this is ClassConstructorDescriptor) {
        isGiven = constructedClass.isGiven(context, trace)
    }
    trace?.record(InjektWritableSlices.IS_GIVEN, key, isGiven)
    return isGiven
}

fun ClassDescriptor.getGivenConstructor(
    context: InjektContext,
    trace: BindingTrace?
): CallableRef? {
    trace?.get(InjektWritableSlices.GIVEN_CONSTRUCTOR, this)?.let { return it.value }
    val rawGivenConstructor = if (isGiven(context, trace))
        unsubstitutedPrimaryConstructor!!.toCallableRef(context, trace)
    else constructors
        .singleOrNull { it.hasAnnotation(InjektFqNames.Given) }
        ?.toCallableRef(context, trace)
        ?.makeGiven()
    val finalConstructor = if (rawGivenConstructor != null) {
        if (rawGivenConstructor.type.classifier.qualifiers.isNotEmpty()) {
            val typeWithQualifiers = rawGivenConstructor.type.copy(
                qualifiers = rawGivenConstructor.type.classifier.qualifiers +
                        rawGivenConstructor.type.qualifiers)
            rawGivenConstructor.copy(type = typeWithQualifiers, originalType = typeWithQualifiers)
        } else {
            rawGivenConstructor
        }
    } else null
    trace?.record(InjektWritableSlices.GIVEN_CONSTRUCTOR, this, Tuple1(finalConstructor))
    return finalConstructor
}

fun CallableRef.collectGivens(
    context: InjektContext,
    ownerDescriptor: DeclarationDescriptor?,
    substitutionMap: Map<ClassifierRef, TypeRef>,
    trace: BindingTrace?,
    addGiven: (CallableRef) -> Unit,
    addConstrainedGiven: (CallableRef) -> Unit
) {
    if (!fromGivenConstraint && typeParameters.any { it.isGivenConstraint }) {
        addConstrainedGiven(this)
        return
    }
    addGiven(this)
    val combinedSubstitutionMap = substitutionMap + type.classifier.typeParameters
        .zip(type.arguments)
    callable
        .returnType!!
        .memberScope
        .collectGivens(context, type, ownerDescriptor, trace, combinedSubstitutionMap)
        .forEach {
            it.collectGivens(
                context,
                ownerDescriptor,
                combinedSubstitutionMap,
                trace,
                addGiven,
                addConstrainedGiven
            )
        }
}

fun HierarchicalScope.collectGivensInScope(
    context: InjektContext,
    trace: BindingTrace?
): List<CallableRef> = getContributedDescriptors()
        .flatMap { declaration ->
            when (declaration) {
                is ClassDescriptor -> listOfNotNull(
                    declaration.getGivenConstructor(context, trace)
                )
                is PropertyDescriptor -> if (declaration.isGiven(context, trace)) {
                    listOf(
                        declaration.toCallableRef(context, trace)
                            .makeGiven()
                    )
                } else emptyList()
                is FunctionDescriptor -> if (declaration.isGiven(context, trace)) {
                    listOf(declaration.toCallableRef(context, trace))
                } else emptyList()
                is VariableDescriptor -> if (declaration.isGiven(context, trace)) {
                    listOf(declaration.toCallableRef(context, trace))
                } else emptyList()
                else -> emptyList()
            }
        }
