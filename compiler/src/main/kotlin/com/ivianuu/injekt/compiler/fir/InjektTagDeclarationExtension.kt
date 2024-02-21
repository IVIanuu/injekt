package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.*
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.*

class InjektTagDeclarationExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
  private fun tags() = session.predicateBasedProvider.getSymbolsByPredicate(TAG_PREDICATE)
    .filterIsInstance<FirRegularClassSymbol>()

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(TAG_PREDICATE)
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext
  ): Set<Name> = if (classSymbol !in tags()) emptySet()
  else setOf(InjektFqNames.TagWrapper)

  override fun generateNestedClassLikeDeclaration(
    owner: FirClassSymbol<*>,
    name: Name,
    context: NestedClassGenerationContext
  ): FirClassLikeSymbol<*>? =
    if (owner !in tags()) null
    else createNestedClass(owner, name, Key, ClassKind.INTERFACE) {
      modality = Modality.SEALED
      owner.typeParameterSymbols.forEach {
        typeParameter(it.name, it.variance)
      }
      typeParameter(InjektFqNames.TagWrapper, Variance.OUT_VARIANCE)
    }.symbol

  object Key : GeneratedDeclarationKey()

  companion object {
    private val TAG_PREDICATE =
      LookupPredicate.AnnotatedWith(setOf(InjektFqNames.Tag.asSingleFqName()))
  }
}
