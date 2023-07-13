/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.INJECTIONS_OCCURRED_IN_FILE_KEY
import com.ivianuu.injekt.compiler.INJECTION_RESULT_KEY
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.cached
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.render
import com.ivianuu.injekt.compiler.reportError
import com.ivianuu.injekt.compiler.resolution.ElementInjectablesScope
import com.ivianuu.injekt.compiler.resolution.InjectionResult
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.resolveRequests
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toInjectableRequest
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.analyzer.AnalysisResult
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
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast

@OptIn(IDEAPluginsCompatibilityAPI::class) class InjectCallChecker : AnalysisHandlerExtension {
  private val checkedCalls = mutableSetOf<ResolvedCall<*>>()

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
    if (!checkedCalls.add(resolvedCall)) return

    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (resultingDescriptor.contextReceiverParameters.isEmpty()) return

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

    val requests = resultingDescriptor.contextReceiverParameters
      .map { it.toInjectableRequest(callee, ctx) }

    // we fill the context receivers list up with dummy's to ensure
    // that the compiler builds a correct ir tree
    // we replace those dummy's later in ir phase
    resolvedCall.contextReceivers.cast<ArrayList<ReceiverValue>>().run {
      clear()
      val dummyReceiver = ImplicitClassReceiver(ctx.module.builtIns.unit)
      repeat(resultingDescriptor.contextReceiverParameters.size) {
        add(dummyReceiver)
      }
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
