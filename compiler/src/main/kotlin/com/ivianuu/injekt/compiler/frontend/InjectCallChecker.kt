package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.types.*

class InjectCallChecker(private val ctx: InjektContext) : FirFunctionCallChecker(
  MppCheckerKind.Common) {
  override fun check(
    expression: FirFunctionCall,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    val callee = expression.calleeReference.toResolvedCallableSymbol()
      ?: return

    val info = callee.callableInfo(ctx)

    if (info.injectParameters.isEmpty()) return

    val substitutionMap = buildMap {
      expression.typeArguments.forEachIndexed { index, argument ->
        val parameter = callee.typeParameterSymbols[index].toInjektClassifier(ctx)
        this[parameter] = argument.toConeTypeProjection().toInjektType(ctx)
      }

      fun InjektType.putAll() {
        for ((index, parameter) in classifier.typeParameters.withIndex()) {
          val argument = arguments[index]
          if (argument.classifier != parameter)
            this@buildMap[parameter] = arguments[index]
        }
      }

      expression.dispatchReceiver?.resolvedType?.toInjektType(ctx)?.putAll()
      expression.extensionReceiver?.resolvedType?.toInjektType(ctx)?.putAll()
    }

    val substitutedCallee = callee
      .toInjektCallable(ctx)
      .substitute(substitutionMap)

    callee.typeParameterSymbols.forEach { it.classifierInfo(ctx).superTypes }

    /*val requests = expression.arguments
      .transform {
        val index = it.injektIndex()
        if (valueArgumentsByIndex[index] is DefaultValueArgument && index in info.injectParameters)
          add(it.toInjectableRequest(callee))
      }

    if (requests.isEmpty()) return*/

    /*val scope = ElementInjectablesScope(ctx, callExpression)

    // look up declarations to support incremental compilation
    context.session.lookupTracker?.recordLookup(
      InjektFqNames.InjectablesLookup.callableName,
      InjektFqNames.InjectablesLookup.packageName.asString(),
      expression.source,
      file.source
    )

    val scope = ElementInjectablesScope(expression, context.containingElements, context.session)

    when (val result = scope.resolveRequests(callee.toInjektCallable(context.session), requests)) {
      is InjectionResult.Success -> {
        cache.cached(INJECTIONS_OCCURRED_IN_FILE_KEY, file.sourceFile!!.path) { Unit }
        cache.cached(
          INJECTION_RESULT_KEY,
          SourcePosition(
            file.sourceFile!!.path!!,
            expression.source!!.startOffset,
            expression.source!!.endOffset
          )
        ) { result }
      }
      is InjectionResult.Error -> reporter.report(expression.source!!, result.toErrorString(), context)
    }*/
  }
}
