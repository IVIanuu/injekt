/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class)

package injekt.compiler.resolution

import injekt.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun elementInjectablesScopeOf(
  containingElements: List<FirElement>,
  position: FirElement,
  ctx: InjektContext
): InjectablesScope {
  val scopeOwners = containingElements
    .filter {
      it is FirFile ||
          it is FirClass ||
          it is FirFunction ||
          it is FirProperty ||
          it is FirBlock
    }
    .flatMap {
      if (it !is FirRegularClass) listOf(it)
      else listOfNotNull(it.companionObjectSymbol?.fir, it)
    }
    .reversed()
    .let { reversedScopeOwner ->
      val finalScopeOwners = mutableListOf<FirElement>()
      var includeClasses = true
      for (scopeOwner in reversedScopeOwner)
        when {
          scopeOwner !is FirClass -> finalScopeOwners += scopeOwner
          scopeOwner.classKind.isSingleton -> finalScopeOwners += scopeOwner
          includeClasses -> {
            finalScopeOwners += scopeOwner
            includeClasses = scopeOwner.isInner || scopeOwner.isLocal
          }
        }
      finalScopeOwners
    }
    .reversed()

  fun scopeOf(remainingScopeOwners: List<FirElement>): InjectablesScope =
    when (val element = remainingScopeOwners.last()) {
      is FirFile -> fileInjectablesScopeOf(
        file = element.symbol,
        ctx = ctx
      )

      is FirClass -> classInjectablesScopeOf(
        clazz = element.symbol,
        parent = scopeOf(remainingScopeOwners.dropLast(1)),
        ctx = ctx
      )

      is FirFunction -> functionInjectablesScopeOf(
        function = element.symbol,
        parent = scopeOf(remainingScopeOwners.dropLast(1)),
        containingElements = containingElements,
        ctx = ctx
      )

      is FirProperty -> propertyInjectablesScopeOf(
        property = element.symbol,
        parent = scopeOf(remainingScopeOwners.dropLast(1)),
        ctx = ctx
      )

      is FirBlock -> blockExpressionScopeOf(
        block = element,
        position = position,
        parent = scopeOf(remainingScopeOwners.dropLast(1)),
        ctx = ctx
      )

      else -> scopeOf(remainingScopeOwners.dropLast(1))
    }

  val scope = scopeOf(scopeOwners)
  return scope
}

private fun fileInjectablesScopeOf(file: FirFileSymbol, ctx: InjektContext): InjectablesScope =
  ctx.cached("file_scope", file) {
    injectableScopeOrParentIfEmptyAndSameCallContext(
      name = "FILE ${file.fir.name}",
      parent = internalGlobalInjectablesScopeOf(file, ctx),
      owner = file,
      ctx = ctx,
      initialInjectables = collectPackageInjectables(file.fir.packageFqName, ctx)
        .filter { ctx.session.firProvider.getContainingFile(it.symbol)?.symbol == file }
    )
  }

private fun classCompanionInjectablesScopeOf(
  clazz: FirClassSymbol<*>,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope = clazz.safeAs<FirRegularClassSymbol>()?.companionObjectSymbol
  ?.let { classInjectablesScopeOf(it, parent, ctx) } ?: parent

private fun classInjectablesScopeOf(
  clazz: FirClassSymbol<*>,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope = ctx.cached(
  "class_scope",
  clazz to parent.name
) {
  injectableScopeOrParentIfEmptyAndSameCallContext(
    name = if (clazz.isCompanion) "COMPANION ${clazz.fqName}"
    else "CLASS ${clazz.fqName}",
    parent = classCompanionInjectablesScopeOf(clazz, parent, ctx),
    owner = clazz,
    initialInjectables = listOf(injectableReceiverOf(
      DISPATCH_RECEIVER_NAME,
      clazz.defaultType(),
      clazz.declarationSymbols.filterIsInstance<FirConstructorSymbol>().first(),
      clazz.source!!.startOffset,
      clazz.source!!.endOffset,
      ctx
    ).toInjektCallable(ctx)),
    typeParameters = clazz.typeParameterSymbols.map { it.toInjektClassifier(ctx) },
    ctx = ctx
  )
}

private fun functionInjectablesScopeOf(
  function: FirFunctionSymbol<*>,
  parent: InjectablesScope,
  containingElements: List<FirElement>,
  ctx: InjektContext
): InjectablesScope = ctx.cached(
  "function_scope",
  function to parent.name
) {
  injectableScopeOrParentIfEmptyAndSameCallContext(
    name = "${if (function is FirConstructorSymbol) "CONSTRUCTOR" else "FUNCTION"} ${function.fqName}",
    parent = parent,
    owner = function,
    typeParameters = (if (function is FirConstructorSymbol)
      function.resolvedReturnType.toRegularClassSymbol(ctx.session)!!.typeParameterSymbols
    else function.typeParameterSymbols)
      .map { it.toInjektClassifier(ctx) },
    initialInjectables = buildList {
      if (function.receiverParameter != null)
        this += injectableReceiverOf(
          EXTENSION_RECEIVER_NAME,
          function.receiverParameter!!.typeRef.coneType,
          function,
          function.receiverParameter!!.source!!.startOffset,
          function.receiverParameter!!.source!!.endOffset,
          ctx
        ).toInjektCallable(ctx)

      this += function.resolvedContextParameters
        .map { it.symbol.toInjektCallable(ctx) }

      this += function.valueParameterSymbols
        .filterIndexed { index, valueParameter ->
          valueParameter.isInjectable(ctx)
        }
        .map { it.toInjektCallable(ctx) }
    },
    callContext = function.callContext(ctx),
    ctx = ctx
  )
}

private fun propertyInjectablesScopeOf(
  property: FirPropertySymbol,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope = ctx.cached(
  "property_scope",
  property to parent.name
) {
  injectableScopeOrParentIfEmptyAndSameCallContext(
    name = "PROPERTY ${property.fqName}",
    parent = parent,
    owner = property,
    typeParameters = property.typeParameterSymbols.map { it.toInjektClassifier(ctx) },
    initialInjectables = buildList {
      if (property.receiverParameter != null)
        this += injectableReceiverOf(
          EXTENSION_RECEIVER_NAME,
          property.receiverParameter!!.typeRef.coneType,
          property.getterSymbol!!,
          property.receiverParameter!!.source!!.startOffset,
          property.receiverParameter!!.source!!.endOffset,
          ctx
        )
          .toInjektCallable(ctx)

      this += property.resolvedContextParameters
        .map { it.symbol.toInjektCallable(ctx) }
    },
    callContext = if (property.isLocal) parent.callContext
    else property.callContext(ctx),
    ctx = ctx
  )
}

private fun blockExpressionScopeOf(
  block: FirBlock,
  position: FirElement,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope {
  val injectablesBeforePosition = block.statements
    .filter { declaration ->
      declaration.source!!.endOffset < position.source!!.startOffset &&
          declaration.safeAs<FirDeclaration>()?.symbol?.isInjectable(ctx) == true
    }
    .flatMap { declaration ->
      when (declaration) {
        is FirRegularClass -> declaration.symbol.collectInjectableConstructors(ctx)
          .map { it.toInjektCallable(ctx) }
        is FirCallableDeclaration -> listOf(declaration.symbol.toInjektCallable(ctx))
        else -> emptyList()
      }
    }

  return injectableScopeOrParentIfEmptyAndSameCallContext(
    name = "BLOCK AT ${position.source!!.startOffset}",
    parent = parent,
    initialInjectables = injectablesBeforePosition,
    nesting = if (injectablesBeforePosition.size > 1) parent.nesting
    else parent.nesting + 1,
    callContext = parent.callContext,
    ctx = ctx
  )
}

private fun internalGlobalInjectablesScopeOf(file: FirFileSymbol, ctx: InjektContext): InjectablesScope =
  ctx.cached("internal_global_scope", file) {
    injectableScopeOrParentIfEmptyAndSameCallContext(
      name = "INTERNAL GLOBAL EXCEPT ${file.fir.name}",
      parent = externalGlobalInjectablesScopeOf(file, ctx),
      initialInjectables = collectGlobalInjectables(ctx)
        .filter {
          it.symbol.moduleData == ctx.session.moduleData &&
              ctx.session.firProvider.getContainingFile(it.symbol)?.symbol != file
        },
      ctx = ctx
    )
  }

private fun externalGlobalInjectablesScopeOf(file: FirFileSymbol, ctx: InjektContext): InjectablesScope =
  ctx.cached("external_global_scope", Unit) {
    InjectablesScope(
      name = "EXTERNAL GLOBAL",
      parent = null,
      owner = file,
      initialInjectables = collectGlobalInjectables(ctx)
        .filter { it.symbol.moduleData != ctx.session.moduleData },
      ctx = ctx
    )
  }

fun injectableScopeOrParentIfEmptyAndSameCallContext(
  name: String,
  parent: InjectablesScope,
  owner: FirBasedSymbol<*>? = null,
  initialInjectables: List<InjektCallable> = emptyList(),
  typeParameters: List<InjektClassifier> = emptyList(),
  nesting: Int = parent.nesting.inc(),
  callContext: CallContext = CallContext.DEFAULT,
  ctx: InjektContext
): InjectablesScope {
  return if (typeParameters.isEmpty() &&
    initialInjectables.isEmpty() && callContext == parent.callContext
  ) parent
  else InjectablesScope(
    name,
    parent,
    owner,
    initialInjectables,
    typeParameters,
    callContext,
    nesting,
    ctx
  )
}
