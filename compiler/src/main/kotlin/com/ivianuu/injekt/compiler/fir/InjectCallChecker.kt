@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjectCallChecker(private val ctx: InjektContext) : FirFunctionCallChecker(MppCheckerKind.Platform) {
  override fun check(
    expression: FirFunctionCall,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    val file = context.containingFile ?: return

    val callee = expression.calleeReference.toResolvedCallableSymbol()
      .safeAs<FirFunctionSymbol<*>>() ?: return

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

    val explicitArguments = expression.resolvedArgumentMapping
      ?.mapTo(mutableSetOf()) { callee.valueParameterSymbols.indexOf(it.value.symbol) }
      ?: emptySet()

    val requests = substitutedCallee.injectableRequests(explicitArguments)

    if (requests.isEmpty()) return

    val scope = elementInjectablesScopeOf(context.containingElements, expression, ctx)

    // look up declarations to support incremental compilation
    context.session.lookupTracker?.recordLookup(
      InjektFqNames.InjectablesLookup.callableName,
      InjektFqNames.InjectablesLookup.packageName.asString(),
      expression.source,
      file.source
    )

    when (val result = scope.resolveRequests(callee.toInjektCallable(ctx), requests)) {
      is InjectionResult.Success -> {
        ctx.cached(INJECTIONS_OCCURRED_IN_FILE_KEY, file.sourceFile!!.path) { Unit }
        ctx.cached(
          INJECTION_RESULT_KEY,
          SourcePosition(
            file.sourceFile!!.path!!,
            expression.source!!.startOffset,
            expression.source!!.endOffset
          )
        ) { result }
      }
      is InjectionResult.Error -> reporter.report(expression.source!!, result.render(), context)
    }
  }
}
