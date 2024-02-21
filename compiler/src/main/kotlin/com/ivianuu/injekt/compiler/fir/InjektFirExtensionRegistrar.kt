package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.extensions.*
import org.jetbrains.kotlin.fir.extensions.*

class InjektFirExtensionRegistrar(private val cache: InjektCache) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +FirAdditionalCheckersExtension.Factory { InjektFirCheckersExtension(it, cache) }
    +FirDeclarationGenerationExtension.Factory { InjektLookupDeclarationGenerationExtension(it) }
    +FirDeclarationGenerationExtension.Factory { InjektTagDeclarationExtension(it) }
  }
}

class InjektFirCheckersExtension(
  session: FirSession,
  val cache: InjektCache,
) : FirAdditionalCheckersExtension(session) {
  init { cache.session = session }

  override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
    override val functionCheckers = setOf(InjektFunctionChecker)
    override val constructorCheckers = setOf(InjektConstructorChecker)
    override val propertyCheckers = setOf(InjektPropertyChecker)
    override val classCheckers = setOf(InjektClassChecker)
  }

  override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
    override val functionCallCheckers = setOf(InjectFunctionCallChecker(cache))
  }
}
