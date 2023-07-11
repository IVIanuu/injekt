/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.INJECTIONS_OCCURRED_IN_FILE_KEY
import com.ivianuu.injekt.compiler.INJECTION_RESULT_KEY
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.allParametersWithContext
import com.ivianuu.injekt.compiler.cached
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.lookupLocation
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

@OptIn(IDEAPluginsCompatibilityAPI::class) class InjectCallChecker : AnalysisHandlerExtension {
  private val checkedCalls = mutableSetOf<ResolvedCall<*>>()

  override fun analysisCompleted(
    project: Project,
    module: ModuleDescriptor,
    bindingTrace: BindingTrace,
    files: Collection<KtFile>
  ): AnalysisResult? {
    with(Context(module, bindingTrace)) {
      files.forEach {
        it.accept(
          object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
              super.visitCallExpression(expression)
              expression.getResolvedCall(trace!!.bindingContext)
                ?.let { checkCall(it) }
            }

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
              super.visitSimpleNameExpression(expression)
              expression.getResolvedCall(trace!!.bindingContext)
                ?.let { checkCall(it) }
            }

            override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall) {
              super.visitConstructorDelegationCall(call)
              call.getResolvedCall(trace!!.bindingContext)
                ?.let { checkCall(it) }
            }
          }
        )
      }
    }

    return null
  }

  context(Context) private fun checkCall(resolvedCall: ResolvedCall<*>) {
    if (!checkedCalls.add(resolvedCall)) return

    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (resultingDescriptor !is InjectFunctionDescriptor) return

    val callExpression = resolvedCall.call.callElement

    val file = callExpression.containingKtFile

    val substitutionMap = buildMap {
      for ((parameter, argument) in resolvedCall.typeArguments)
        this[parameter.toClassifierRef()] = argument.toTypeRef()

      fun TypeRef.putAll() {
        for ((index, parameter) in classifier.typeParameters.withIndex()) {
          val argument = arguments[index]
          if (argument.classifier != parameter)
            this@buildMap[parameter] = arguments[index]
        }
      }

      resolvedCall.dispatchReceiver?.type?.toTypeRef()?.putAll()
      resolvedCall.extensionReceiver?.type?.toTypeRef()?.putAll()
    }

    val callee = resultingDescriptor
      .toCallableRef()
      .substitute(substitutionMap)

    val valueArgumentsByIndex = resolvedCall.valueArguments
      .mapKeys { it.key.injektIndex() }

    val requests = callee.callable.allParametersWithContext
      .transform {
        val index = it.injektIndex()
        if (valueArgumentsByIndex[index] is DefaultValueArgument && it.isInject())
          add(it.toInjectableRequest(callee))
      }

    if (requests.isEmpty()) return

    val scope = ElementInjectablesScope(callExpression)

    val location = callExpression.lookupLocation
    memberScopeForFqName(InjektFqNames.InjectablesPackage, location)
      ?.recordLookup(InjektFqNames.InjectablesLookup.shortName(), location)

    when (val result = with(scope) { resolveRequests(callee, requests) }) {
      is InjectionResult.Success -> {
        cached(INJECTIONS_OCCURRED_IN_FILE_KEY, file.virtualFilePath) { Unit }
        cached(
          INJECTION_RESULT_KEY,
          SourcePosition(
            file.virtualFilePath,
            callExpression.startOffset,
            callExpression.endOffset
          )
        ) { result }
      }
      is InjectionResult.Error -> reportError(callExpression, result.render())
    }
  }
}
