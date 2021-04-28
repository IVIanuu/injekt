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

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun TypeRef.collectGivens(
    context: InjektContext,
    trace: BindingTrace
): List<CallableRef> {
    // special case to support @Given () -> Foo
    if (isGiven && isFunctionTypeWithOnlyGivenParameters) {
        return listOf(
            classifier.descriptor!!
                .defaultType
                .memberScope
                .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
                .first()
                .toCallableRef(context, trace)
                .let { callable ->
                    callable.copy(
                        type = arguments.last(),
                        isGiven = true,
                        parameterTypes = callable.parameterTypes.toMutableMap()
                            .also { it[callable.callable.dispatchReceiverParameter!!.injektName()] = this }
                    ).substitute(classifier.typeParameters.toMap(arguments))
                }
        )
    }

    val callables = mutableListOf<CallableRef>()
    val seen = mutableSetOf<TypeRef>()
    fun collectInner(type: TypeRef, overriddenDepth: Int) {
        if (type in seen) return
        seen += type
        val substitutionMap = type.classifier.typeParameters.toMap(type.arguments)
        callables += type.classifier.descriptor!!
            .defaultType
            .memberScope
            .collectGivens(context, trace)
            .filter {
                (it.callable as CallableMemberDescriptor)
                    .kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
            }
            .map { it.substitute(substitutionMap) }
            .map { callable ->
                callable.copy(
                    overriddenDepth = overriddenDepth,
                    owner = this.classifier,
                    isGiven = true,
                    parameterTypes = if (callable.callable.dispatchReceiverParameter != null) {
                        callable.parameterTypes.toMutableMap()
                            .also {
                                it[callable.callable.dispatchReceiverParameter!!.injektName()] = this
                            }
                    } else callable.parameterTypes
                )
            }
        type.superTypes.forEach { collectInner(it, overriddenDepth + 1) }
    }
    collectInner(this, 0)
    return callables
}

fun org.jetbrains.kotlin.resolve.scopes.ResolutionScope.collectGivens(
    context: InjektContext,
    trace: BindingTrace
): List<CallableRef> = getContributedDescriptors()
    .flatMap { declaration ->
        when (declaration) {
            is ClassDescriptor -> declaration
                .getGivenConstructors(context, trace) + listOfNotNull(
                declaration.companionObjectDescriptor
                    ?.thisAsReceiverParameter
                    ?.toCallableRef(context, trace)
                    ?.makeGiven()
            )
            is CallableMemberDescriptor -> if (declaration.isGiven(context, trace)) {
                listOf(
                    declaration.toCallableRef(context, trace)
                        .let { callable ->
                            callable.copy(
                                isGiven = true,
                                parameterTypes = callable.parameterTypes.toMutableMap()
                            )
                        }
                )
            } else emptyList()
            is VariableDescriptor -> if (declaration.isGiven(context, trace)) {
                listOf(declaration.toCallableRef(context, trace).makeGiven())
            } else emptyList()
            else -> emptyList()
        }
    }

fun Annotated.isGiven(context: InjektContext, trace: BindingTrace?): Boolean {
    @Suppress("IMPLICIT_CAST_TO_ANY")
    val key = if (this is KotlinType) System.identityHashCode(this) else this
    trace?.get(InjektWritableSlices.IS_GIVEN, key)?.let { return it }
    var isGiven = hasAnnotation(InjektFqNames.Given)
    if (!isGiven && this is PropertyDescriptor) {
        isGiven = overriddenTreeUniqueAsSequence(false)
            .map { it.containingDeclaration }
            .filterIsInstance<ClassDescriptor>()
            .flatMap { clazz ->
                val clazzClassifier = clazz.toClassifierRef(context, trace)
                clazz.unsubstitutedPrimaryConstructor
                    ?.valueParameters
                    ?.filter {
                        it.name == name &&
                                it.name in clazzClassifier.primaryConstructorPropertyParameters &&
                                it.isGiven(context, trace)
                    }
                    ?: emptyList()
            }
            .any() == true
    }
    if (!isGiven && this is ParameterDescriptor) {
        isGiven = type.isGiven(context, trace) ||
                containingDeclaration.safeAs<FunctionDescriptor>()
                    ?.takeIf { it.isDeserializedDeclaration() }
                    ?.let { context.callableInfoFor(it, trace) }
                    ?.let { name.asString() in it.givenParameters } == true
    }
    if (!isGiven && this is ClassConstructorDescriptor && isPrimary) {
        isGiven = constructedClass.isGiven(context, trace)
    }
    trace?.record(InjektWritableSlices.IS_GIVEN, key, isGiven)
    return isGiven
}

fun ClassDescriptor.getGivenConstructors(
    context: InjektContext,
    trace: BindingTrace
): List<CallableRef> {
    trace.get(InjektWritableSlices.GIVEN_CONSTRUCTORS, this)?.let { return it }
    val givenConstructors = constructors
        .filter { constructor ->
            constructor.hasAnnotation(InjektFqNames.Given) ||
                    (constructor.isPrimary && hasAnnotation(InjektFqNames.Given))
        }
        .map { constructor ->
            val callable = constructor.toCallableRef(context, trace)
            val qualifiedType = callable.type
                .copy(qualifiers = callable.type.classifier.qualifiers)
            callable.copy(
                isGiven = true,
                type = qualifiedType,
                originalType = qualifiedType
            )
        }
    trace.record(InjektWritableSlices.GIVEN_CONSTRUCTORS, this, givenConstructors)
    return givenConstructors
}

fun CallableRef.collectGivens(
    context: InjektContext,
    scope: ResolutionScope,
    trace: BindingTrace,
    addGiven: (CallableRef) -> Unit,
    addConstrainedGiven: (CallableRef) -> Unit,
    seen: MutableSet<CallableRef> = mutableSetOf()
) {
    if (this in seen) return
    seen += this
    if (!scope.canSee(this)) return

    if (source == null && typeParameters.any { it.isGivenConstraint }) {
        addConstrainedGiven(this)
        return
    }

    val nextCallable = if (type.isGiven && type.isFunctionTypeWithOnlyGivenParameters) {
        addGiven(this)
        copy(type = type.copy(frameworkKey = generateFrameworkKey()))
    }
    else this
    addGiven(nextCallable)

    nextCallable
        .type
        .collectGivens(context, trace)
        .forEach { innerCallable ->
            innerCallable.collectGivens(
                context,
                scope,
                trace,
                addGiven,
                addConstrainedGiven,
                seen
            )
        }
}

fun List<String>.collectImportGivens(context: InjektContext, trace: BindingTrace): List<CallableRef> =
    flatMap { importPath ->
        if (importPath.endsWith("*")) {
            val packageFqName = FqName(importPath.removeSuffix(".*"))
            context.memberScopeForFqName(packageFqName)!!
                .collectGivens(context, trace)
        } else {
            val fqName = FqName(importPath)
            val parentFqName = fqName.parent()
            val name = fqName.shortName()
            context.memberScopeForFqName(parentFqName)!!
                .collectGivens(context, trace)
                .filter {
                    it.callable.name == name ||
                            it.callable.safeAs<ClassConstructorDescriptor>()
                                ?.constructedClass
                                ?.name == name
                }
        }
    }

private fun ResolutionScope.canSee(callable: CallableRef): Boolean =
    callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
            callable.callable.visibility == DescriptorVisibilities.INTERNAL ||
            callable.callable.visibility == DescriptorVisibilities.LOCAL ||
            (callable.callable is ClassConstructorDescriptor &&
                    callable.type.classifier.isObject) ||
            callable.callable.parents.any { callableParent ->
                allScopes.any {
                    it.ownerDescriptor == callableParent ||
                            (it.ownerDescriptor is ClassDescriptor &&
                                    it.ownerDescriptor.toClassifierRef(context, trace) == callable.owner)
                }
            }
