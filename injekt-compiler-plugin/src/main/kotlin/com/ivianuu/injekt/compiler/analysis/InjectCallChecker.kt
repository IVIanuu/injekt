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
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

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

    val substitutionMap = buildMap<TypeConstructor, TypeProjection> {
      for ((parameter, argument) in resolvedCall.typeArguments)
        this[parameter.typeConstructor] = argument.asTypeProjection()

      for ((parameter, argument) in resolvedCall.typeArguments)
        this[parameter.typeConstructor] = argument.asTypeProjection()

      fun KotlinType.putAll() {
        for ((index, parameter) in constructor.parameters.withIndex()) {
          val argument = arguments[index]
          if (argument.type.constructor.declarationDescriptor != parameter)
            this@buildMap[parameter.typeConstructor] = arguments[index]
        }
      }

      resolvedCall.dispatchReceiver?.type?.putAll()
      resolvedCall.extensionReceiver?.type?.putAll()
    }

    val callee = resultingDescriptor
      .toCallableRef(ctx)
      .substitute(TypeSubstitutor.create(substitutionMap))

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
