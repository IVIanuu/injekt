/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjectCallChecker(private val ctx: InjektContext) : FirFunctionCallChecker(MppCheckerKind.Common) {
  override fun check(
    expression: FirFunctionCall,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    runInjekt(ctx)

    val result = checkCall(expression, ctx)

    if (result is InjectionResult.Error)
      reporter.reportOn(expression.source!!, INJEKT_ERROR, result.render(), context)
  }
}

private fun runInjekt(ctx: InjektContext) {
  if (ctx.firDone) return
  ctx.firDone = true
  val files = mutableListOf<FirFile>()
  ctx.session.firProvider.symbolProvider.symbolNamesProvider
    .getPackageNames()
    ?.forEach {
      files += ctx.session.firProvider.getFirFilesByPackage(FqName(it))
    }
    ?: error("no package names found")
  ctx.files = files
}

fun containingElements(target: FirElement, ctx: InjektContext): List<FirElement> {
  var containingElements: List<FirElement>? = null

  val visitor = object : FirDefaultVisitorVoid() {
    private val currentElements = mutableListOf<FirElement>()

    override fun visitElement(element: FirElement) {
      if (element == target) {
        containingElements = currentElements.toList()
      } else {
        currentElements += element
        element.acceptChildren(this)
        currentElements.popLast()
      }
    }
  }

  for (file in ctx.files) {
    file.accept(visitor)
    if (containingElements != null) break
  }

  return containingElements ?: error("Could not find ${target.render()} in ${ctx.files}")
}

fun checkCall(
  expression: FirFunctionCall,
  ctx: InjektContext
): InjectionResult? {
  val containingElements = containingElements(expression, ctx)
  val file = containingElements.firstOrNull().safeAs<FirFile>()
    ?: return null

  return ctx.cached(
    INJECTION_RESULT_KEY,
    SourcePosition(
      file.sourceFile!!.path!!,
      expression.source!!.endOffset
    )
  ) {
    val callee = expression.calleeReference.toResolvedCallableSymbol()
      .safeAs<FirFunctionSymbol<*>>() ?: return@cached null

    val info = callee.callableInfo(ctx)

    println("check call ${callee.fqName} ${info.injectParameters} ${info.contextualParameters.map { 
      it.renderToString()
    }}")

    if (info.injectParameters.isEmpty() &&
      info.contextualParameters.isEmpty()) return@cached null

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

    val explicitArguments = buildSet {
      if (expression.dispatchReceiver != null)
        this += DISPATCH_RECEIVER_INDEX

      if (expression.extensionReceiver != null)
        this += EXTENSION_RECEIVER_INDEX

      expression.resolvedArgumentMapping?.forEach {
        this += callee.valueParameterSymbols.indexOf(it.value.symbol)
      }
    }

    val requests = substitutedCallee.injectableRequests(
      explicitArguments + callee.valueParameterSymbols.indices
        .filter { it !in info.injectParameters }
    )

    println("requests for call ${callee.fqName} ${requests.map { it.type.renderToString() }}")

    if (requests.isEmpty()) return null

    val scope = elementInjectablesScopeOf(
      containingElements,
      expression,
      ctx
    )

    // look up declarations to support incremental compilation
    ctx.session.lookupTracker?.recordLookup(
      InjektFqNames.InjectablesLookup.callableName.asString(),
      InjektFqNames.InjectablesLookup.packageName.asString(),
      expression.source,
      file.source
    )

    val result = scope.resolveRequests(callee.toInjektCallable(ctx), requests)
    if (result is InjectionResult.Success) {
      ctx.cached(INJECTIONS_OCCURRED_IN_FILE_KEY, file.sourceFile!!.path) { Unit }
      ctx.cached(
        INJECTION_RESULT_KEY,
        SourcePosition(
          file.sourceFile!!.path!!,
          expression.source!!.endOffset
        )
      ) { result }
    }
    result
  }
}
