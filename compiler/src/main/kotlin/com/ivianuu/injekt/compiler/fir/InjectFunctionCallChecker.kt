package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*

fun collectPackagesWithInjectables(session: FirSession): Set<FqName> =
  session.symbolProvider.getTopLevelFunctionSymbols(
    InjektFqNames.InjectablesLookup.parent(),
    InjektFqNames.InjectablesLookup.shortName()
  ).mapTo(mutableSetOf()) {
    it.valueParameterSymbols.first().resolvedReturnType.classId!!.packageFqName
  }

fun collectPackageInjectables(packageFqName: FqName, session: FirSession): List<FirCallableSymbol<*>> =
  if (packageFqName !in collectPackagesWithInjectables(session)) emptyList()
  else buildList {
    fun collectClassInjectables(classSymbol: FirClassSymbol<*>) {
      for (declarationSymbol in classSymbol.declarationSymbols) {
        if (declarationSymbol is FirConstructorSymbol &&
          ((declarationSymbol.isPrimary &&
              classSymbol.hasAnnotation(InjektFqNames.Provide, session)) ||
              declarationSymbol.hasAnnotation(InjektFqNames.Provide, session)))
          add(declarationSymbol)

        if (declarationSymbol is FirClassSymbol<*>)
          collectClassInjectables(declarationSymbol)
      }
    }

    session.symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName)
      ?.mapNotNull {
        session.symbolProvider.getRegularClassSymbolByClassId(ClassId(packageFqName, it.asNameId()))
      }
      ?.forEach { collectClassInjectables(it) }

    session.symbolProvider.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName)
      ?.flatMap { name ->
        session.symbolProvider.getTopLevelCallableSymbols(packageFqName, name)
      }
      ?.filter { it.hasAnnotation(InjektFqNames.Provide, session) }
      ?.forEach { add(it) }
  }

class InjectFunctionCallChecker(val cache: InjektCache) : FirFunctionCallChecker() {
  override fun check(
    expression: FirFunctionCall,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    val calleeFunction = expression.calleeReference.toResolvedCallableSymbol()
      ?: return

    if (calleeFunction.callableId != InjektFqNames.inject)
      return

    val injectedType = expression.typeRef

    context.containingDeclarations
    val (internalInjectables, externalInjectables) = collectPackagesWithInjectables(context.session)
      .flatMap { collectPackageInjectables(it, context.session) }
      .partition { it.moduleData == context.session.moduleData }
    println()

    /*+
    val callee = resultingDescriptor
      .toCallableRef(ctx)
      .substitute(substitutionMap)

    val requests = listOf(
      InjectableRequest(
        callee.type,
        callee.callableFqName,
        callee.typeArguments,
        "x".asNameId(),
        0
      )
    )
    if (requests.isEmpty()) return

    val scope = ElementInjectablesScope(ctx, callExpression)

    val location = KotlinLookupLocation(callExpression)
    memberScopeForFqName(InjektFqNames.InjectablesPackage, location, ctx)
      ?.recordLookup(InjektFqNames.InjectablesLookup.shortName(), location)

    when (val result = scope.resolveRequests(callee, requests)) {
      is InjectionResult.Success -> {
        ctx.cache.cached(INJECTIONS_OCCURRED_IN_FILE_KEY, file.virtualFilePath) { Unit }
        ctx.cache.cached(
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
     */
  }
}
