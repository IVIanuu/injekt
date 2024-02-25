@file:OptIn(SymbolInternals::class, UnsafeCastFunction::class,
  InternalDiagnosticFactoryMethod::class
)

package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.utils.addToStdlib.*

object InjektFunctionChecker : FirFunctionChecker(MppCheckerKind.Common) {
  override fun check(
    declaration: FirFunction,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    if (declaration.hasAnnotation(InjektFqNames.Provide, context.session))
      checkSpreadTypeParameters(
        declaration.typeParameters.map { it.symbol.fir },
        context,
        reporter
      )
    checkOverrides(declaration, context, reporter)
    checkExceptActual(declaration, context, reporter)
  }
}

object InjektConstructorChecker : FirConstructorChecker(MppCheckerKind.Common) {
  override fun check(
    declaration: FirConstructor,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    checkExceptActual(declaration, context, reporter)
  }
}

object InjektClassChecker : FirClassChecker(MppCheckerKind.Common) {
  override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
    val provideConstructors = declaration.declarations
      .filterIsInstance<FirConstructorSymbol>()
      .filter { it.hasAnnotation(InjektFqNames.Provide, context.session) }

    val isProvider = provideConstructors.isNotEmpty() ||
        declaration.hasAnnotation(InjektFqNames.Provide, context.session)

    if (isProvider && declaration.classKind == ClassKind.ANNOTATION_CLASS)
      reporter.report(
        declaration.getAnnotationByClassId(InjektFqNames.Provide, context.session)!!.source!!,
        "annotation class cannot be injectable",
        context
      )

    if (isProvider && declaration.classKind == ClassKind.ENUM_CLASS)
      reporter.report(
        declaration.getAnnotationByClassId(InjektFqNames.Provide, context.session)!!.source!!,
        "enum class cannot be injectable",
        context
      )

    if (isProvider && declaration.status.isInner)
      reporter.report(
        declaration.getAnnotationByClassId(InjektFqNames.Provide, context.session)!!.source!!,
        "inner class cannot be injectable",
        context
      )

    if (declaration.classKind == ClassKind.INTERFACE &&
      declaration.hasAnnotation(InjektFqNames.Provide, context.session))
      reporter.report(
        declaration.getAnnotationByClassId(InjektFqNames.Provide, context.session)!!.source!!,
        "interface cannot be injectable",
        context
      )

    if (isProvider && declaration.classKind == ClassKind.CLASS &&
      declaration.status.modality == Modality.ABSTRACT)
      reporter.report(
        declaration.getModifier(KtTokens.ABSTRACT_KEYWORD)!!.source,
        "abstract class cannot be injectable",
        context
      )

    if (declaration.hasAnnotation(InjektFqNames.Provide, context.session) &&
      declaration.primaryConstructorIfAny(context.session)
        ?.hasAnnotation(InjektFqNames.Provide, context.session) == true
    )
      reporter.report(
        declaration.getAnnotationByClassId(InjektFqNames.Provide, context.session)!!.source!!,
        "class cannot be marked with @Provide if it has a @Provide primary constructor",
        context
      )

    if (isProvider)
      checkSpreadTypeParameters(declaration.typeParameters.map { it.symbol.fir }, context, reporter)

    checkExceptActual(declaration, context, reporter)

    if (declaration.hasAnnotation(InjektFqNames.Tag, context.session) &&
      declaration.primaryConstructorIfAny(context.session)?.valueParameterSymbols?.isNotEmpty() == true)
      reporter.report(
        declaration.getAnnotationByClassId(InjektFqNames.Tag, context.session)!!.source!!,
        "tag cannot have value parameters",
        context
      )
  }
}

object InjektPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
  override fun check(
    declaration: FirProperty,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    checkOverrides(declaration, context, reporter)
    checkExceptActual(declaration, context, reporter)

    if (declaration.hasAnnotation(InjektFqNames.Provide, context.session) &&
      declaration.getter == null &&
      declaration.delegate == null &&
      !declaration.status.isLateInit &&
      declaration.initializer == null)
      reporter.report(
        declaration.source!!,
        "injectable variable must be initialized, delegated or marked with lateinit",
        context
      )
  }
}

private fun checkSpreadTypeParameters(
  typeParameters: List<FirTypeParameter>,
  context: CheckerContext,
  reporter: DiagnosticReporter
) {
  val addOnParameters = typeParameters.filter {
    it.hasAnnotation(InjektFqNames.AddOn, context.session)
  }
  if (addOnParameters.size > 1)
    addOnParameters
      .drop(1)
      .forEach {
        reporter.report(
          it.getAnnotationByClassId(InjektFqNames.AddOn, context.session)!!.source!!,
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
  val parentClass = declaration.getContainingClass(context.session) ?: return

  parentClass.unsubstitutedScope(context)
    .getDirectOverriddenMembers(declaration.symbol)
    .firstOrNull()
    ?.takeUnless { isValidOverride(declaration, it.fir.cast(), context.session) }
    ?.let {
      reporter.report(
        FirErrors.NOTHING_TO_OVERRIDE
          .on(declaration.source!!, declaration.symbol, null),
        context
      )
    }
}

private fun checkExceptActual(
  declaration: FirMemberDeclaration,
  context: CheckerContext,
  reporter: DiagnosticReporter
) {
  if (!declaration.status.isActual) return

  if (declaration is FirCallableDeclaration)
    declaration.symbol.expectForActual
      ?.values
      ?.first()
      ?.first()
      ?.takeUnless { isValidOverride(declaration, it.fir.cast(), context.session) }
      ?.let {
        reporter.report(
          FirErrors.ACTUAL_WITHOUT_EXPECT
            .on(declaration.source!!, declaration.symbol, emptyMap(), null),
          context
        )
      }
}

private fun isValidOverride(
  declaration: FirDeclaration,
  overriddenDeclaration: FirDeclaration,
  session: FirSession
): Boolean {
  if (overriddenDeclaration.hasAnnotation(InjektFqNames.Provide, session) &&
    !declaration.hasAnnotation(InjektFqNames.Provide, session))
    return false

  val (typeParameters, overriddenTypeParameters) = when (declaration) {
    is FirCallableDeclaration -> declaration.typeParameters.map { it.symbol } to
        overriddenDeclaration.cast<FirCallableDeclaration>().typeParameters.map { it.symbol }
    is FirClassLikeDeclaration -> declaration.typeParameters.map { it.symbol } to
        overriddenDeclaration.cast<FirClassLikeDeclaration>().typeParameters.map { it.symbol }
    else -> emptyList<FirTypeParameterSymbol>() to emptyList()
  }

  for ((index, overriddenTypeParameter) in overriddenTypeParameters.withIndex()) {
    val typeParameter = typeParameters[index]
    if (typeParameter.hasAnnotation(InjektFqNames.AddOn, session) !=
      overriddenTypeParameter.hasAnnotation(InjektFqNames.AddOn, session))
      return false
  }

  return true
}
