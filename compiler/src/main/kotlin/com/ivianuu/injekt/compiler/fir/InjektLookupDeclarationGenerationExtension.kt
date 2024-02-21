package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.*
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import java.util.*

class InjektLookupDeclarationGenerationExtension(session: FirSession) :
  FirDeclarationGenerationExtension(session) {
  private fun injectables() =
    session.predicateBasedProvider.getSymbolsByPredicate(PROVIDE_PREDICATE)
      .flatMap {
        if (it is FirRegularClassSymbol) it.collectInjectableConstructors(session)
        else listOfNotNull(it)
      }
      .filter { it is FirPropertySymbol || it is FirFunctionSymbol<*> }
      .filterIsInstance<FirCallableSymbol<*>>()

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(PROVIDE_PREDICATE)
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> = injectables()
    .mapIndexed { providerIndex, provider ->
      createTopLevelFunction(Key, InjektFqNames.InjectablesLookup, session.builtinTypes.unitType.type) {
        valueParameter(
          "marker".asNameId(),
          session.symbolProvider.getRegularClassSymbolByClassId(provider.markerClassId())!!
            .defaultType()
            .toFirResolvedTypeRef()
            .type
        )

        repeat(providerIndex + 1) {
          valueParameter("index$providerIndex".asNameId(), session.builtinTypes.byteType.type)
        }

        val key = provider.uniqueKey()
        val finalKey = String(Base64.getEncoder().encode(key.toByteArray()))
          .filter { it.isLetterOrDigit() }

        finalKey
          .chunked(256)
          .forEachIndexed { index, value ->
            valueParameter("hash_${index}_$value".asNameId(), session.builtinTypes.intType.type)
          }
      }.symbol
    }

  override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*> =
    createTopLevelClass(classId, Key, ClassKind.INTERFACE) {
      modality = Modality.SEALED
    }.symbol

  override fun getTopLevelCallableIds(): Set<CallableId> = setOf(InjektFqNames.InjectablesLookup)

  override fun getTopLevelClassIds(): Set<ClassId> = injectables()
    .mapTo(mutableSetOf()) { session.firProvider.getContainingFile(it)!!.markerClassId() }

  private fun FirBasedSymbol<*>.markerClassId(): ClassId =
    session.firProvider.getContainingFile(this)!!.markerClassId()

  private fun FirFile.markerClassId(): ClassId {
    val markerName = "_${
      name.removeSuffix(".kt")
        .substringAfterLast(".")
        .substringAfterLast("/")
    }_ProvidersMarker".asNameId()
    return ClassId.topLevel(packageFqName.child(markerName))
  }

  private fun FirCallableSymbol<*>.uniqueKey() = buildString {
    if (this@uniqueKey is FirConstructorSymbol) {
      this@uniqueKey.getConstructedClass(session)!!.run {
        annotations.forEach {
          append(it.annotationTypeRef.coneType.renderToString())
        }

        resolvedSuperTypes.forEach { append(it.renderToString()) }
      }
    }

    append(visibility.name)

    append(name)

    receiverParameter?.let {
      append(it.typeRef.coneType.renderToString())
    }

    if (this@uniqueKey is FirFunctionSymbol<*>) {
      valueParameterSymbols.forEach {
        append(it.name)
        append(it.resolvedReturnType.renderToString())
        append(it.hasDefaultValue)
      }
    }

    append(resolvedReturnType.renderToString())
  }

  override fun hasPackage(packageFqName: FqName): Boolean = packageFqName == InjektFqNames.InjectablesLookupPackage

  object Key : GeneratedDeclarationKey()

  companion object {
    private val PROVIDE_PREDICATE =
      LookupPredicate.AnnotatedWith(setOf(InjektFqNames.Provide.asSingleFqName()))
  }
}
