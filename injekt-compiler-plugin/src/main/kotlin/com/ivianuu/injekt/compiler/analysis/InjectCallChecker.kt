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
import com.ivianuu.injekt.compiler.analysis.InjectFunctionDescriptor
import com.ivianuu.injekt.compiler.analysis.InjectNParameterDescriptor
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.getSubstitutionMap
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.injekt.compiler.resolution.CallableInjectable
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.ElementInjectablesScope
import com.ivianuu.injekt.compiler.resolution.InjectionGraph
import com.ivianuu.injekt.compiler.resolution.ResolutionResult
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.isInject
import com.ivianuu.injekt.compiler.resolution.resolveRequests
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toInjectableRequest
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getVariableResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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
    if (resultingDescriptor !is InjectFunctionDescriptor &&
      !resultingDescriptor.hasAnnotation(injektFqNames().inject2) &&
      !resultingDescriptor.hasAnnotation(injektFqNames().injectNInfo) &&
      (resolvedCall !is VariableAsFunctionResolvedCall ||
          (!resolvedCall.variableCall.resultingDescriptor.type.hasAnnotation(injektFqNames().inject2) &&
              !resolvedCall.variableCall.resultingDescriptor.type.hasAnnotation(injektFqNames().injectNInfo))) &&
      resolvedCall.dispatchReceiver?.type?.hasAnnotation(injektFqNames().inject2) != true &&
      resolvedCall.dispatchReceiver?.type?.hasAnnotation(injektFqNames().injectNInfo) != true) return

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

    val substitutionMap = resolvedCall.getSubstitutionMap()

    val injectLambdaType = resolvedCall
      .safeAs<VariableAsFunctionResolvedCall>()
      ?.variableCall
      ?.resultingDescriptor
      ?.callableInfo()
      ?.type
      ?: resolvedCall.dispatchReceiver
        ?.safeAs<ExpressionReceiver>()
        ?.expression
        ?.getResolvedCall(trace()!!.bindingContext)
        ?.let {
          it.resultingDescriptor
            .toCallableRef()
            .substitute(it.getSubstitutionMap())
        }
        ?.type
      ?: resolvedCall.dispatchReceiver
        ?.type
        ?.takeIf { it.hasAnnotation(injektFqNames().inject2) }
        ?.toTypeRef()

    val lambdaInjectParameters = injectLambdaType
      ?.injectNTypes
      ?.mapIndexed { index, injectNType ->
        InjectNParameterDescriptor(
          resultingDescriptor.containingDeclaration,
          resultingDescriptor.valueParameters.size + index,
          injectNType.substitute(substitutionMap)
        )
      }

    val callee = resultingDescriptor
      .toCallableRef()
      .let {
        if (lambdaInjectParameters == null) it
        else it.copy(
          parameterTypes = it.parameterTypes + lambdaInjectParameters
            .map { it.index to it.typeRef }
        )
      }
      .substitute(substitutionMap)

    val valueArgumentsByIndex = resolvedCall.valueArguments
      .mapKeys { it.key.injektIndex() }

    val requests = (callee.callable.valueParameters +
        callee.injectNParameters +
        (lambdaInjectParameters ?: emptyList()))
      .filter {
        val argument = valueArgumentsByIndex[it.injektIndex()]
        (argument == null || argument is DefaultValueArgument) && it.isInject()
      }
      .map { it.toInjectableRequest(callee) }

    if (requests.isEmpty()) return

    val scope = ElementInjectablesScope(callExpression)
    val graph = scope.resolveRequests(callee, requests, callExpression.lookupLocation) { _, result ->
      if (result is ResolutionResult.Success.WithCandidate.Value &&
        result.candidate is CallableInjectable
      ) {
        if (filePath != null) {
          result.candidate.callable.import?.element?.let {
            trace()!!.record(
              InjektWritableSlices.USED_IMPORT,
              SourcePosition(filePath, it.startOffset, it.endOffset),
              Unit
            )
          }
        }
      }
    }

    when (graph) {
      is InjectionGraph.Success -> if (filePath != null) {
        trace()!!.record(
          InjektWritableSlices.INJECTIONS_OCCURRED_IN_FILE,
          filePath,
          Unit
        )
        trace()!!.record(
          InjektWritableSlices.INJECTION_GRAPH,
          SourcePosition(
            filePath,
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
