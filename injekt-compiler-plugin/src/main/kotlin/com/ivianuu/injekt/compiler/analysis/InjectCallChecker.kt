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
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.extensions.*
import org.jetbrains.kotlin.types.typeUtil.*

class InjectCallChecker(private val withDeclarationGenerator: Boolean) : KtTreeVisitorVoid(), AnalysisHandlerExtension {
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
    if (resultingDescriptor.fqNameSafe != InjektFqNames.inject)
      return

    val callExpression = resolvedCall.call.callElement

    val file = callExpression.containingKtFile

    val callee = resultingDescriptor.toCallableRef(ctx!!)

    val requestedType = resolvedCall.typeArguments.values.single()

    val request = InjectableRequest(
      type = requestedType,
      callableFqName = callee.callable.fqNameSafe,
      resolvedCall.typeArguments
        .mapValues { it.value.asTypeProjection() },
      parameterName = "x".asNameId(),
      parameterIndex = 0
    )

    val scope = ElementInjectablesScope(ctx!!, callExpression)
    val graph = scope.resolveRequests(
      callee,
      listOf(request),
      callExpression.lookupLocation
    )

    // todo record lookup

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
