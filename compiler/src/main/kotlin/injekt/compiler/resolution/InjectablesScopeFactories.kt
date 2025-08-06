/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class, DirectDeclarationsAccess::class)

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
import org.jetbrains.kotlin.utils.addToStdlib.*

context(ctx: InjektContext)
fun elementInjectablesScopeOf(
  containingElements: List<FirElement>,
  position: FirElement
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
      is FirFile -> fileInjectablesScopeOf(file = element.symbol)

      is FirClass -> classInjectablesScopeOf(
        clazz = element.symbol,
        parent = scopeOf(remainingScopeOwners.dropLast(1))
      )

      is FirFunction -> functionInjectablesScopeOf(
        function = element.symbol,
        parent = scopeOf(remainingScopeOwners.dropLast(1))
      )

      is FirProperty -> propertyInjectablesScopeOf(
        property = element.symbol,
        parent = scopeOf(remainingScopeOwners.dropLast(1))
      )

      is FirBlock -> blockExpressionScopeOf(
        block = element,
        position = position,
        parent = scopeOf(remainingScopeOwners.dropLast(1))
      )

      else -> scopeOf(remainingScopeOwners.dropLast(1))
    }

  val scope = scopeOf(scopeOwners)
  return scope
}

context(ctx: InjektContext)
private fun fileInjectablesScopeOf(file: FirFileSymbol): InjectablesScope =
  cached("file_scope", file) {
    injectableScopeOrParentIfEmptyAndSameCallContext(
      name = "FILE ${file.fir.name}",
      parent = internalGlobalInjectablesScopeOf(file),
      owner = file,
      initialInjectables = collectPackageInjectables(file.fir.packageFqName)
        .filter { session.firProvider.getContainingFile(it.symbol)?.symbol == file }
    )
  }

context(ctx: InjektContext)
private fun classCompanionInjectablesScopeOf(
  clazz: FirClassSymbol<*>,
  parent: InjectablesScope
): InjectablesScope = clazz.safeAs<FirRegularClassSymbol>()?.companionObjectSymbol
  ?.let { classInjectablesScopeOf(it, parent) } ?: parent

context(ctx: InjektContext)
private fun classInjectablesScopeOf(
  clazz: FirClassSymbol<*>,
  parent: InjectablesScope
): InjectablesScope = cached(
  "class_scope",
  clazz to parent.name
) {
  injectableScopeOrParentIfEmptyAndSameCallContext(
    name = if (clazz.isCompanion) "COMPANION ${clazz.fqName}"
    else "CLASS ${clazz.fqName}",
    parent = classCompanionInjectablesScopeOf(clazz, parent),
    owner = clazz,
    initialInjectables = listOf(injectableReceiverOf(
      DISPATCH_RECEIVER_NAME,
      clazz.defaultType(),
      clazz.declarationSymbols.filterIsInstance<FirConstructorSymbol>().first(),
      clazz.source!!.startOffset,
      clazz.source!!.endOffset
    ).toInjektCallable()),
    typeParameters = clazz.typeParameterSymbols.map { it.toInjektClassifier() }
  )
}

context(ctx: InjektContext)
private fun functionInjectablesScopeOf(
  function: FirFunctionSymbol<*>,
  parent: InjectablesScope
): InjectablesScope = cached(
  "function_scope",
  function to parent.name
) {
  injectableScopeOrParentIfEmptyAndSameCallContext(
    name = "${if (function is FirConstructorSymbol) "CONSTRUCTOR" else "FUNCTION"} ${function.fqName}",
    parent = parent,
    owner = function,
    typeParameters = (if (function is FirConstructorSymbol)
      function.resolvedReturnType.toRegularClassSymbol(session)!!.typeParameterSymbols
    else function.typeParameterSymbols)
      .map { it.toInjektClassifier() },
    initialInjectables = buildList {
      if (function.resolvedReceiverType != null)
        this += injectableReceiverOf(
          EXTENSION_RECEIVER_NAME,
          function.resolvedReceiverType!!,
          function,
          function.fir.receiverParameter!!.source!!.startOffset,
          function.fir.receiverParameter!!.source!!.endOffset
        ).toInjektCallable()

      this += function.contextParameterSymbols
        .map { it.toInjektCallable() }

      this += function.valueParameterSymbols
        .filterIndexed { index, valueParameter ->
          valueParameter.isInjectable()
        }
        .map { it.toInjektCallable() }
    },
    callContext = function.callContext()
  )
}

context(ctx: InjektContext)
private fun propertyInjectablesScopeOf(
  property: FirPropertySymbol,
  parent: InjectablesScope
): InjectablesScope = cached(
  "property_scope",
  property to parent.name
) {
  injectableScopeOrParentIfEmptyAndSameCallContext(
    name = "PROPERTY ${property.fqName}",
    parent = parent,
    owner = property,
    typeParameters = property.typeParameterSymbols.map { it.toInjektClassifier() },
    initialInjectables = buildList {
      if (property.resolvedReceiverType != null)
        this += injectableReceiverOf(
          EXTENSION_RECEIVER_NAME,
          property.resolvedReceiverType!!,
          property.getterSymbol!!,
          property.fir.receiverParameter!!.source!!.startOffset,
          property.fir.receiverParameter!!.source!!.endOffset
        )
          .toInjektCallable()

      this += property.contextParameterSymbols
        .map { it.toInjektCallable() }
    },
    callContext = if (property.isLocal) parent.callContext
    else property.callContext()
  )
}

context(ctx: InjektContext)
private fun blockExpressionScopeOf(
  block: FirBlock,
  position: FirElement,
  parent: InjectablesScope
): InjectablesScope {
  val injectablesBeforePosition = block.statements
    .filter { declaration ->
      declaration.source!!.endOffset < position.source!!.startOffset &&
          declaration.safeAs<FirDeclaration>()?.symbol?.isInjectable() == true
    }
    .flatMap { declaration ->
      when (declaration) {
        is FirRegularClass -> declaration.symbol.collectInjectableConstructors()
          .map { it.toInjektCallable() }
        is FirCallableDeclaration -> listOf(declaration.symbol.toInjektCallable())
        else -> emptyList()
      }
    }

  return injectableScopeOrParentIfEmptyAndSameCallContext(
    name = "BLOCK AT ${position.source!!.startOffset}",
    parent = parent,
    initialInjectables = injectablesBeforePosition,
    nesting = if (injectablesBeforePosition.size > 1) parent.nesting
    else parent.nesting + 1,
    callContext = parent.callContext
  )
}

context(ctx: InjektContext)
private fun internalGlobalInjectablesScopeOf(file: FirFileSymbol): InjectablesScope =
  cached("internal_global_scope", file) {
    injectableScopeOrParentIfEmptyAndSameCallContext(
      name = "INTERNAL GLOBAL EXCEPT ${file.fir.name}",
      parent = externalGlobalInjectablesScopeOf(file),
      initialInjectables = collectGlobalInjectables()
        .filter {
          it.symbol.moduleData == session.moduleData &&
              session.firProvider.getContainingFile(it.symbol)?.symbol != file
        }
    )
  }

context(ctx: InjektContext)
private fun externalGlobalInjectablesScopeOf(file: FirFileSymbol): InjectablesScope =
  cached("external_global_scope", Unit) {
    InjectablesScope(
      name = "EXTERNAL GLOBAL",
      parent = null,
      owner = file,
      initialInjectables = collectGlobalInjectables()
        .filter { it.symbol.moduleData != session.moduleData },
      ctx = ctx
    )
  }

context(ctx: InjektContext)
fun injectableScopeOrParentIfEmptyAndSameCallContext(
  name: String,
  parent: InjectablesScope,
  owner: FirBasedSymbol<*>? = null,
  initialInjectables: List<InjektCallable> = emptyList(),
  typeParameters: List<InjektClassifier> = emptyList(),
  nesting: Int = parent.nesting.inc(),
  callContext: CallContext = CallContext.DEFAULT
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
