package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.scopes.*
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
        else listOf(it)
      }
      .filterIsInstance<FirCallableSymbol<*>>()

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(PROVIDE_PREDICATE)
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> = injectables()
    .mapIndexed { providerIndex, provider ->
      buildSimpleFunction {
        moduleData = session.moduleData
        origin = FirDeclarationOrigin.Plugin(Key)

        symbol = FirNamedFunctionSymbol(InjektFqNames.InjectablesLookup)
        name = InjektFqNames.InjectablesLookup.callableName
        status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public)
        returnTypeRef = session.builtinTypes.unitType

        fun addValueParameter(name: Name, typeRef: FirTypeRef) {
          valueParameters += buildValueParameter {
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(Key)

            containingFunctionSymbol = this@buildSimpleFunction.symbol

            this.name = name
            symbol = FirValueParameterSymbol(name)
            returnTypeRef = typeRef

            isCrossinline = false
            isNoinline = false
            isVararg = false
          }
        }

        addValueParameter(
          "marker".asNameId(),
          session.symbolProvider.getRegularClassSymbolByClassId(provider.markerClassId())!!
            .defaultType()
            .toFirResolvedTypeRef()
        )

        repeat(providerIndex + 1) {
          addValueParameter("index$providerIndex".asNameId(), session.builtinTypes.byteType)
        }

        val key = provider.uniqueKey()
        val finalKey = String(Base64.getEncoder().encode(key.toByteArray()))
          .filter { it.isLetterOrDigit() }

        finalKey
          .chunked(256)
          .forEachIndexed { index, value ->
            addValueParameter("hash_${index}_$value".asNameId(), session.builtinTypes.intType)
          }
      }.symbol
    }

  override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*> =
    buildRegularClass {
      moduleData = session.moduleData
      origin = FirDeclarationOrigin.Plugin(Key)
      scopeProvider = session.kotlinScopeProvider

      symbol = FirRegularClassSymbol(classId)
      name = classId.shortClassName
      classKind = ClassKind.INTERFACE
      status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.SEALED, EffectiveVisibility.Public)
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
          append(it.annotationTypeRef.coneType.renderReadableWithFqNames())
        }

        resolvedSuperTypes.forEach { append(it.renderReadableWithFqNames()) }
      }
    }

    append(visibility.name)

    append(name)

    receiverParameter?.let {
      append(it.typeRef.coneType.renderReadableWithFqNames())
    }

    if (this@uniqueKey is FirFunctionSymbol<*>) {
      valueParameterSymbols.forEach {
        append(it.name)
        append(it.resolvedReturnType.renderReadableWithFqNames())
        append(it.hasDefaultValue)
      }
    }

    append(resolvedReturnType.renderReadableWithFqNames())
  }

  override fun hasPackage(packageFqName: FqName) = true

  object Key : GeneratedDeclarationKey()

  companion object {
    private val PROVIDE_PREDICATE =
      LookupPredicate.AnnotatedWith(setOf(InjektFqNames.Provide.asSingleFqName()))
  }
}
