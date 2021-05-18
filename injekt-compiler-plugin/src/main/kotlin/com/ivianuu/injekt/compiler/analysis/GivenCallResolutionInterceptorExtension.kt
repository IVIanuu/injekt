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
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.extensions.internal.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.*

@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
class GivenCallResolutionInterceptorExtension : CallResolutionInterceptorExtension {
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
  ): Collection<FunctionDescriptor> = if (candidates.isEmpty()) emptyList()
  else {
    val context = resolutionContext.scope.ownerDescriptor.module.injektContext
    candidates
      .map { candidate ->
        if (candidate.allParameters.any { it.isProvided(context, resolutionContext.trace) }) {
          candidate.toGivenFunctionDescriptor(context, resolutionContext.trace)
        } else {
          candidate
        }
      }
  }
}