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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.inline.*

class GivenCallChecker(private val context: InjektContext) : CallChecker {
  override fun check(
    resolvedCall: ResolvedCall<*>,
    reportOn: PsiElement,
    context: CallCheckerContext
  ) {
    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (resultingDescriptor !is FunctionDescriptor) return

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

    if (resultingDescriptor.valueParameters.none {
        it.isProvide(this.context, context.trace)
      }) return

    val substitutionMap = resolvedCall.typeArguments
      .mapKeys { it.key.toClassifierRef(this.context, context.trace) }
      .mapValues { it.value.toTypeRef(this.context, context.trace) }
      .filter { it.key != it.value.classifier }

    val callable = resultingDescriptor.toCallableRef(this.context, context.trace)
      .substitute(substitutionMap)

    val requests = callable.givenParameters
      .asSequence()
      .map { parameterName ->
        callable.callable.valueParameters.single {
          it.injektName() == parameterName
        }
      }
      .filter { resolvedCall.valueArguments[it] is DefaultValueArgument }
      .map { parameter ->
        GivenRequest(
          type = callable.parameterTypes[parameter.injektName()]!!,
          defaultStrategy = if (parameter is ValueParameterDescriptor &&
            parameter.hasDefaultValueIgnoringGiven
          ) {
            if (parameter.injektName() in callable.defaultOnAllErrorParameters)
              GivenRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
            else GivenRequest.DefaultStrategy.DEFAULT_IF_NOT_GIVEN
          } else GivenRequest.DefaultStrategy.NONE,
          callableFqName = resultingDescriptor.fqNameSafe,
          parameterName = parameter.injektName().asNameId(),
          isInline = InlineUtil.isInlineParameter(parameter),
          isLazy = false
        )
      }
      .toList()

    if (requests.isEmpty()) return

    val scope = HierarchicalResolutionScope(this.context, context.scope, context.trace)

    when (val graph = scope.resolveRequests(requests, callExpression.lookupLocation) { result ->
      if (result.candidate is CallableGivenNode) {
        context.trace.record(
          InjektWritableSlices.USED_GIVEN,
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
      is GivenGraph.Success -> {
        if (filePath != null && !isIde) {
          context.trace.record(
            InjektWritableSlices.FILE_HAS_GIVEN_CALLS,
            filePath,
            Unit
          )
          context.trace.record(
            InjektWritableSlices.GIVEN_GRAPH,
            SourcePosition(
              filePath,
              callExpression.startOffset,
              callExpression.endOffset
            ),
            graph
          )
        }
      }
      is GivenGraph.Error -> context.trace.report(
        InjektErrors.UNRESOLVED_GIVEN
          .on(reportOn, graph)
      )
    }
  }
}