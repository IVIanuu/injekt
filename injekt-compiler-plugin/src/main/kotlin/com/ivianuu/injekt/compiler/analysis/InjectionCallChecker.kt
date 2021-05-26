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
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.model.*

class InjectionCallChecker(private val context: InjektContext) : CallChecker {
  override fun check(
    resolvedCall: ResolvedCall<*>,
    reportOn: PsiElement,
    context: CallCheckerContext
  ) {
    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (resultingDescriptor !is InjectFunctionDescriptor) return

    val callExpression = resolvedCall.call.callElement

    val file = try {
      callExpression.containingKtFile
    } catch (e: Throwable) {
      return
    }

    val filePath: String? = try {
      file.virtualFilePath
    } catch (e: Throwable) {
      null
    }

    val substitutionMap = resolvedCall.typeArguments
      .mapKeys { it.key.toClassifierRef(this.context, context.trace) }
      .mapValues { it.value.toTypeRef(this.context, context.trace) }
      .filter { it.key != it.value.classifier }

    val callable = resultingDescriptor
      .toCallableRef(this.context, context.trace)
      .substitute(substitutionMap)

    val valueArgumentsByIndex = resolvedCall.valueArguments
      .mapKeys { it.key.index }

    val requests = callable.callable.valueParameters
      .filter {
        valueArgumentsByIndex[it.index] is DefaultValueArgument &&
            it.isInject(this.context, context.trace)
      }
      .map { it.toInjectableRequest(callable) }
      .toList()

    if (requests.isEmpty()) return

    val scope = HierarchicalInjectablesScope(this.context, context.trace, context.scope, callExpression)

    when (val graph = scope.resolveRequests(requests, callExpression.lookupLocation) { result ->
      if (result.candidate is CallableInjectable) {
        context.trace.record(
          InjektWritableSlices.USED_INJECTABLE,
          result.candidate.callable.callable,
          Unit
        )
        if (filePath != null) {
          result.candidate.callable.import?.element?.let {
            context.trace.record(
              InjektWritableSlices.USED_IMPORT,
              SourcePosition(filePath, it.startOffset, it.endOffset),
              Unit
            )
          }
        }
      }
    }) {
      is InjectionGraph.Success -> {
        if (filePath != null && !isIde) {
          context.trace.record(
            InjektWritableSlices.INJECTIONS_OCCURRED_IN_FILE,
            filePath,
            Unit
          )
          context.trace.record(
            InjektWritableSlices.INJECTION_GRAPH,
            SourcePosition(
              filePath,
              callExpression.startOffset,
              callExpression.endOffset
            ),
            graph
          )
        }
      }
      is InjectionGraph.Error -> context.trace.report(
        InjektErrors.UNRESOLVED_INJECTION.on(callExpression, graph)
      )
    }
  }
}