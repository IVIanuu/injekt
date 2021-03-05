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
import com.ivianuu.injekt.compiler.analysis.hasDefaultValueIgnoringGiven
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.transform.toKotlinType
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

sealed class GivenNode {
    abstract val type: TypeRef
    abstract val originalType: TypeRef
    abstract val dependencies: List<GivenRequest>
    abstract val callableFqName: FqName
    abstract val callContext: CallContext
    abstract val ownerScope: ResolutionScope
    abstract val dependencyScope: ResolutionScope?
    abstract val lazyDependencies: Boolean
    abstract val isFrameworkGiven: Boolean
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
    override val lazyDependencies: Boolean
        get() = false
    override val originalType: TypeRef
        get() = callable.originalType
    override val isFrameworkGiven: Boolean
        get() = false
}

class SetGivenNode(
    override val type: TypeRef,
    override val ownerScope: ResolutionScope,
    override val dependencies: List<GivenRequest>,
) : GivenNode() {
    override val callableFqName: FqName = FqName("GivenSet<${type.render()}>")
    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val dependencyScope: ResolutionScope?
        get() = null
    override val lazyDependencies: Boolean
        get() = false
    override val originalType: TypeRef
        get() = type
    override val isFrameworkGiven: Boolean
        get() = true
}

class ProviderGivenNode(
    override val type: TypeRef,
    override val ownerScope: ResolutionScope,
    val declarationStore: DeclarationStore
) : GivenNode() {
    override val callableFqName: FqName = FqName("Provider<${type.render()} $ownerScope>")
    override val dependencies: List<GivenRequest> = listOf(
        GivenRequest(
            type = type.arguments.last(),
            required = true,
            callableFqName = callableFqName,
            parameterName = "instance".asNameId()
        )
    )

    override val dependencyScope = ResolutionScope(
        "Provider<${type.render()} $ownerScope>",
        parent = ownerScope,
        declarationStore = declarationStore,
        callContext = type.callContext,
        produceContributions = {
            type
                .toKotlinType(declarationStore)
                .memberScope
                .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
                .first()
                .valueParameters
                .map { ProviderParameterDescriptor(this, it) }
                .map { parameter ->
                    parameter
                        .toCallableRef(declarationStore = declarationStore)
                        .copy(
                            contributionKind = type.arguments[parameter.index].contributionKind
                        )
                }
        }
    )

    override val lazyDependencies: Boolean
        get() = true

    override val callContext: CallContext
        get() = CallContext.DEFAULT
    override val originalType: TypeRef
        get() = type
    override val isFrameworkGiven: Boolean
        get() = true

    class ProviderParameterDescriptor(
        val given: ProviderGivenNode,
        private val delegate: ValueParameterDescriptor
    ) : ValueParameterDescriptor by delegate
}

fun CallableRef.toGivenNode(
    type: TypeRef,
    ownerScope: ResolutionScope
): CallableGivenNode {
    val finalCallable = substitute(getSubstitutionMap(ownerScope.declarationStore, listOf(type to this.type)))
    return CallableGivenNode(
        type,
        finalCallable.getGivenRequests(ownerScope.declarationStore),
        ownerScope,
        finalCallable
    )
}

fun CallableRef.getGivenRequests(declarationStore: DeclarationStore): List<GivenRequest> {
    return callable.allParameters
        .filter {
            callable !is ClassConstructorDescriptor || it.name.asString() != "<this>"
        }
        .filter {
            it === callable.dispatchReceiverParameter ||
                    it.contributionKind(declarationStore) == ContributionKind.VALUE ||
                    parameterTypes[it.injektName()]!!.contributionKind == ContributionKind.VALUE
        }
        .map {
            val name = it.injektName()
            GivenRequest(
                type = parameterTypes[name]!!,
                required = it !is ValueParameterDescriptor || !it.hasDefaultValueIgnoringGiven,
                callableFqName = callable.fqNameSafe,
                parameterName = name.asNameId()
            )
        }
}

data class GivenRequest(
    val type: TypeRef,
    val required: Boolean,
    val callableFqName: FqName,
    val parameterName: Name
)
