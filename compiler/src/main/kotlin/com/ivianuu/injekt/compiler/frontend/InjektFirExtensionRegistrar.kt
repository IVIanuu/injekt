package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.extensions.*
import org.jetbrains.kotlin.fir.extensions.*

class InjektFirExtensionRegistrar(private val ctx: InjektContext) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +FirAdditionalCheckersExtension.Factory { InjektFirCheckersExtension(it, ctx) }
  }
}

class InjektFirCheckersExtension(
  session: FirSession,
  val ctx: InjektContext,
) : FirAdditionalCheckersExtension(session) {
  init { ctx.session = session }

  override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
    override val functionCheckers = setOf(InjektFunctionChecker)
    override val constructorCheckers = setOf(InjektConstructorChecker)
    override val propertyCheckers = setOf(InjektPropertyChecker)
    override val classCheckers = setOf(InjektClassChecker)
  }

  override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
    override val functionCallCheckers = setOf(InjectCallChecker(ctx))
  }
}
