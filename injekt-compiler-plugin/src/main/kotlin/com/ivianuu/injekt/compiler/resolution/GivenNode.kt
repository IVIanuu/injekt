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
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.transform.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.inline.*
import org.jetbrains.kotlin.utils.addToStdlib.*

sealed class GivenNode {
    abstract val type: TypeRef
    abstract val originalType: TypeRef
    abstract val dependencies: List<GivenRequest>
    abstract val dependencyScope: ResolutionScope?
    abstract val callableFqName: FqName
    abstract val callContext: CallContext
    abstract val ownerScope: ResolutionScope
    abstract val cacheExpressionResultIfPossible: Boolean
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
    override val dependencyScope: ResolutionScope?
        get() = null
    override val originalType: TypeRef
        get() = callable.originalType
    override val cacheExpressionResultIfPossible: Boolean
        get() = false
}

class SetGivenNode(
    override val type: TypeRef,
    override val ownerScope: ResolutionScope,
    override val dependencies: List<GivenRequest>,
    val singleElementType: TypeRef,
    val collectionElementType: TypeRef
) : GivenNode() {
    override val callableFqName: FqName = FqName("com.ivianuu.injekt.givenSetOf<${type.arguments[0].render()}>")
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val dependencyScope: ResolutionScope?
        get() = null
    override val originalType: TypeRef
        get() = type.classifier.defaultType
    override val cacheExpressionResultIfPossible: Boolean
        get() = false
}

class ProviderGivenNode(
    override val type: TypeRef,
    override val ownerScope: ResolutionScope,
    dependencyCallContext: CallContext
) : GivenNode() {
    override val callableFqName: FqName = when (type.callContext) {
        CallContext.DEFAULT -> FqName("com.ivianuu.injekt.providerOf")
        CallContext.COMPOSABLE -> FqName("com.ivianuu.injekt.composableProviderOf")
        CallContext.SUSPEND -> FqName("com.ivianuu.injekt.suspendProviderOf")
    }
    override val dependencies: List<GivenRequest> = listOf(
        GivenRequest(
            type = type.arguments.last(),
            defaultStrategy = if (type.arguments.last().isNullableType)
                if (type.defaultOnAllErrors)
                    GivenRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
                else GivenRequest.DefaultStrategy.DEFAULT_IF_NOT_GIVEN
            else GivenRequest.DefaultStrategy.NONE,
            callableFqName = callableFqName,
            parameterName = "instance".asNameId(),
            isInline = false,
            isLazy = true,
            requestDescriptor = ownerScope.ownerDescriptor.cast()
        )
    )

    val parameterDescriptors = mutableListOf<ParameterDescriptor>()

    override val dependencyScope = ResolutionScope(
        callableFqName.shortName().asString(),
        parent = ownerScope,
        context = ownerScope.context,
        callContext = dependencyCallContext,
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
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val originalType: TypeRef
        get() = type.classifier.defaultType
    override val cacheExpressionResultIfPossible: Boolean
        get() = true
}

fun CallableRef.toGivenNode(
    type: TypeRef,
    ownerScope: ResolutionScope
): GivenNode {
    val finalCallable = substitute(getSubstitutionMap(ownerScope.context, listOf(type to this.type)))
    return CallableGivenNode(
        type,
        finalCallable.getGivenRequests(ownerScope.context, ownerScope.trace),
        ownerScope,
        finalCallable
    )
}

fun CallableRef.getGivenRequests(
    context: InjektContext,
    trace: BindingTrace?,
    callableFqNameProvider: (CallableDescriptor) -> FqName = { it.containingDeclaration.fqNameSafe }
): List<GivenRequest> = callable.allParameters
    .asSequence()
    .filter {
        callable !is ClassConstructorDescriptor || it.name.asString() != "<this>"
    }
    .filter {
        it === callable.dispatchReceiverParameter ||
                it === callable.extensionReceiverParameter ||
                it.isGiven(context, trace) ||
                parameterTypes[it.injektName()]!!.isGiven
    }
    .map { parameter ->
        val name = parameter.injektName()
        GivenRequest(
            type = parameterTypes[name]!!,
            defaultStrategy = if (parameter is ValueParameterDescriptor && parameter.hasDefaultValueIgnoringGiven) {
                if (name in defaultOnAllErrorParameters) GivenRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
                else GivenRequest.DefaultStrategy.DEFAULT_IF_NOT_GIVEN
            } else GivenRequest.DefaultStrategy.NONE,
            callableFqName = callableFqNameProvider(parameter),
            parameterName = name.asNameId(),
            isInline = InlineUtil.isInlineParameter(parameter),
            isLazy = false,
            requestDescriptor = callable
        )
    }
    .toList()

data class GivenRequest(
    val type: TypeRef,
    val defaultStrategy: DefaultStrategy,
    val callableFqName: FqName,
    val parameterName: Name,
    val isInline: Boolean,
    val isLazy: Boolean,
    val requestDescriptor: DeclarationDescriptorWithVisibility?
) {
    enum class DefaultStrategy {
        NONE, DEFAULT_IF_NOT_GIVEN, DEFAULT_ON_ALL_ERRORS
    }
}
