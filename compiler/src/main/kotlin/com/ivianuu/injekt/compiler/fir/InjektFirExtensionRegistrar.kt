package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.extensions.*
import org.jetbrains.kotlin.fir.extensions.*

class InjektFirExtensionRegistrar(private val ctx: InjektContext) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +FirAdditionalCheckersExtension.Factory { session ->
      ctx.session = session
      object : FirAdditionalCheckersExtension(session) {
        override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
          override val callableDeclarationCheckers = setOf(InjektCallableChecker)
          override val classCheckers = setOf(InjektClassChecker)
        }

        override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
          override val functionCallCheckers = setOf(InjectCallChecker(ctx))
        }
      }
    }
  }
}
