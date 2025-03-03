/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class)

package injekt.compiler.resolution

import injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun elementInjectablesScopeOf(
  containingElements: List<FirElement>,
  position: FirElement,
  ctx: InjektContext,
): InjectablesScope {
  fun scopeOf(elements: List<FirElement>): InjectablesScope =
    when (val element = elements.last()) {
      is FirFile -> fileInjectablesScopeOf(file = element.symbol, ctx = ctx)

      is FirClass -> classInjectablesScopeOf(
        clazz = element.symbol,
        parent = scopeOf(
          elements
            .dropLast(1)
            .mapIndexedNotNull { index, parentCandidate ->
              when {
                parentCandidate !is FirRegularClass -> parentCandidate
                index == elements.lastIndex - 1 && element.isInner -> parentCandidate
                else -> parentCandidate.companionObjectSymbol?.fir
              }
            }
        ),
        ctx = ctx
      )

      is FirFunction -> functionInjectablesScopeOf(
        function = element.symbol,
        parent = scopeOf(
          elements.dropLast(1)
            .let { currentElements ->
              currentElements
                .mapIndexedNotNull { index, parentCandidate ->
                  when {
                    parentCandidate !is FirRegularClass -> parentCandidate
                    index == elements.indexOfLast { it is FirClass } -> parentCandidate
                    else -> parentCandidate.companionObjectSymbol?.fir
                  }
                }
            }
        ),
        containingElements = elements,
        ctx = ctx
      )

      is FirProperty -> propertyInjectablesScopeOf(
        property = element.symbol,
        parent = scopeOf(
          elements.dropLast(1)
            .let { currentElements ->
              currentElements
                .mapIndexedNotNull { index, parentCandidate ->
                  when {
                    parentCandidate !is FirRegularClass -> parentCandidate
                    index == elements.indexOfLast { it is FirClass } -> parentCandidate
                    else -> parentCandidate.companionObjectSymbol?.fir
                  }
                }
            }
        ),
        ctx = ctx
      )

      is FirBlock -> blockExpressionScopeOf(
        block = element,
        position = position,
        parent = scopeOf(elements.dropLast(1)),
        ctx = ctx
      )

      else -> scopeOf(elements.dropLast(1))
    }

  return scopeOf(containingElements)
}

private fun fileInjectablesScopeOf(file: FirFileSymbol, ctx: InjektContext): InjectablesScope =
  ctx.cached("file_scope", file) {
    injectableScopeOrParentIfEmpty(
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
  injectableScopeOrParentIfEmpty(
    name = if (clazz.isCompanion) "COMPANION ${clazz.fqName}"
    else "CLASS ${clazz.fqName}",
    parent = classCompanionInjectablesScopeOf(clazz, parent, ctx),
    owner = clazz,
    initialInjectables = listOf(injectableReceiverOf(
      DISPATCH_RECEIVER_INDEX,
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
  val lambdaValueParameterTypes = function.safeAs<FirAnonymousFunctionSymbol>()
    ?.fir?.typeRef?.coneType?.typeArguments?.map { it.toInjektType(ctx) }
  val funInterfaceProvideValueParameters = function.safeAs<FirAnonymousFunctionSymbol>()
    ?.let {
      containingElements.getOrNull(containingElements.indexOf(function.fir) - 2)
        ?.safeAs<FirFunctionCallImpl>()
        ?.resolvedType
        ?.toRegularClassSymbol(ctx.session)
        ?.takeIf { it.isFun }
        ?.declarationSymbols
        ?.filterIsInstance<FirFunctionSymbol<*>>()
        ?.singleOrNull { it.resolvedStatus.modality == Modality.ABSTRACT }
        ?.let { funInterfaceFunction ->
          buildSet {
            if (funInterfaceFunction.receiverParameter
                ?.hasAnnotation(InjektFqNames.Provide, ctx.session) == true)
              this += EXTENSION_RECEIVER_INDEX
            funInterfaceFunction.valueParameterSymbols.forEachIndexed { index, valueParameter ->
              if (valueParameter.isInjectable(ctx))
                this += index
            }
          }
        }
    }
  injectableScopeOrParentIfEmpty(
    name = "${if (function is FirConstructorSymbol) "CONSTRUCTOR" else "FUNCTION"} ${function.fqName}",
    parent = parent,
    owner = function,
    typeParameters = (if (function is FirConstructorSymbol)
      function.resolvedReturnType.toRegularClassSymbol(ctx.session)!!.typeParameterSymbols
    else function.typeParameterSymbols)
      .map { it.toInjektClassifier(ctx) },
    initialInjectables = buildList<FirValueParameterSymbol> {
      if (function.receiverParameter?.hasAnnotation(InjektFqNames.Provide, ctx.session) == true ||
        (function.receiverParameter != null &&
            (lambdaValueParameterTypes?.get(0)?.isProvide == true ||
                funInterfaceProvideValueParameters?.contains(EXTENSION_RECEIVER_INDEX) == true)))
        this += injectableReceiverOf(
          EXTENSION_RECEIVER_INDEX,
          function.receiverParameter!!.typeRef.coneType,
          function,
          function.receiverParameter!!.source!!.startOffset,
          function.receiverParameter!!.source!!.endOffset,
          ctx
        )

      this += function.valueParameterSymbols
        .filterIndexed { index, valueParameter ->
          (valueParameter.isInjectable(ctx) ||
              lambdaValueParameterTypes?.get(
                index + (if (function.receiverParameter != null) 1 else 0)
              )?.isProvide == true ||
              funInterfaceProvideValueParameters?.contains(index) == true)
        }
    }
      .map { it.toInjektCallable(ctx) },
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
  injectableScopeOrParentIfEmpty(
    name = "PROPERTY ${property.fqName}",
    parent = parent,
    owner = property,
    typeParameters = property.typeParameterSymbols.map { it.toInjektClassifier(ctx) },
    initialInjectables = buildList {
      if (property.receiverParameter?.hasAnnotation(InjektFqNames.Provide, ctx.session) == true)
        this += injectableReceiverOf(
          EXTENSION_RECEIVER_INDEX,
          property.receiverParameter!!.typeRef.coneType,
          property.getterSymbol!!,
          property.receiverParameter!!.source!!.startOffset,
          property.receiverParameter!!.source!!.endOffset,
          ctx
        )
          .toInjektCallable(ctx)
    },
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

  return injectableScopeOrParentIfEmpty(
    name = "BLOCK AT ${position.source!!.startOffset}",
    parent = parent,
    initialInjectables = injectablesBeforePosition,
    nesting = if (injectablesBeforePosition.size > 1) parent.nesting
    else parent.nesting + 1,
    ctx = ctx
  )
}

private fun internalGlobalInjectablesScopeOf(file: FirFileSymbol, ctx: InjektContext): InjectablesScope =
  ctx.cached("internal_global_scope", file) {
    injectableScopeOrParentIfEmpty(
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

fun injectableScopeOrParentIfEmpty(
  name: String,
  parent: InjectablesScope,
  owner: FirBasedSymbol<*>? = null,
  initialInjectables: List<InjektCallable> = emptyList(),
  typeParameters: List<InjektClassifier> = emptyList(),
  nesting: Int = parent.nesting.inc(),
  ctx: InjektContext
): InjectablesScope = if (typeParameters.isEmpty() && initialInjectables.isEmpty()) parent
else InjectablesScope(name, parent, owner, initialInjectables, typeParameters, nesting, ctx)
