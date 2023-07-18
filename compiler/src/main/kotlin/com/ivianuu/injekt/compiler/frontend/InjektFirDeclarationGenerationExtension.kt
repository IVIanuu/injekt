package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

@OptIn(SymbolInternals::class)
class InjektFirDeclarationGenerationExtension(session: FirSession) :
  FirDeclarationGenerationExtension(session) {
  private val functionWithInject by lazy {
    session.predicateBasedProvider.getSymbolsByPredicate(INJECT_PREDICATE)
      .filterIsInstance<FirValueParameterSymbol>()
      .map { it.containingFunctionSymbol.fir }
  }

  override fun getTopLevelCallableIds(): Set<CallableId> =
    functionWithInject.mapTo(mutableSetOf()) { it.symbol.callableId }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> {
    return functionWithInject
      .filter { it.symbol.callableId == callableId }
      .map { originalFunction ->
        session.symbolProvider.getTopLevelPropertySymbols(
          InjektFqNames.InjectablesLookup.parent(),
          InjektFqNames.InjectablesLookup.shortName()
        )
        buildSimpleFunction {
          origin = Key.origin
          moduleData = originalFunction.moduleData
          status = originalFunction.status
          symbol = FirNamedFunctionSymbol(originalFunction.symbol.callableId)
          name = originalFunction.symbol.name
          returnTypeRef = originalFunction.returnTypeRef
          typeParameters += originalFunction.typeParameters.map { it.symbol.fir }
          valueParameters += originalFunction.valueParameters
            .filterNot { it.hasAnnotation(ClassId.topLevel(InjektFqNames.Inject), session) }
            .map { originalValueParameter ->
              buildValueParameter {
                containingFunctionSymbol = this@buildSimpleFunction.symbol
                origin = Key.origin
                moduleData = originalFunction.moduleData
                symbol = FirValueParameterSymbol(originalValueParameter.name)
                name = originalValueParameter.name
                returnTypeRef = originalValueParameter.returnTypeRef
                defaultValue = originalValueParameter.defaultValue
                isCrossinline = originalValueParameter.isCrossinline
                isNoinline = originalValueParameter.isNoinline
                isVararg = originalValueParameter.isVararg
              }
            }
        }.symbol
      }
  }

  override fun hasPackage(packageFqName: FqName) = true

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(INJECT_PREDICATE)
  }

  object Key : GeneratedDeclarationKey()

  companion object {
    private val INJECT_PREDICATE = LookupPredicate.AnnotatedWith(setOf(InjektFqNames.Inject))
  }
}

object OriginalFunctionKey : FirDeclarationDataKey()
var FirDeclaration.originalFunction: FirFunction? by FirDeclarationDataRegistry.data(OriginalFunctionKey)
