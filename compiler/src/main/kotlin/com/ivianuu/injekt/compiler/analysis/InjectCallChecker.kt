/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.extensions.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.*

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

    val info = resultingDescriptor.callableInfo(ctx)
    if (info.injectParameters.isEmpty()) return

    val callExpression = resolvedCall.call.callElement

    val file = callExpression.containingKtFile

    val substitutor = NewTypeSubstitutorByConstructorMap(
      buildMap {
        for ((parameter, argument) in resolvedCall.typeArguments) {
          this[parameter.typeConstructor(ctx)] = argument.unwrap()
        }

        fun KotlinType.putAll() {
          for ((index, parameter) in constructor.parameters.withIndex()) {
            val argument = arguments[index]
            if (argument.type != parameter.defaultType)
              this@buildMap[parameter.typeConstructor(ctx)] = arguments[index].type.unwrap()
          }
        }

        resolvedCall.dispatchReceiver?.type?.putAll()
        resolvedCall.extensionReceiver?.type?.putAll()
      }
    )

    val callee = resultingDescriptor
      .toCallableRef(ctx)
      .substitute(substitutor)

    val valueArgumentsByIndex = resolvedCall.valueArguments
      .mapKeys { it.key.injektIndex() }

    val requests = callee.callable.allParameters
      .transform {
        val index = it.injektIndex()
        if (valueArgumentsByIndex[index] is DefaultValueArgument && index in info.injectParameters)
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
