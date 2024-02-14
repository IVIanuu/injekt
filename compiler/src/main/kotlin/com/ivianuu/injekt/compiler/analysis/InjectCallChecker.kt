/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.INJECTIONS_OCCURRED_IN_FILE_KEY
import com.ivianuu.injekt.compiler.INJECTION_RESULT_KEY
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.cached
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.render
import com.ivianuu.injekt.compiler.reportError
import com.ivianuu.injekt.compiler.resolution.ElementInjectablesScope
import com.ivianuu.injekt.compiler.resolution.InjectionResult
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
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
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

@OptIn(IDEAPluginsCompatibilityAPI::class) class InjectCallChecker : AnalysisHandlerExtension {
  override fun analysisCompleted(
    project: Project,
    module: ModuleDescriptor,
    bindingTrace: BindingTrace,
    files: Collection<KtFile>
  ): AnalysisResult? {
    val ctx = Context(module, bindingTrace)
    files.forEach { file ->
      file.accept(
        object : KtTreeVisitorVoid() {
          override fun visitCallExpression(expression: KtCallExpression) {
            super.visitCallExpression(expression)
            expression.getResolvedCall(ctx.trace!!.bindingContext)
              ?.let { checkCall(it, ctx) }
          }

          override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
            super.visitSimpleNameExpression(expression)
            expression.getResolvedCall(ctx.trace!!.bindingContext)
              ?.let { checkCall(it, ctx) }
          }

          override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
            super.visitConstructorDelegationCall(call)
            call.getResolvedCall(ctx.trace!!.bindingContext)
              ?.let { checkCall(it, ctx) }
          }
        }
      )
    }
    return null
  }

  private fun checkCall(resolvedCall: ResolvedCall<*>, ctx: Context) {
    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (resultingDescriptor !is InjectFunctionDescriptor) return

    val callExpression = resolvedCall.call.callElement

    val file = callExpression.containingKtFile

    val substitutionMap = buildMap {
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

    val requests = callee.callable.allParameters
      .transform {
        val index = it.injektIndex()
        if (valueArgumentsByIndex[index] is DefaultValueArgument && it.isInject(ctx))
          add(it.toInjectableRequest(callee, ctx))
      }

    if (requests.isEmpty()) return

    val scope = ElementInjectablesScope(ctx, callExpression)

    val location = KotlinLookupLocation(callExpression)
    memberScopeForFqName(InjektFqNames.InjectablesPackage, location, ctx)
      ?.recordLookup(InjektFqNames.InjectablesLookup.shortName(), location)

    when (val result = scope.resolveRequests(callee, requests)) {
      is InjectionResult.Success -> {
        ctx.cached(INJECTIONS_OCCURRED_IN_FILE_KEY, file.virtualFilePath) { Unit }
        ctx.cached(
          INJECTION_RESULT_KEY,
          SourcePosition(
            file.virtualFilePath,
            callExpression.startOffset,
            callExpression.endOffset
          )
        ) { result }
      }
      is InjectionResult.Error -> ctx.reportError(callExpression, result.render())
    }
  }
}
