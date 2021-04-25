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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.compiler.transform.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.extensions.internal.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.*
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.utils.addToStdlib.*

@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
class GivenCallResolutionInterceptorExtension : CallResolutionInterceptorExtension {
    private var context: InjektContext? = null

    override fun interceptFunctionCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: PSICallResolver,
        name: Name,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?,
    ): Collection<FunctionDescriptor> {
        if (context?.module != scopeTower.lexicalScope.ownerDescriptor.module) {
            context = InjektContext(scopeTower.lexicalScope.ownerDescriptor.module)
        }

        if (candidates.isEmpty() && dispatchReceiver == null && extensionReceiver == null)
            return candidates

        val newCandidates = candidates.toMutableList()

        if (dispatchReceiver != null || extensionReceiver != null) {
            val scope = HierarchicalResolutionScope(context!!,
                resolutionContext.scope, resolutionContext.trace)
            val conversions = listOfNotNull(dispatchReceiver, extensionReceiver)
                .map { it.receiverValue.type.toTypeRef(context!!, resolutionContext.trace) }
                .map {
                    context!!.conversionClassifier.defaultType
                        .typeWith(listOf(it, STAR_PROJECTION_TYPE))
                }
                .flatMap {
                    scope.givensForRequest(
                        GivenRequest(it, GivenRequest.DefaultStrategy.NONE,
                        FqName.ROOT, "l".asNameId(), false, false, null)
                    ) ?: emptyList()
                }
            if (conversions.isNotEmpty()) {
                newCandidates += generateSequence<HierarchicalScope>(resolutionContext.scope) { it.parent }
                    .flatMap { it.getContributedDescriptors() }
                    .filter { it.name == name }
                    .filterIsInstance<CallableMemberDescriptor>()
                    .flatMap { callable ->
                        val receiverType = callable.extensionReceiverParameter?.type?.toTypeRef(context!!, resolutionContext.trace)
                            ?: return@flatMap emptyList()
                        conversions.filter {
                            it.originalType.arguments[1]
                                .isSubTypeOf(context!!, receiverType)
                        }.map { conversion ->
                            SimpleFunctionDescriptorImpl.create(
                                callable.containingDeclaration,
                                callable.annotations,
                                callable.name,
                                CallableMemberDescriptor.Kind.SYNTHESIZED,
                                callable.source
                            ).apply {
                                initialize(
                                    DescriptorFactory.createExtensionReceiverParameterForCallable(
                                        this,
                                        conversion.originalType.arguments[1]
                                            .toKotlinType(context!!),
                                        callable.extensionReceiverParameter!!.annotations
                                    ),
                                    callable.dispatchReceiverParameter,
                                    callable.typeParameters,
                                    callable.valueParameters,
                                    callable.returnType,
                                    callable.modality,
                                    callable.visibility,
                                    null
                                )
                            }
                        }
                    }
            }
        }

        if (candidates.isNotEmpty()) {
            candidates
                .map { candidate ->
                    if (candidate.allParameters.any { it.isGiven(context!!, resolutionContext.trace) }) {
                        candidate.toGivenFunctionDescriptor(context!!, resolutionContext.trace)
                    } else {
                        candidate
                    }
                }
        }

        return newCandidates
    }
}