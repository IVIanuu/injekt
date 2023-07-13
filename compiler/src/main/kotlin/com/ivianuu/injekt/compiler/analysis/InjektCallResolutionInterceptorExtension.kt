/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(InternalNonStableExtensionPoints::class)

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.extensions.internal.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.calls.tower.PSICallResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

class InjektCallResolutionInterceptorExtension : CallResolutionInterceptorExtension {
  override fun interceptFunctionCandidates(
    candidates: Collection<FunctionDescriptor>,
    scopeTower: ImplicitScopeTower,
    resolutionContext: BasicCallResolutionContext,
    resolutionScope: ResolutionScope,
    callResolver: PSICallResolver,
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    extensionReceiver: ReceiverValueWithSmartCastInfo?
  ): Collection<FunctionDescriptor> = candidates.map {
    it.toInjectFunctionDescriptor(Context(it.module, resolutionContext.trace))
      ?: it
  }

  override fun interceptFunctionCandidates(
    candidates: Collection<FunctionDescriptor>,
    scopeTower: ImplicitScopeTower,
    resolutionContext: BasicCallResolutionContext,
    resolutionScope: ResolutionScope,
    callResolver: CallResolver,
    name: Name,
    location: LookupLocation
  ): Collection<FunctionDescriptor> = candidates.map {
    it.toInjectFunctionDescriptor(Context(it.module, resolutionContext.trace))
      ?: it
  }
}
