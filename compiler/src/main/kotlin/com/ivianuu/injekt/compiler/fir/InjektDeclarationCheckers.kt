@file:OptIn(SymbolInternals::class, UnsafeCastFunction::class,
  InternalDiagnosticFactoryMethod::class
)

package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.utils.addToStdlib.*

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
      reporter.report(
        declaration.source!!,
        "enum class cannot be injectable",
        context
      )

    if (isInjectable && declaration.status.modality == Modality.ABSTRACT)
      reporter.report(
        declaration.source!!,
        "injectable cannot be abstract",
        context
      )

    if (isInjectable)
      checkAddOnTypeParameters(declaration.typeParameters.map { it.symbol.fir }, context, reporter)
    
    if (declaration.hasAnnotation(InjektFqNames.Tag, context.session) &&
      declaration.primaryConstructorIfAny(context.session)?.valueParameterSymbols?.isNotEmpty() == true)
      reporter.report(
        declaration.source!!,
        "tag cannot have value parameters",
        context
      )
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
      reporter.report(
        it.source!!,
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

  declaration.getDirectOverriddenSymbols(context)
    .firstOrNull()
    ?.takeUnless { isValidOverride(it.fir.cast()) }
    ?.let {
      reporter.report(
        FirErrors.NOTHING_TO_OVERRIDE
          .on(declaration.source!!, declaration.symbol, null),
        context
      )
    }
}
