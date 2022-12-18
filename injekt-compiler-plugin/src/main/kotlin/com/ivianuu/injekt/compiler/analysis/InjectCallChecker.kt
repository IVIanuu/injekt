/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.injekt.compiler.resolution.CallableInjectable
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.ElementInjectablesScope
import com.ivianuu.injekt.compiler.resolution.InjectionResult
import com.ivianuu.injekt.compiler.resolution.ResolutionResult
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.isInject
import com.ivianuu.injekt.compiler.resolution.resolveRequests
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toInjectableRequest
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.transform
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

@OptIn(IDEAPluginsCompatibilityAPI::class) class InjectCallChecker(
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
    if ((completionCount < 1 && withDeclarationGenerator) ||
      (completionCount > 0 && !withDeclarationGenerator))
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
      .substitute(substitutionMap)

    val valueArgumentsByIndex = resolvedCall.valueArguments
      .mapKeys { it.key.injektIndex() }

    val requests = callee.callable.valueParameters
      .transform {
        if (valueArgumentsByIndex[it.injektIndex()] is DefaultValueArgument && it.isInject(ctx!!))
          add(it.toInjectableRequest(callee))
      }

    if (requests.isEmpty()) return

    val scope = ElementInjectablesScope(ctx!!, callExpression)
    val location = callExpression.lookupLocation
    val lookups = ctx!!.trace!!.getOrPut(InjektWritableSlices.LOOKUPS, file.virtualFilePath) {
      mutableMapOf()
    }.getOrPut(location) { mutableSetOf() }
    val result = scope.resolveRequests(
      callee,
      requests,
      location,
      lookups
    ) { _, result ->
      if (result is ResolutionResult.Success.Value &&
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

    when (result) {
      is InjectionResult.Success -> {
        ctx!!.trace!!.record(
          InjektWritableSlices.INJECTIONS_OCCURRED_IN_FILE,
          file.virtualFilePath,
          Unit
        )
        ctx!!.trace!!.record(
          InjektWritableSlices.INJECTION_RESULT,
          SourcePosition(
            file.virtualFilePath,
            callExpression.startOffset,
            callExpression.endOffset
          ),
          result
        )
      }
      is InjectionResult.Error -> ctx!!.trace!!.report(
        InjektErrors.UNRESOLVED_INJECTION.on(callExpression, result)
      )
    }
  }
}
