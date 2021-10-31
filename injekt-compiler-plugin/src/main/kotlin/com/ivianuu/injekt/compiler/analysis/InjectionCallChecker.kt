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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.WithInjektContext
import com.ivianuu.injekt.compiler.callableInfo
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
import com.ivianuu.injekt.compiler.resolution.resolveRequests
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toInjectableRequest
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt_shaded.Inject
import com.ivianuu.injekt_shaded.Provide
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InjectionCallChecker(@Inject private val context: InjektContext) : CallChecker {
  override fun check(
    resolvedCall: ResolvedCall<*>,
    reportOn: PsiElement,
    context: CallCheckerContext
  ) {
    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (!resultingDescriptor.hasAnnotation(injektFqNames.inject2) &&
      (resolvedCall !is VariableAsFunctionResolvedCall ||
          !resolvedCall.variableCall.resultingDescriptor.type.hasAnnotation(injektFqNames.inject2)) &&
      resolvedCall.dispatchReceiver?.type?.hasAnnotation(injektFqNames.inject2) != true) return

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

    @Provide val trace = context.trace

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
        ?.getResolvedCall(trace.bindingContext)
        ?.let {
          it.resultingDescriptor
            .toCallableRef()
            .substitute(it.getSubstitutionMap())
        }
        ?.type
      ?: resolvedCall.dispatchReceiver
        ?.type
        ?.takeIf { it.hasAnnotation(injektFqNames.inject2) }
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

    val requests = (callee.injectNParameters + (lambdaInjectParameters ?: emptyList()))
      .map { it.toInjectableRequest(callee) }

    if (requests.isEmpty()) return

    val scope = ElementInjectablesScope(callExpression)
    val graph = scope.resolveRequests(callee, requests, callExpression.lookupLocation) { _, result ->
      if (result is ResolutionResult.Success.WithCandidate.Value &&
        result.candidate is CallableInjectable) {
        if (filePath != null) {
          result.candidate.callable.import?.element?.let {
            trace.record(
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
        trace.record(
          InjektWritableSlices.INJECTIONS_OCCURRED_IN_FILE,
          filePath,
          Unit
        )
        trace.record(
          InjektWritableSlices.INJECTION_GRAPH,
          SourcePosition(
            filePath,
            callExpression.startOffset,
            callExpression.endOffset
          ),
          graph
        )
      }
      is InjectionGraph.Error -> trace.report(
        InjektErrors.UNRESOLVED_INJECTION.on(callExpression, graph)
      )
    }
  }

  @WithInjektContext
  private fun ResolvedCall<*>.getSubstitutionMap(): Map<ClassifierRef, TypeRef> = typeArguments
    .mapKeys { it.key.toClassifierRef() }
    .mapValues { it.value.toTypeRef() }
    .filter { it.key != it.value.classifier } +
      (dispatchReceiver?.type?.toTypeRef()?.let {
        it.classifier.typeParameters
          .zip(it.arguments)
          .filter { it.first != it.second.classifier }
          .toMap()
      } ?: emptyMap()) +
      (extensionReceiver?.type?.toTypeRef()?.let {
        it.classifier.typeParameters
          .zip(it.arguments)
          .filter { it.first != it.second.classifier }
          .toMap()
      } ?: emptyMap())
}
