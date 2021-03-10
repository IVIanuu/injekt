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
import com.ivianuu.injekt.compiler.getContributionParameters
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class CallableRef(
    val callable: CallableDescriptor,
    val type: TypeRef,
    val originalType: TypeRef,
    val typeParameters: List<ClassifierRef>,
    val parameterTypes: Map<String, TypeRef>,
    val parameterContributionKinds: Map<String, ContributionKind?>,
    val qualifiers: List<AnnotationRef>,
    val typeArguments: Map<ClassifierRef, TypeRef>,
    val contributionKind: ContributionKind?,
    val isMacro: Boolean,
    val isFromMacro: Boolean,
    val callContext: CallContext
)

fun CallableRef.substitute(substitutionMap: Map<ClassifierRef, TypeRef>): CallableRef {
    if (substitutionMap.isEmpty()) return this
    return copy(
        type = type.substitute(substitutionMap),
        parameterTypes = parameterTypes
            .mapValues { it.value.substitute(substitutionMap) },
        typeArguments = typeArguments
            .mapValues { it.value.substitute(substitutionMap) }
    )
}

fun CallableRef.substituteInputs(substitutionMap: Map<ClassifierRef, TypeRef>): CallableRef {
    if (substitutionMap.isEmpty()) return this
    return copy(
        parameterTypes = parameterTypes.mapValues { it.value.substitute(substitutionMap) },
        typeArguments = typeArguments
            .mapValues { it.value.substitute(substitutionMap) }
    )
}

enum class ContributionKind {
    VALUE, SET_ELEMENT, MODULE
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
        parameterContributionKinds = (if (this is ConstructorDescriptor) valueParameters else allParameters)
            .map { it.injektName() to it.contributionKind(declarationStore) }
            .toMap(),
        qualifiers = qualifiers,
        typeArguments = typeParameters
            .map { it to it.defaultType }
            .toMap(),
        contributionKind = contributionKind(declarationStore),
        isMacro = hasAnnotation(InjektFqNames.Macro),
        isFromMacro = false,
        callContext = callContext
    ).let {
        if (applyCallableInfo && original.isExternalDeclaration()) it.apply(
            declarationStore,
            declarationStore.callableInfoFor(it.callable)
        ) else it
    }
}

fun MemberScope.collectContributions(
    declarationStore: DeclarationStore,
    type: TypeRef,
    substitutionMap: Map<ClassifierRef, TypeRef>
): List<CallableRef> {
    val primaryConstructorKinds = (type.classifier.descriptor
        ?.safeAs<ClassDescriptor>()
        ?.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.mapNotNull {
            val kind = it.contributionKind(declarationStore)
            if (kind != null) it.name to kind else null
        }
        ?.toMap()
        ?: emptyMap())
    return getContributedDescriptors()
        .flatMap { callable ->
            when (callable) {
                is ClassDescriptor -> callable.getContributionConstructors(declarationStore)
                    .map { it.substitute(substitutionMap) }
                is PropertyDescriptor -> (callable.contributionKind(declarationStore)
                    ?: primaryConstructorKinds[callable.name])
                    ?.let { kind ->
                        listOf(
                            callable.toCallableRef(declarationStore)
                                .copy(contributionKind = kind)
                                .substitute(substitutionMap)
                        )
                    } ?: emptyList()
                is FunctionDescriptor -> callable.contributionKind(declarationStore)?.let { kind ->
                    listOf(
                        callable.toCallableRef(declarationStore)
                            .copy(contributionKind = kind)
                            .substitute(substitutionMap)
                    )
                } ?: emptyList()
                else -> emptyList()
            }
        }
}

fun Annotated.contributionKind(declarationStore: DeclarationStore): ContributionKind? = when {
    hasAnnotation(InjektFqNames.Given) -> ContributionKind.VALUE
    hasAnnotation(InjektFqNames.GivenSetElement) -> ContributionKind.SET_ELEMENT
    hasAnnotation(InjektFqNames.Module) -> ContributionKind.MODULE
    this is ClassConstructorDescriptor -> constructedClass.contributionKind(declarationStore)
    else -> null
}

fun CallableDescriptor.collectContributions(
    declarationStore: DeclarationStore
): List<CallableRef> {
    val declarations = mutableListOf<CallableRef>()

    declarations += allParameters
        .mapNotNull { parameter ->
            val kind = parameter.contributionKind(declarationStore)
            if (kind != null || parameter == extensionReceiverParameter) parameter.toCallableRef(declarationStore)
                .copy(contributionKind = kind ?: ContributionKind.VALUE)
            else null
        }

    extensionReceiverParameter?.let { receiver ->
        declarations += receiver.type.memberScope.collectContributions(
            declarationStore,
            extensionReceiverParameter!!.type.toTypeRef(declarationStore),
            emptyMap()
        )
    }

    return declarations
}

fun ParameterDescriptor.contributionKind(declarationStore: DeclarationStore): ContributionKind? {
    return (this as Annotated).contributionKind(declarationStore)
        ?: type.contributionKind(declarationStore)
        ?: getUserData(DslMarkerUtils.FunctionTypeAnnotationsKey)?.let { userData ->
        when {
            userData.hasAnnotation(InjektFqNames.Given) -> ContributionKind.VALUE
            userData.hasAnnotation(InjektFqNames.GivenSetElement) -> ContributionKind.SET_ELEMENT
            userData.hasAnnotation(InjektFqNames.Module) -> ContributionKind.MODULE
            else -> null
        }
    } ?: getContributionParameters(declarationStore)
        .firstOrNull { it.callable == this }
        ?.contributionKind
        ?: containingDeclaration.safeAs<FunctionDescriptor>()
            ?.takeIf { it.isExternalDeclaration() }
            ?.let { declarationStore.callableInfoFor(it) }
            ?.let { it.parameterContributionKinds[name.asString()] }
}

fun ClassDescriptor.getContributionConstructors(
    declarationStore: DeclarationStore
): List<CallableRef> = constructors
    .mapNotNull { constructor ->
        if (constructor.isPrimary) {
            (constructor.contributionKind(declarationStore)
                ?: contributionKind(declarationStore))?.let { kind ->
                constructor.toCallableRef(declarationStore)
                    .copy(contributionKind = kind)
            }
        } else {
            constructor.contributionKind(declarationStore)?.let { kind ->
                constructor.toCallableRef(declarationStore)
                    .copy(contributionKind = kind)
            }
        }
    }
    .map {
        if (it.type.classifier.qualifiers.isNotEmpty()) {
            val typeWithQualifiers = it.type.copy(qualifiers = it.type.classifier.qualifiers + it.type.qualifiers)
            it.copy(type = typeWithQualifiers, originalType = typeWithQualifiers)
        } else {
            it
        }
    }

fun CallableRef.collectContributions(
    declarationStore: DeclarationStore,
    substitutionMap: Map<ClassifierRef, TypeRef>,
    addGiven: (CallableRef) -> Unit,
    addGivenSetElement: (CallableRef) -> Unit,
    addMacro: (CallableRef) -> Unit
) {
    if (isMacro) {
        addMacro(this)
        return
    }
    when (contributionKind) {
        ContributionKind.VALUE -> addGiven(this)
        ContributionKind.SET_ELEMENT -> addGivenSetElement(this)
        ContributionKind.MODULE -> {
            addGiven(this)
            callable
                .returnType!!
                .memberScope
                .collectContributions(declarationStore, type, substitutionMap)
                .forEach {
                    it.collectContributions(
                        declarationStore,
                        substitutionMap,
                        addGiven,
                        addGivenSetElement,
                        addMacro
                    )
                }
        }
    }
}
