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
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.generateFrameworkKey
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.toMap
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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
                    ?.takeIf { it.isExternalDeclaration() }
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
        .let { givenConstructors ->
            if (givenConstructors.isEmpty() && hasAnnotation(InjektFqNames.Given)) {
                listOf(AbstractGivenFakeConstructor(this))
            } else givenConstructors
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

class AbstractGivenFakeConstructor(
    clazz: ClassDescriptor
) : FunctionDescriptorImpl(clazz, null, Annotations.EMPTY,
    Name.special("<init>"), CallableMemberDescriptor.Kind.SYNTHESIZED, clazz.source) {
    init {
        initialize(
            null,
            null,
            emptyList(),
            emptyList(),
            clazz.defaultType,
            null,
            clazz.visibility
        )
    }
    override fun createSubstitutedCopy(
        p0: DeclarationDescriptor,
        p1: FunctionDescriptor?,
        p2: CallableMemberDescriptor.Kind,
        p3: Name?,
        p4: Annotations,
        p5: SourceElement
    ): FunctionDescriptorImpl = TODO()
}

fun CallableRef.collectGivens(
    context: InjektContext,
    scope: ResolutionScope,
    trace: BindingTrace,
    addGiven: (CallableRef) -> Unit,
    addAbstractGiven: (CallableRef) -> Unit,
    addConstrainedGiven: (CallableRef) -> Unit,
    seen: MutableSet<CallableRef> = mutableSetOf()
) {
    if (this in seen) return
    seen += this
    if (!scope.canSee(this)) return

    if (constrainedGivenSource == null && typeParameters.any { it.isGivenConstraint }) {
        addConstrainedGiven(this)
        return
    }

    if (isForAbstractGiven(context, trace)) {
        addAbstractGiven(this)
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
                addAbstractGiven,
                addConstrainedGiven,
                seen
            )
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

fun CallableRef.isForAbstractGiven(context: InjektContext, trace: BindingTrace?): Boolean {
    return callable is AbstractGivenFakeConstructor ||
            type.classifier.descriptor!!
                .safeAs<ClassDescriptor>()?.let {
                    it.modality == Modality.ABSTRACT &&
                            it.isGiven(context, trace)
                } == true
}