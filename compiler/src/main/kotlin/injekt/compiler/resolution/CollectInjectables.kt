/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, DirectDeclarationsAccess::class)

package injekt.compiler.resolution

import injekt.compiler.*
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

context(ctx: InjektContext)
fun InjektType.collectModuleInjectables(): List<InjektCallable> =
  cached("module_injectables", this) {
  buildList {
    fun InjektType.visit() {
      val classSymbol = classifier.symbol.safeAs<FirClassSymbol<*>>() ?: return

      superTypes.forEach { it.visit() }

      for (declaration in classSymbol.declarationSymbols)
        if (declaration !is FirConstructorSymbol &&
          declaration is FirCallableSymbol<*> &&
          (!declaration.isOverride ||
              declaration.safeAs<FirNamedFunctionSymbol>()
                ?.takeIf { it.getContainingClassSymbol() != null }
                ?.overriddenFunctions(
                  declaration.getContainingClassSymbol()!!.cast(),
                  session,
                  ctx.scopeSession
                )
                ?.firstOrNull()
                ?.isInjectable() == false) &&
          (declaration.isInjectable() ||
              (declaration.name.asString() == "invoke" && isProvideFunctionType()))) {
          val substitutionMap = classifier.typeParameters
            .zip(subtypeView(declaration.dispatchReceiverType?.toInjektType()?.classifier!!)!!.arguments)
            .toMap()
          this@buildList += declaration.toInjektCallable()
            .substitute(substitutionMap)
            .let { callable ->
              if (callable.parameterTypes[DISPATCH_RECEIVER_NAME] == this) callable
              else callable.copy(
                parameterTypes = callable.parameterTypes.toMutableMap()
                  .also { it[DISPATCH_RECEIVER_NAME] = this }
              )
            }
        }
    }

    visit()
  }
}

context(scope: InjectablesScope, ctx: InjektContext)
fun InjektCallable.collectModuleInjectables(
  addInjectable: (InjektCallable) -> Unit,
  addAddOnInjectable: (InjektCallable) -> Unit
) {
  if (typeArguments.any { it.key.isAddOn && it.value == it.key.defaultType }) {
    addAddOnInjectable(this)
    return
  }

  if (type.isUnconstrained(scope.allStaticTypeParameters)) return

  val nextCallable = copy(type = type.copy(uniqueId = UUID.randomUUID().toString()))
  addInjectable(nextCallable)

  nextCallable
    .type
    .collectModuleInjectables()
    .forEach { innerCallable ->
      innerCallable
        .copy(
          chainFqName = nextCallable.chainFqName.child(innerCallable.chainFqName.shortName()),
          type = if (nextCallable.type.isNullableType) innerCallable.type.withNullability(true)
          else innerCallable.type,
          originalType = if (nextCallable.type.isNullableType) innerCallable.type.withNullability(true)
          else innerCallable.type,
          parameterTypes = if (nextCallable.type.isNullableType &&
            DISPATCH_RECEIVER_NAME in innerCallable.parameterTypes) innerCallable.parameterTypes
            .toMutableMap().apply {
              put(
                DISPATCH_RECEIVER_NAME,
                innerCallable.parameterTypes[DISPATCH_RECEIVER_NAME]!!.withNullability(true)
              )
            } else innerCallable.parameterTypes
        )
        .collectModuleInjectables(
          addInjectable = addInjectable,
          addAddOnInjectable = addAddOnInjectable
        )
    }
}

context(ctx: InjektContext)
fun FirRegularClassSymbol.collectInjectableConstructors() = declarationSymbols
  .filterIsInstance<FirConstructorSymbol>()
  .filter { it.isInjectable() }

context(ctx: InjektContext)
fun collectGlobalInjectables(): List<InjektCallable> = collectPackagesWithInjectables()
  .flatMap { collectPackageInjectables(it) }

context(ctx: InjektContext)
fun collectPackageInjectables(packageFqName: FqName): List<InjektCallable> =
  cached("injectables_in_package", packageFqName) {
    if (packageFqName !in collectPackagesWithInjectables()) emptyList()
    else buildList {
      fun collectClassInjectables(classSymbol: FirClassSymbol<*>) {
        for (declarationSymbol in classSymbol.declarationSymbols) {
          if (declarationSymbol is FirConstructorSymbol &&
            ((declarationSymbol.isPrimary &&
                classSymbol.isInjectable()) || declarationSymbol.isInjectable()))
            this += declarationSymbol.toInjektCallable()

          if (declarationSymbol is FirClassSymbol<*>)
            collectClassInjectables(declarationSymbol)
        }
      }

      for (declaration in collectDeclarationsInFqName(packageFqName))
        if (declaration is FirRegularClassSymbol) collectClassInjectables(declaration)
        else if (declaration is FirCallableSymbol<*> && declaration.isInjectable())
          this += declaration.toInjektCallable()
    }
  }

context(ctx: InjektContext)
fun collectPackagesWithInjectables(): Set<FqName> =
  cached("packages_with_injectables", Unit) {
    session.symbolProvider.getTopLevelFunctionSymbols(
      InjektFqNames.InjectablesLookup.packageName,
      InjektFqNames.InjectablesLookup.callableName
    ).mapTo(mutableSetOf()) {
      FqName(
        it.valueParameterSymbols.first().resolvedReturnType.classId!!.shortClassName.asString()
          .split("___")
          .first()
          .replace("__", ".")
      )
    }
  }

context(ctx: InjektContext)
fun injectableReceiverOf(
  name: Name,
  type: ConeKotlinType,
  containingDeclarationSymbol: FirFunctionSymbol<*>,
  startOffset: Int,
  endOffset: Int
): FirValueParameterSymbol = buildValueParameter {
  resolvePhase = FirResolvePhase.BODY_RESOLVE
  moduleData = session.moduleData
  origin = FirDeclarationOrigin.Source
  isCrossinline = false
  isNoinline = false
  isVararg = false
  source = containingDeclarationSymbol.source!!.fakeElement(
    KtFakeSourceElementKind.ReceiverFromType,
    startOffset,
    endOffset
  )

  this.name = name
  symbol = FirValueParameterSymbol()
  returnTypeRef = type.toFirResolvedTypeRef()
  this.containingDeclarationSymbol = containingDeclarationSymbol
}.symbol
