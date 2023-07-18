package com.ivianuu.injekt.compiler.frontend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class InjektFirCheckersExtensions(session: FirSession) : FirAdditionalCheckersExtension(session) {
  override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
    override val functionCallCheckers: Set<FirFunctionCallChecker> =
      setOf(InjektFunctionCallChecker(session))
  }
}
