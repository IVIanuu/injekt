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
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getContributionParameters
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.toAnnotationRef
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class CallableRef(
    val callable: CallableDescriptor,
    val type: TypeRef,
    val originalType: TypeRef,
    val typeParameters: List<ClassifierRef>,
    val parameterTypes: Map<ParameterDescriptor, TypeRef>,
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
    VALUE, SET_ELEMENT, MODULE, INTERCEPTOR
}

fun CallableDescriptor.toCallableRef(
    declarationStore: DeclarationStore,
    applyCallableInfo: Boolean = true
): CallableRef {
    val type = returnType!!.toTypeRef(declarationStore).let {
        it.copy(
            qualifiers = ((this as? ClassConstructorDescriptor)
                ?.takeIf { it.isPrimary }
                ?.constructedClass
                ?.getAnnotatedAnnotations(InjektFqNames.Qualifier)
                ?: getAnnotatedAnnotations(InjektFqNames.Qualifier))
                .map { it.toAnnotationRef(declarationStore) } + it.qualifiers
        )
    }
    val typeParameters = typeParameters
        .map { it.toClassifierRef(declarationStore) }
    return CallableRef(
        callable = this,
        type = type,
        originalType = type,
        typeParameters = typeParameters,
        parameterTypes = allParameters
            .map { it to it.type.toTypeRef(declarationStore) }
            .toMap(),
        typeArguments = typeParameters
            .map { it to it.defaultType }
            .toMap(),
        contributionKind = contributionKind(declarationStore),
        isMacro = hasAnnotation(InjektFqNames.Macro),
        isFromMacro = false,
        callContext = callContext
    ).let {
        if (applyCallableInfo) it.apply(
            declarationStore,
            declarationStore.callableInfoFor(it)
        ) else it
    }
}

fun MemberScope.collectContributions(
    declarationStore: DeclarationStore,
    type: TypeRef
): List<CallableRef> {
    // special case to support @Given () -> Foo etc
    if ((type.classifier.fqName.asString().startsWith("kotlin.Function")
                || type.classifier.fqName.asString()
            .startsWith("kotlin.coroutines.SuspendFunction"))
    ) {
        val contributionKind = type.contributionKind
        if (contributionKind != null) {
            return listOf(
                getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
                    .first()
                    .toCallableRef(declarationStore)
                    .let { callable ->
                        callable.copy(
                            contributionKind = contributionKind,
                            parameterTypes = callable.parameterTypes.toMutableMap()
                                .also { it[callable.callable.dispatchReceiverParameter!!] = type }
                        )
                    }
            )
        }
    }

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
                is PropertyDescriptor -> (callable.contributionKind(declarationStore)
                    ?: primaryConstructorKinds[callable.name])
                    ?.let { kind ->
                        listOf(
                            callable.toCallableRef(declarationStore)
                                .copy(contributionKind = kind)
                        )
                    } ?: emptyList()
                is FunctionDescriptor -> callable.contributionKind(declarationStore)?.let { kind ->
                    listOf(
                        callable.toCallableRef(declarationStore)
                            .copy(contributionKind = kind)
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
    hasAnnotation(InjektFqNames.Interceptor) -> ContributionKind.INTERCEPTOR
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
            when {
                kind == ContributionKind.INTERCEPTOR ->
                    parameter.toCallableRef(declarationStore)
                        .copy(contributionKind = ContributionKind.MODULE)
                kind != null ->  parameter.toCallableRef(declarationStore)
                    .copy(contributionKind = kind)
                else -> null
            }
        }

    extensionReceiverParameter?.let { receiver ->
        declarations += receiver.toCallableRef(declarationStore)
            .copy(contributionKind = ContributionKind.VALUE)
        declarations += receiver.type.memberScope.collectContributions(
            declarationStore,
            extensionReceiverParameter!!.type.toTypeRef(declarationStore)
        )
    }

    return declarations
}

fun ParameterDescriptor.contributionKind(declarationStore: DeclarationStore): ContributionKind? {
    val userData = getUserData(DslMarkerUtils.FunctionTypeAnnotationsKey)
    val contributionParameters = getContributionParameters(declarationStore)

    return (this as Annotated).contributionKind(declarationStore)
        ?: type.contributionKind(declarationStore)
        ?: userData?.let {
        when {
            userData.hasAnnotation(InjektFqNames.Given) -> ContributionKind.VALUE
            userData.hasAnnotation(InjektFqNames.GivenSetElement) -> ContributionKind.SET_ELEMENT
            userData.hasAnnotation(InjektFqNames.Module) -> ContributionKind.MODULE
            userData.hasAnnotation(InjektFqNames.Interceptor) -> ContributionKind.INTERCEPTOR
            else -> null
        }
    } ?: contributionParameters
        .firstOrNull { it.callable == this }
        ?.contributionKind
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
    .flatMap { declaration ->
        if (declaration.contributionKind == ContributionKind.VALUE) {
            allGivenTypes(declarationStore).map { type ->
                declaration.copy(
                    type = type.copy(
                        qualifiers = declaration.type.qualifiers + type.qualifiers
                    )
                )
            }
        } else {
            listOf(declaration)
        }
    }


fun ClassDescriptor.allGivenTypes(declarationStore: DeclarationStore): List<TypeRef> = buildList<TypeRef> {
    this += defaultType.toTypeRef(declarationStore)
    this += defaultType.constructor.supertypes
        .filter { it.hasAnnotation(InjektFqNames.Given) }
        .map { it.toTypeRef(declarationStore) }
}

fun CallableRef.collectContributions(
    declarationStore: DeclarationStore,
    path: List<Any>,
    addGiven: (CallableRef) -> Unit,
    addGivenSetElement: (CallableRef) -> Unit,
    addInterceptor: (CallableRef) -> Unit,
    addMacro: (CallableRef) -> Unit
) {
    if (isMacro) {
        addMacro(this)
        return
    }
    when (contributionKind) {
        ContributionKind.VALUE -> addGiven(this)
        ContributionKind.SET_ELEMENT -> addGivenSetElement(this)
        ContributionKind.INTERCEPTOR -> addInterceptor(this)
        ContributionKind.MODULE -> {
            val isFunction = type.allTypes.any {
                it.classifier.fqName.asString().startsWith("kotlin.Function")
                        || it.classifier.fqName.asString()
                    .startsWith("kotlin.coroutines.SuspendFunction")
            }
            if (isFunction) {
                val nextPath = path + callable.fqNameSafe
                if (isFunction) {
                    val nextCallable = copy(type = type.copy(path = nextPath))
                    addGiven(nextCallable)
                    callable.returnType!!.memberScope
                        .collectContributions(declarationStore, nextCallable.type)
                        .forEach {
                            it.collectContributions(
                                declarationStore,
                                path + it.callable.fqNameSafe,
                                addGiven,
                                addGivenSetElement,
                                addInterceptor,
                                addMacro
                            )
                        }
                } else {
                    addGiven(this)
                    callable.returnType!!.memberScope
                        .collectContributions(declarationStore, type)
                        .forEach {
                            it.collectContributions(
                                declarationStore,
                                path + it.callable.fqNameSafe,
                                addGiven,
                                addGivenSetElement,
                                addInterceptor,
                                addMacro
                            )
                        }
                }
            } else {
                addGiven(this)
                callable.returnType!!.memberScope
                    .collectContributions(declarationStore, type)
                    .forEach {
                        it.collectContributions(
                            declarationStore,
                            path + it.callable.fqNameSafe,
                            addGiven,
                            addGivenSetElement,
                            addInterceptor,
                            addMacro
                        )
                    }
            }
        }
    }
}
