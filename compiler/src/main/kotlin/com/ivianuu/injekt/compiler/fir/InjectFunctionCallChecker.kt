package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.types.*

class InjectFunctionCallChecker(private val cache: InjektCache) : FirFunctionCallChecker() {
  override fun check(
    expression: FirFunctionCall,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    val callee = expression.calleeReference.toResolvedCallableSymbol()
      ?: return

    val file = context.containingFile ?: return

    if (callee.callableId != InjektFqNames.inject)
      return

    // look up declarations to support incremental compilation
    context.session.lookupTracker?.recordLookup(
      InjektFqNames.InjectablesLookup.callableName,
      InjektFqNames.InjectablesLookup.packageName.asString(),
      expression.source,
      file.source
    )

    val injectedType = expression.typeRef.coneType

    val requests = listOf(
      InjectableRequest(
        injectedType,
        callee.callableId.asSingleFqName(),
        "x".asNameId(),
        0
      )
    )

    val scope = ElementInjectablesScope(expression, context.containingElements, context.session)

    when (val result = scope.resolveRequests(callee.toInjektCallable(), requests)) {
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
    }
  }
}
