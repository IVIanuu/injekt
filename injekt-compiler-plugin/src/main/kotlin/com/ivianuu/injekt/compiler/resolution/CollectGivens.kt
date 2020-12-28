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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getContributionParameters
import com.ivianuu.injekt.compiler.hasAnnotation
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
    val type: TypeRef = callable.returnType!!.toTypeRef().let {
        it.copy(
            qualifiers = ((callable as? ClassConstructorDescriptor)
                ?.takeIf { it.isPrimary }
                ?.constructedClass
                ?.getAnnotatedAnnotations(InjektFqNames.Qualifier)
                ?: callable.getAnnotatedAnnotations(InjektFqNames.Qualifier)) + it.qualifiers
        )
    },
    val originalType: TypeRef = callable.returnType!!.toTypeRef().let {
        it.copy(
            qualifiers = ((callable as? ClassConstructorDescriptor)
                ?.takeIf { it.isPrimary }
                ?.constructedClass
                ?.getAnnotatedAnnotations(InjektFqNames.Qualifier)
                ?: callable.getAnnotatedAnnotations(InjektFqNames.Qualifier)) + it.qualifiers
        )
    },
    val parameterTypes: Map<ParameterDescriptor, TypeRef> = callable.allParameters
        .map { it to it.type.toTypeRef() }
        .toMap(),
    val typeArguments: Map<ClassifierRef, TypeRef> = callable.typeParameters
        .map { it.toClassifierRef() to it.defaultType.toTypeRef() }
        .toMap(),
    val contributionKind: ContributionKind? = callable.contributionKind(),
    val isMacro: Boolean = callable.hasAnnotation(InjektFqNames.Macro),
    val isFromMacro: Boolean = false,
    val callContext: CallContext = callable.callContext,
)

fun CallableRef.substitute(substitutionMap: Map<ClassifierRef, TypeRef>): CallableRef {
    if (substitutionMap.isEmpty()) return this
    return copy(
        type = type.substitute(substitutionMap),
        parameterTypes = parameterTypes.mapValues { it.value.substitute(substitutionMap) },
        typeArguments = typeArguments
            .mapValues { it.value.substitute(substitutionMap) }
    )
}

enum class ContributionKind {
    VALUE, SET_ELEMENT, MODULE, INTERCEPTOR
}

fun CallableDescriptor.toCallableRef() = CallableRef(this)

fun MemberScope.collectContributions(type: TypeRef): List<CallableRef> {
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
                    .toCallableRef()
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
            val kind = it.contributionKind()
            if (kind != null) it.name to kind else null
        }
        ?.toMap()
        ?: emptyMap())
    return getContributedDescriptors()
        .flatMap { callable ->
            when (callable) {
                is ClassDescriptor -> callable.getContributionConstructors()
                is PropertyDescriptor -> (callable.contributionKind()
                    ?: primaryConstructorKinds[callable.name])
                    ?.let { kind -> listOf(CallableRef(callable, contributionKind = kind)) } ?: emptyList()
                is FunctionDescriptor -> callable.contributionKind()?.let { kind ->
                    listOf(CallableRef(callable, contributionKind = kind))
                } ?: emptyList()
                else -> emptyList()
            }
        }
}

fun Annotated.contributionKind(): ContributionKind? = when {
    hasAnnotation(InjektFqNames.Given) -> ContributionKind.VALUE
    hasAnnotation(InjektFqNames.GivenSetElement) -> ContributionKind.SET_ELEMENT
    hasAnnotation(InjektFqNames.Module) -> ContributionKind.MODULE
    hasAnnotation(InjektFqNames.Interceptor) -> ContributionKind.INTERCEPTOR
    this is ClassConstructorDescriptor -> constructedClass.contributionKind()
    else -> null
}

fun CallableDescriptor.collectContributions(): List<CallableRef> {
    val declarations = mutableListOf<CallableRef>()

    declarations += allParameters
        .mapNotNull {
            val kind = it.contributionKind()
            when {
                kind == ContributionKind.INTERCEPTOR ->
                    CallableRef(it, contributionKind = ContributionKind.MODULE)
                kind != null -> CallableRef(it, contributionKind = kind)
                else -> null
            }
        }

    extensionReceiverParameter?.let { receiver ->
        declarations += CallableRef(receiver, contributionKind = ContributionKind.VALUE)
        declarations += receiver.type.memberScope.collectContributions(
            extensionReceiverParameter!!.type.toTypeRef()
        )
    }

    return declarations
}

fun ParameterDescriptor.contributionKind(): ContributionKind? {
    val userData = getUserData(DslMarkerUtils.FunctionTypeAnnotationsKey)
    val contributionParameters = getContributionParameters()

    return (this as Annotated).contributionKind() ?: type.contributionKind() ?: userData?.let {
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

fun ClassDescriptor.getContributionConstructors(): List<CallableRef> = constructors
    .mapNotNull { constructor ->
        if (constructor.isPrimary) {
            (constructor.contributionKind() ?: contributionKind())?.let { kind ->
                CallableRef(constructor, contributionKind = kind)
            }
        } else {
            constructor.contributionKind()?.let { kind ->
                CallableRef(constructor, contributionKind = kind)
            }
        }
    }
    .flatMap { declaration ->
        if (declaration.contributionKind == ContributionKind.VALUE) {
            allGivenTypes().map { type ->
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


fun ClassDescriptor.allGivenTypes(): List<TypeRef> = buildList<TypeRef> {
    this += defaultType.toTypeRef()
    this += defaultType.constructor.supertypes
        .filter { it.hasAnnotation(InjektFqNames.Given) }
        .map { it.toTypeRef() }
}

fun CallableRef.collectContributions(
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
                        .collectContributions(nextCallable.type)
                        .forEach {
                            it.collectContributions(
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
                        .collectContributions(type)
                        .forEach {
                            it.collectContributions(
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
                    .collectContributions(type)
                    .forEach {
                        it.collectContributions(
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
