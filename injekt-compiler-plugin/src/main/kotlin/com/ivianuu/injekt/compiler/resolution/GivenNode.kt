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
import com.ivianuu.injekt.compiler.analysis.hasDefaultValueIgnoringGiven
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.toMap
import com.ivianuu.injekt.compiler.transform.toKotlinType
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class GivenNode {
    abstract val type: TypeRef
    abstract val originalType: TypeRef
    abstract val dependencies: List<GivenRequest>
    abstract val dependencyScopes: Map<GivenRequest, ResolutionScope>
    abstract val callableFqName: FqName
    abstract val callContext: CallContext
    abstract val ownerScope: ResolutionScope
    abstract val isFrameworkGiven: Boolean
    abstract val expressionStrategy: ExpressionStrategy
    enum class ExpressionStrategy {
        // inserts the expression at each request
        INLINE,
        // caches the result of the expression in a variable
        CACHE,
        // extracts the expression into a local function
        WRAP
    }
}

class CallableGivenNode(
    override val type: TypeRef,
    override val dependencies: List<GivenRequest>,
    override val ownerScope: ResolutionScope,
    val callable: CallableRef,
) : GivenNode() {
    override val callableFqName: FqName = if (callable.callable is ClassConstructorDescriptor)
        callable.callable.constructedClass.fqNameSafe
    else callable.callable.fqNameSafe
    override val callContext: CallContext
        get() = callable.callContext
    override val dependencyScopes: Map<GivenRequest, ResolutionScope>
        get() = emptyMap()
    override val originalType: TypeRef
        get() = callable.originalType
    override val isFrameworkGiven: Boolean
        get() = false
    override val expressionStrategy: ExpressionStrategy
        get() = ExpressionStrategy.WRAP
}

class SetGivenNode(
    override val type: TypeRef,
    override val ownerScope: ResolutionScope,
    override val dependencies: List<GivenRequest>,
) : GivenNode() {
    override val callableFqName: FqName = FqName("com.ivianuu.injekt.givenSetOf<${type.render()}>")
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val dependencyScopes: Map<GivenRequest, ResolutionScope>
        get() = emptyMap()
    override val originalType: TypeRef
        get() = type.classifier.defaultType
    override val isFrameworkGiven: Boolean
        get() = true
    override val expressionStrategy: ExpressionStrategy
        get() = ExpressionStrategy.WRAP
}

class ProviderGivenNode(
    override val type: TypeRef,
    override val ownerScope: ResolutionScope
) : GivenNode() {
    override val callableFqName: FqName = when (type.callContext) {
        CallContext.DEFAULT -> FqName("com.ivianuu.injekt.providerOf")
        CallContext.COMPOSABLE -> FqName("com.ivianuu.injekt.composableProviderOf")
        CallContext.SUSPEND -> FqName("com.ivianuu.injekt.suspendProviderOf")
    }
    override val dependencies: List<GivenRequest> = listOf(
        GivenRequest(
            type = type.arguments.last(),
            isRequired = true,
            callableFqName = callableFqName,
            parameterName = "instance".asNameId(),
            isInline = false,
            isLazy = true
        )
    )

    val parameterDescriptors = mutableListOf<ParameterDescriptor>()

    override val dependencyScopes = mapOf(
        dependencies.single() to ResolutionScope(
            callableFqName.shortName().asString(),
            parent = ownerScope,
            context = ownerScope.context,
            callContext = type.callContext,
            ownerDescriptor = ownerScope.ownerDescriptor,
            trace = ownerScope.trace,
            initialGivens = type
                .toKotlinType(ownerScope.context)
                .memberScope
                .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
                .first()
                .valueParameters
                .asSequence()
                .onEach { parameterDescriptors += it }
                .mapIndexed { index, parameter ->
                    parameter
                        .toCallableRef(ownerScope.context, ownerScope.trace)
                        .copy(isGiven = true, type = type.arguments[index])
                }
                .toList()
        )
    )
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val originalType: TypeRef
        get() = type.classifier.defaultType
    override val isFrameworkGiven: Boolean
        get() = true
    override val expressionStrategy: ExpressionStrategy
        get() = ExpressionStrategy.CACHE
}

class AbstractGivenNode(
    override val type: TypeRef,
    override val ownerScope: ResolutionScope
) : GivenNode() {
    override val callableFqName: FqName = FqName(type.classifier.fqName.asString() + "Impl")

    val superConstructor = type.classifier
        .descriptor
        .cast<ClassDescriptor>()
        .getGivenConstructor(ownerScope.context, ownerScope.trace)

    val requestCallables: List<CallableRef> = type
        .classifier
        .descriptor!!
        .defaultType
        .memberScope
        .collectGivens(ownerScope.context, ownerScope.trace, type,
            type.classifier.typeParameters.toMap(type.arguments))
        .filter {
            superConstructor == null ||
                    it.callable.name !in type.classifier.primaryConstructorPropertyParameters
        }

    val requestsByRequestCallables = requestCallables
        .associateWith { requestCallable ->
            GivenRequest(
                type = requestCallable.type,
                isRequired = requestCallable.callable
                    .cast<CallableMemberDescriptor>()
                    .modality == Modality.ABSTRACT,
                callableFqName = callableFqName,
                parameterName = requestCallable.callable.name,
                isInline = false,
                isLazy = true
            )
        }

    val constructorDependencies = superConstructor
        ?.getGivenRequests(ownerScope.context, ownerScope.trace) { callableFqName }
        ?: emptyList()

    override val dependencies: List<GivenRequest> = requestsByRequestCallables
        .values + constructorDependencies

    val dependencyScopesByRequestCallable = requestCallables
        .associateWith { requestCallable ->
            ResolutionScope(
                name = callableFqName.child(requestCallable.callable.name).asString(),
                parent = ownerScope,
                context = ownerScope.context,
                callContext = requestCallable.callContext,
                ownerDescriptor = ownerScope.ownerDescriptor,
                trace = ownerScope.trace,
                initialGivens = requestCallable.callable.allParameters
                    .asSequence()
                    .filter { it != requestCallable.callable.dispatchReceiverParameter }
                    .map { parameter ->
                        parameter.toCallableRef(ownerScope.context, ownerScope.trace)
                    }
                    .toList()
            )
        }

    override val dependencyScopes: Map<GivenRequest, ResolutionScope> = dependencyScopesByRequestCallable
        .mapKeys {
            val index = requestCallables.indexOf(it.key)
            dependencies[index]
        }

    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val originalType: TypeRef
        get() = type.classifier.defaultType
    override val isFrameworkGiven: Boolean
        get() = true
    override val expressionStrategy: ExpressionStrategy
        get() = ExpressionStrategy.INLINE
}

fun CallableRef.toGivenNode(
    type: TypeRef,
    ownerScope: ResolutionScope
): GivenNode {
    val finalCallable = substitute(getSubstitutionMap(ownerScope.context, listOf(type to this.type)))
    return if (finalCallable.isForAbstractGiven(ownerScope.context, ownerScope.trace)) {
        AbstractGivenNode(type, ownerScope)
    } else {
        CallableGivenNode(
            type,
            finalCallable.getGivenRequests(ownerScope.context, ownerScope.trace),
            ownerScope,
            finalCallable
        )
    }
}

fun CallableRef.getGivenRequests(
    context: InjektContext,
    trace: BindingTrace?,
    callableFqNameProvider: (CallableDescriptor) -> FqName = { it.fqNameSafe }
): List<GivenRequest> = callable.allParameters
    .asSequence()
    .filter {
        callable !is ClassConstructorDescriptor || it.name.asString() != "<this>"
    }
    .filter {
        it === callable.dispatchReceiverParameter ||
                it.isGiven(context, trace) ||
                parameterTypes[it.injektName()]!!.isGiven
    }
    .map { callable ->
        val name = callable.injektName()
        GivenRequest(
            type = parameterTypes[name]!!,
            isRequired = callable !is ValueParameterDescriptor || !callable.hasDefaultValueIgnoringGiven,
            callableFqName = callableFqNameProvider(callable),
            parameterName = name.asNameId(),
            isInline = this.callable is FunctionDescriptor && this.callable.isInline,
            isLazy = false
        )
    }
    .toList()

data class GivenRequest(
    val type: TypeRef,
    val isRequired: Boolean,
    val callableFqName: FqName,
    val parameterName: Name,
    val isInline: Boolean,
    val isLazy: Boolean
)
