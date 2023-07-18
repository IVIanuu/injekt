/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.frontend

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol

class InjektFunctionCallChecker(
  private val session: FirSession
) : FirFunctionCallChecker() {
  override fun check(
    expression: FirFunctionCall,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    val calleeFunction = expression.calleeReference.toResolvedCallableSymbol()
      ?: return
    if (calleeFunction.origin != InjektFirDeclarationGenerationExtension.Key.origin)
      return

    /*val substitutionMap = buildMap {
      for ((parameter, argument) in calleeFunction.typeParameterSymbols.zip(expression.typeArguments))
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
      resolvedCall.contextReceivers.forEach { it.type.toTypeRef(ctx).putAll() }
    }

    val callee = resultingDescriptor
      .toCallableRef(ctx)
      .substitute(substitutionMap)

    val valueArgumentsByIndex = resolvedCall.valueArguments
      .mapKeys { it.key.injektIndex(ctx) }

    val requests = callee.callable.allParametersWithContext
      .transform {
        val index = it.injektIndex(ctx)
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
    }*/
  }
}
