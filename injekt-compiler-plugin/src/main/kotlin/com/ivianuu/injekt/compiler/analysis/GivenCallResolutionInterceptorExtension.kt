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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.getGivenParameters
import com.ivianuu.injekt.compiler.index.IndexStoreFactory
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.extensions.internal.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.calls.tower.PSICallResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
class GivenCallResolutionInterceptorExtension(
    private val indexStoreFactory: IndexStoreFactory
) : CallResolutionInterceptorExtension {
    private var declarationStore: DeclarationStore? = null
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
        val newCandidates = mutableListOf<FunctionDescriptor>()

        if (newCandidates.isEmpty()) newCandidates += candidates
            .map {
                if (declarationStore?.module != it.module) {
                    declarationStore = DeclarationStore(indexStoreFactory(it.module), it.module)
                }
                if (it.getGivenParameters(declarationStore!!).isNotEmpty()) {
                    it.toGivenFunctionDescriptor(declarationStore!!)
                } else {
                    it
                }
            }

        if (newCandidates.isEmpty()) newCandidates += candidates

        return newCandidates
    }
}