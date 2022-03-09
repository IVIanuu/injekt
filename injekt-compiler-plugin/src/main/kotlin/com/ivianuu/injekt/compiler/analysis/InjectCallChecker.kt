/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.extensions.*

class InjectCallChecker(
  private val withDeclarationGenerator: Boolean
) : KtTreeVisitorVoid(), AnalysisHandlerExtension {
  private var completionCount = 0
  private val checkedCalls = mutableSetOf<ResolvedCall<*>>()
  private var ctx: Context? = null

  override fun analysisCompleted(
    project: Project,
    module: ModuleDescriptor,
    bindingTrace: BindingTrace,
    files: Collection<KtFile>
  ): AnalysisResult? {
    if (completionCount < 1 && withDeclarationGenerator)
      return null.also { completionCount++ }
    if (completionCount > 0 && !withDeclarationGenerator)
      return null.also { completionCount++ }

    ctx = Context(module, bindingTrace)
    files.forEach { it.accept(this) }
    ctx = null

    return null
  }

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    expression.getResolvedCall(ctx!!.trace!!.bindingContext)
      ?.let { checkCall(it) }
  }

  override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
    super.visitSimpleNameExpression(expression)
    expression.getResolvedCall(ctx!!.trace!!.bindingContext)
      ?.let { checkCall(it) }
  }

  override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
    super.visitConstructorDelegationCall(call)
    call.getResolvedCall(ctx!!.trace!!.bindingContext)
      ?.let { checkCall(it) }
  }

  @OptIn(ExperimentalStdlibApi::class)
  private fun checkCall(resolvedCall: ResolvedCall<*>) {
    if (!checkedCalls.add(resolvedCall)) return

    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (resultingDescriptor !is InjectFunctionDescriptor) return

    val callExpression = resolvedCall.call.callElement

    val file = callExpression.containingKtFile

    val substitutionMap = buildMap<ClassifierRef, TypeRef> {
      for ((parameter, argument) in resolvedCall.typeArguments)
        this[parameter.toClassifierRef(ctx!!)] = argument.toTypeRef(ctx!!)

      fun TypeRef.putAll() {
        for ((index, parameter) in classifier.typeParameters.withIndex()) {
          val argument = arguments[index]
          if (argument.classifier != parameter)
            this@buildMap[parameter] = arguments[index]
        }
      }

      resolvedCall.dispatchReceiver?.type?.toTypeRef(ctx!!)?.putAll()
      resolvedCall.extensionReceiver?.type?.toTypeRef(ctx!!)?.putAll()
    }

    val callee = resultingDescriptor
      .toCallableRef(ctx!!)
      .substitute(substitutionMap, ctx!!)

    val valueArgumentsByIndex = resolvedCall.valueArguments
      .mapKeys { it.key.injektIndex() }

    val requests = callee.callable.valueParameters
      .transform {
        if (valueArgumentsByIndex[it.injektIndex()] is DefaultValueArgument && it.isInject(ctx!!))
          add(it.toInjectableRequest(callee, ctx!!))
      }

    if (requests.isEmpty()) return

    val scope = ElementInjectablesScope(ctx!!, callExpression)
    val graph = scope.resolveRequests(
      callee,
      requests,
      callExpression.lookupLocation
    ) { _, result ->
      if (result is ResolutionResult.Success.WithCandidate.Value &&
        result.candidate is CallableInjectable) {
        result.candidate.callable.import?.element?.let {
          ctx!!.trace!!.record(
            InjektWritableSlices.USED_IMPORT,
            SourcePosition(file.virtualFilePath, it.startOffset, it.endOffset),
            Unit
          )
        }
      }
    }

    when (graph) {
      is InjectionGraph.Success -> {
        ctx!!.trace!!.record(
          InjektWritableSlices.INJECTIONS_OCCURRED_IN_FILE,
          file.virtualFilePath,
          Unit
        )
        ctx!!.trace!!.record(
          InjektWritableSlices.INJECTION_GRAPH,
          SourcePosition(
            file.virtualFilePath,
            callExpression.startOffset,
            callExpression.endOffset
          ),
          graph
        )
      }
      is InjectionGraph.Error -> ctx!!.trace!!.report(
        InjektErrors.UNRESOLVED_INJECTION.on(callExpression, graph)
      )
    }
  }
}
