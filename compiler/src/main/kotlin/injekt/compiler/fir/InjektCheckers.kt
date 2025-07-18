/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(SymbolInternals::class, UnsafeCastFunction::class,
  InternalDiagnosticFactoryMethod::class, DeprecatedForRemovalCompilerApi::class,
  DirectDeclarationsAccess::class
)

package injekt.compiler.fir

import injekt.compiler.*
import injekt.compiler.resolution.*
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjectCallChecker(private val ctx: InjektContext) : FirFunctionCallChecker(MppCheckerKind.Common) {
  override fun check(
    expression: FirFunctionCall,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    val file = context.containingFile ?: return

    val callee = expression.calleeReference.toResolvedCallableSymbol()
      .safeAs<FirFunctionSymbol<*>>() ?: return

    val metadata = callee.callableMetadata(ctx)

    if (metadata.injectParameters.isEmpty()) return

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
        this += DISPATCH_RECEIVER_NAME

      if (expression.extensionReceiver != null)
        this += EXTENSION_RECEIVER_NAME

      expression.resolvedArgumentMapping?.forEach {
        this += it.value.symbol.name
      }
    }

    val requests = substitutedCallee.injectableRequests(
      explicitArguments + callee.valueParameterSymbols
        .filter { it.name !in metadata.injectParameters }
        .map { it.name },
      ctx
    )

    if (requests.isEmpty()) return

    val scope = elementInjectablesScopeOf(context.containingElements, expression, ctx)

    // look up declarations to support incremental compilation
    context.session.lookupTracker?.recordLookup(
      InjektFqNames.InjectablesLookup.callableName.asString(),
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
            expression.source!!.endOffset
          )
        ) { result }
      }
      is InjectionResult.Error ->
        reporter.reportOn(expression.source!!, INJEKT_ERROR, result.render(ctx), context)
    }
  }
}

object InjektCallableChecker : FirCallableDeclarationChecker(MppCheckerKind.Common) {
  override fun check(
    declaration: FirCallableDeclaration,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    checkAddOnTypeParameters(
      declaration.typeParameters.map { it.symbol.fir },
      context,
      reporter
    )
    checkOverrides(declaration, context, reporter)
  }
}

object InjektClassChecker : FirClassChecker(MppCheckerKind.Common) {
  override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
    val injectableConstructors = declaration.declarations
      .filterIsInstance<FirConstructor>()
      .filter { it.hasAnnotation(InjektFqNames.Provide, context.session) }

    val isInjectable = injectableConstructors.isNotEmpty() ||
        declaration.hasAnnotation(InjektFqNames.Provide, context.session)

    if (isInjectable && declaration.classKind == ClassKind.ENUM_CLASS)
      reporter.reportOn(declaration.source!!, INJEKT_ERROR, "enum class cannot be injectable", context)

    if (isInjectable && declaration.status.modality == Modality.ABSTRACT)
      reporter.reportOn(declaration.source!!, INJEKT_ERROR, "injectable cannot be abstract", context)

    if (isInjectable)
      checkAddOnTypeParameters(declaration.typeParameters.map { it.symbol.fir }, context, reporter)
    
    if (declaration.hasAnnotation(InjektFqNames.Tag, context.session)) {
      declaration.constructors(context.session).forEach {
        if (it.valueParameterSymbols.isNotEmpty())
          reporter.reportOn(it.source!!, INJEKT_ERROR, "tag cannot have value parameters", context)
      }
    }
  }
}

private fun checkAddOnTypeParameters(
  typeParameters: List<FirTypeParameter>,
  context: CheckerContext,
  reporter: DiagnosticReporter
) {
  typeParameters
    .filter { it.hasAnnotation(InjektFqNames.AddOn, context.session) }
    .takeIf { it.size > 1 }
    ?.drop(1)
    ?.forEach {
      reporter.reportOn(
        it.source!!,
        INJEKT_ERROR,
        "a declaration may have only one @AddOn type parameter",
        context
      )
    }
}

private fun checkOverrides(
  declaration: FirCallableDeclaration,
  context: CheckerContext,
  reporter: DiagnosticReporter
) {
  fun isValidOverride(overriddenDeclaration: FirCallableDeclaration): Boolean {
    if (overriddenDeclaration.hasAnnotation(InjektFqNames.Provide, context.session) &&
      !declaration.hasAnnotation(InjektFqNames.Provide, context.session))
      return false

    declaration.symbol.typeParameterSymbols
      .zip(overriddenDeclaration.symbol.typeParameterSymbols)
      .forEach { (typeParameter, overriddenTypeParameter) ->
        if (typeParameter.hasAnnotation(InjektFqNames.AddOn, context.session) !=
          overriddenTypeParameter.hasAnnotation(InjektFqNames.AddOn, context.session))
          return false
      }

    return true
  }

  (declaration.symbol.directOverriddenSymbolsSafe(context))
    .firstOrNull()
    ?.takeUnless { isValidOverride(it.fir.cast()) }
    ?.let {
      reporter.report(
        FirErrors.NOTHING_TO_OVERRIDE
          .on(
            declaration.source!!,
            declaration.symbol,
            emptyList(),
            null,
            context.languageVersionSettings
          ),
        context
      )
    }
}
