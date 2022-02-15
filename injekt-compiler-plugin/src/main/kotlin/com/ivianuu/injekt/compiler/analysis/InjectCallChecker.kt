/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.model.*

class InjectCallChecker(private val ctx: Context) : KtTreeVisitorVoid() {
  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    expression.getResolvedCall(ctx.trace!!.bindingContext)
      ?.let { checkCall(it) }
  }

  override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
    super.visitSimpleNameExpression(expression)
    expression.getResolvedCall(ctx.trace!!.bindingContext)
      ?.let { checkCall(it) }
  }

  override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
    super.visitConstructorDelegationCall(call)
    call.getResolvedCall(ctx.trace!!.bindingContext)
      ?.let { checkCall(it) }
  }

  private val checkedCalls = mutableSetOf<ResolvedCall<*>>()

  @OptIn(ExperimentalStdlibApi::class)
  private fun checkCall(resolvedCall: ResolvedCall<*>) {
    if (!checkedCalls.add(resolvedCall)) return

    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (resultingDescriptor !is InjectFunctionDescriptor) return

    val callExpression = resolvedCall.call.callElement

    val file = callExpression.containingKtFile

    val substitutionMap = buildMap<ClassifierRef, TypeRef> {
      for ((parameter, argument) in resolvedCall.typeArguments)
        this[parameter.toClassifierRef(ctx)] = argument.toTypeRef(ctx)

      fun TypeRef.putAll() {
        for ((index, parameter) in classifier.typeParameters.withIndex()) {
          val argument = arguments[index]
          if (argument.classifier != parameter)
            this@buildMap[parameter] = arguments[index]
        }
      }

      resolvedCall.dispatchReceiver?.type?.toTypeRef(ctx)?.putAll()
      resolvedCall.extensionReceiver?.type?.toTypeRef(ctx)?.putAll()
    }

    val callee = resultingDescriptor
      .toCallableRef(ctx)
      .substitute(substitutionMap)

    val valueArgumentsByIndex = resolvedCall.valueArguments
      .mapKeys { it.key.injektIndex() }

    val requests = callee.callable.valueParameters
      .transform {
        if (valueArgumentsByIndex[it.injektIndex()] is DefaultValueArgument && it.isInject(ctx))
          add(it.toInjectableRequest(callee))
      }

    if (requests.isEmpty()) return

    val scope = ElementInjectablesScope(ctx, callExpression)
    val graph = scope.resolveRequests(
      callee,
      requests,
      callExpression.lookupLocation
    ) { _, result ->
      if (result is ResolutionResult.Success.WithCandidate.Value &&
        result.candidate is CallableInjectable) {
        result.candidate.callable.import?.element?.let {
          ctx.trace!!.record(
            InjektWritableSlices.USED_IMPORT,
            SourcePosition(file.virtualFilePath, it.startOffset, it.endOffset),
            Unit
          )
        }
      }
    }

    when (graph) {
      is InjectionGraph.Success -> {
        ctx.trace!!.record(
          InjektWritableSlices.INJECTIONS_OCCURRED_IN_FILE,
          file.virtualFilePath,
          Unit
        )
        ctx.trace.record(
          InjektWritableSlices.INJECTION_GRAPH,
          SourcePosition(
            file.virtualFilePath,
            callExpression.startOffset,
            callExpression.endOffset
          ),
          graph
        )
      }
      is InjectionGraph.Error -> ctx.trace!!.report(
        InjektErrors.UNRESOLVED_INJECTION.on(callExpression, graph)
      )
    }
  }
}
