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

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.apply
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getGivenParameters
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.com.intellij.icons.AllIcons.Nodes.Public
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.scopes.MemberScope
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
            .mapValues { it.value.substitute(map) }
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

fun CallableDescriptor.toCallableRef(
    declarationStore: DeclarationStore,
    applyCallableInfo: Boolean = true
): CallableRef {
    val qualifiers = getAnnotatedAnnotations(InjektFqNames.Qualifier)
        .map { it.toAnnotationRef(declarationStore) }
    val type = returnType!!.toTypeRef(declarationStore).let {
        it.copy(qualifiers = qualifiers + it.qualifiers)
    }
    val typeParameters = typeParameters
        .map { it.toClassifierRef(declarationStore) }
    return CallableRef(
        callable = this,
        type = type,
        originalType = type,
        typeParameters = typeParameters,
        parameterTypes = (if (this is ConstructorDescriptor) valueParameters else allParameters)
            .map { it.injektName() to it.type.toTypeRef(declarationStore) }
            .toMap(),
        givenParameters = (if (this is ConstructorDescriptor) valueParameters else allParameters)
            .filter { it.isGiven(declarationStore) }
            .mapTo(mutableSetOf()) { it.injektName() },
        qualifiers = qualifiers,
        typeArguments = typeParameters
            .map { it to it.defaultType }
            .toMap(),
        isGiven = isGiven(declarationStore),
        fromGivenConstraint = false,
        callContext = callContext
    ).let {
        if (applyCallableInfo && original.isExternalDeclaration()) it.apply(
            declarationStore,
            declarationStore.callableInfoFor(it.callable)
        ) else it
    }
}

fun MemberScope.collectGivens(
    declarationStore: DeclarationStore,
    type: TypeRef,
    owningDescriptor: DeclarationDescriptor?,
    substitutionMap: Map<ClassifierRef, TypeRef>
): List<CallableRef> {
    val givenPrimaryConstructorParameters = (type.classifier.descriptor
        ?.safeAs<ClassDescriptor>()
        ?.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.filter { it.isGiven(declarationStore) }
        ?.map { it.name } ?: emptyList())
    return getContributedDescriptors()
        .flatMap { callable ->
            when (callable) {
                is ClassDescriptor -> callable.getGivenConstructors(declarationStore)
                    .map { it.substitute(substitutionMap) }
                is PropertyDescriptor -> if (callable.isGiven(declarationStore) ||
                        callable.name in givenPrimaryConstructorParameters) {
                    listOf(
                        callable.toCallableRef(declarationStore)
                            .copy(isGiven = true)
                            .substitute(substitutionMap)
                    )
                } else emptyList()
                is FunctionDescriptor -> if (callable.isGiven(declarationStore)) {
                    listOf(
                        callable.toCallableRef(declarationStore)
                            .copy(isGiven = true)
                            .substitute(substitutionMap)
                    )
                } else emptyList()
                else -> emptyList()
            }
        }
        .filter {
            it.callable.visibility == DescriptorVisibilities.PUBLIC ||
                    it.callable.visibility == DescriptorVisibilities.LOCAL ||
                    (it.callable.visibility == DescriptorVisibilities.INTERNAL &&
                            !it.callable.original.isExternalDeclaration()) ||
                    it.callable.parents.any {
                        it == owningDescriptor
                    }
        }
}

fun Annotated.isGiven(declarationStore: DeclarationStore): Boolean =
    hasAnnotation(InjektFqNames.Given) ||
            (this is ClassConstructorDescriptor && constructedClass.isGiven(declarationStore))

fun CallableDescriptor.collectGivens(declarationStore: DeclarationStore): List<CallableRef> {
    val declarations = mutableListOf<CallableRef>()

    declarations += allParameters
        .filter { it.isGiven(declarationStore) || it == extensionReceiverParameter }
        .map { it.toCallableRef(declarationStore).copy(isGiven = true) }

    return declarations
}

fun ParameterDescriptor.isGiven(declarationStore: DeclarationStore): Boolean {
    return (this as Annotated).isGiven(declarationStore) ||
            type.isGiven(declarationStore) ||
            getUserData(DslMarkerUtils.FunctionTypeAnnotationsKey)
                ?.hasAnnotation(InjektFqNames.Given) == true ||
            getGivenParameters(declarationStore)
                .firstOrNull { it.callable == this }
                ?.isGiven == true ||
            containingDeclaration.safeAs<FunctionDescriptor>()
                ?.takeIf { it.isExternalDeclaration() }
                ?.let { declarationStore.callableInfoFor(it) }
                ?.let { name.asString() in it.givenParameters } == true
}

fun ClassDescriptor.getGivenConstructors(
    declarationStore: DeclarationStore
): List<CallableRef> = constructors
    .filter { constructor ->
        constructor.isGiven(declarationStore) ||
                (constructor.isPrimary && isGiven(declarationStore))
    }
    .map {
        it.toCallableRef(declarationStore)
            .copy(isGiven = true)
    }
    .map {
        if (it.type.classifier.qualifiers.isNotEmpty()) {
            val typeWithQualifiers = it.type.copy(qualifiers = it.type.classifier.qualifiers + it.type.qualifiers)
            it.copy(type = typeWithQualifiers, originalType = typeWithQualifiers)
        } else {
            it
        }
    }

fun CallableRef.collectGivens(
    declarationStore: DeclarationStore,
    owningDescriptor: DeclarationDescriptor?,
    substitutionMap: Map<ClassifierRef, TypeRef>,
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
        .collectGivens(declarationStore, type, owningDescriptor, combinedSubstitutionMap)
        .forEach {
            it.collectGivens(
                declarationStore,
                owningDescriptor,
                combinedSubstitutionMap,
                addGiven,
                addConstrainedGiven
            )
        }
}
