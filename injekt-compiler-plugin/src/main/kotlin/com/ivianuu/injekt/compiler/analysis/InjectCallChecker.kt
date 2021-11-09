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

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.injekt.compiler.resolution.CallableInjectable
import com.ivianuu.injekt.compiler.resolution.ComponentInjectable
import com.ivianuu.injekt.compiler.resolution.ElementInjectablesScope
import com.ivianuu.injekt.compiler.resolution.InjectionGraph
import com.ivianuu.injekt.compiler.resolution.ResolutionResult
import com.ivianuu.injekt.compiler.resolution.isInject
import com.ivianuu.injekt.compiler.resolution.resolveRequests
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toInjectableRequest
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class InjectCallChecker(@Inject private val ctx: Context) : KtTreeVisitorVoid() {
  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    expression.getResolvedCall(trace()!!.bindingContext)
      ?.let { checkCall(it) }
  }

  override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
    super.visitSimpleNameExpression(expression)
    expression.getResolvedCall(trace()!!.bindingContext)
      ?.let { checkCall(it) }
  }

  override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
    super.visitConstructorDelegationCall(call)
    call.getResolvedCall(trace()!!.bindingContext)
      ?.let { checkCall(it) }
  }

  private val checkedCalls = mutableSetOf<ResolvedCall<*>>()

  private fun checkCall(resolvedCall: ResolvedCall<*>) {
    if (resolvedCall in checkedCalls) return
    checkedCalls += resolvedCall

    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (resultingDescriptor !is InjectFunctionDescriptor) return

    val callExpression = resolvedCall.call.callElement

    val file = callExpression.containingKtFile

    val substitutionMap = resolvedCall.typeArguments
      .mapKeys { it.key.toClassifierRef() }
      .mapValues { it.value.toTypeRef() }
      .filter { it.key != it.value.classifier } +
        (resolvedCall.dispatchReceiver?.type?.toTypeRef()?.let {
          it.classifier.typeParameters
            .zip(it.arguments)
            .filter { it.first != it.second.classifier }
            .toMap()
        } ?: emptyMap()) +
        (resolvedCall.extensionReceiver?.type?.toTypeRef()?.let {
          it.classifier.typeParameters
            .zip(it.arguments)
            .filter { it.first != it.second.classifier }
            .toMap()
        } ?: emptyMap())

    val callee = resultingDescriptor
      .toCallableRef()
      .substitute(substitutionMap)

    val valueArgumentsByIndex = resolvedCall.valueArguments
      .mapKeys { it.key.injektIndex() }

    val requests = callee.callable.valueParameters
      .filter {
        valueArgumentsByIndex[it.injektIndex()] is DefaultValueArgument
            && it.isInject()
      }
      .map { it.toInjectableRequest(callee) }

    if (requests.isEmpty()) return

    val scope = ElementInjectablesScope(callExpression)
    val graph = scope.resolveRequests(
      callee,
      requests,
      callExpression.lookupLocation
    ) { _, result ->
      if (result is ResolutionResult.Success.WithCandidate.Value) {
        if (result.candidate is CallableInjectable) {
          result.candidate.callable.import?.element?.let {
            trace()!!.record(
              InjektWritableSlices.USED_IMPORT,
              SourcePosition(file.virtualFilePath, it.startOffset, it.endOffset),
              Unit
            )
          }
        } else if (result.candidate is ComponentInjectable) {
          result.candidate.component.import?.element?.let {
            trace()!!.record(
              InjektWritableSlices.USED_IMPORT,
              SourcePosition(file.virtualFilePath, it.startOffset, it.endOffset),
              Unit
            )
          }
          result.candidate.entryPoints.forEach {
            it.import?.element?.let {
              trace()!!.record(
                InjektWritableSlices.USED_IMPORT,
                SourcePosition(file.virtualFilePath, it.startOffset, it.endOffset),
                Unit
              )
            }
          }
        }
      }
    }

    when (graph) {
      is InjectionGraph.Success -> {
        trace()!!.record(
          InjektWritableSlices.INJECTIONS_OCCURRED_IN_FILE,
          file.virtualFilePath,
          Unit
        )
        trace()!!.record(
          InjektWritableSlices.INJECTION_GRAPH,
          SourcePosition(
            file.virtualFilePath,
            callExpression.startOffset,
            callExpression.endOffset
          ),
          graph
        )
      }
      is InjectionGraph.Error -> trace()!!.report(
        InjektErrors.UNRESOLVED_INJECTION.on(callExpression, graph)
      )
    }
  }
}
