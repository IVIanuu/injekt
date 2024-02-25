/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
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

fun ElementInjectablesScope(
  containingElements: List<FirElement>,
  position: FirElement,
  ctx: InjektContext,
): InjectablesScope = containingElements.fold(null as InjectablesScope?) { parentScope, element ->
  when (element) {
    is FirFile -> FileInjectablesScope(element.symbol, ctx)
    is FirClass -> ClassInjectablesScope(element.symbol, parentScope!!, ctx)
    is FirFunction -> FunctionInjectablesScope(element.symbol, parentScope!!, containingElements, ctx)
    is FirProperty -> PropertyInjectablesScope(element.symbol, parentScope!!, ctx)
    is FirBlock -> BlockExpressionInjectablesScope(element, position, parentScope!!, ctx)
    else -> parentScope
  }
}!!

private fun FileInjectablesScope(file: FirFileSymbol, ctx: InjektContext): InjectablesScope =
  ctx.cached("file_scope", file) {
    InjectableScopeOrParent(
      name = "FILE ${file.fir.name}",
      parent = InternalGlobalInjectablesScope(ctx, file),
      owner = file,
      ctx = ctx,
      initialInjectables = collectPackageInjectables(file.fir.packageFqName, ctx)
        .filter { ctx.session.firProvider.getContainingFile(it.symbol)?.symbol == file }
    )
  }

private fun ClassCompanionInjectablesScope(
  clazz: FirClassSymbol<*>,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope = clazz.safeAs<FirRegularClassSymbol>()?.companionObjectSymbol
  ?.let { ClassInjectablesScope(it, parent, ctx) } ?: parent

private fun ClassInjectablesScope(
  clazz: FirClassSymbol<*>,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope = ctx.cached(
  "class_scope",
  clazz to parent.name
) {
  val finalParent = ClassCompanionInjectablesScope(clazz, parent, ctx)
  val name = if (clazz.isCompanion) "COMPANION ${clazz.fqName}"
  else "CLASS ${clazz.fqName}"
  InjectableScopeOrParent(
    name = name,
    parent = finalParent,
    owner = clazz,
    initialInjectables = listOf(injectableReceiver(
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

private fun FunctionInjectablesScope(
  function: FirFunctionSymbol<*>,
  parent: InjectablesScope,
  containingElements: List<FirElement>,
  ctx: InjektContext
): InjectablesScope = ctx.cached(
  "function_scope",
  function to parent.name
) {
  val parameterScopes = FunctionParameterInjectablesScopes(parent, function, null, containingElements, ctx)
  val baseName = if (function is FirConstructorSymbol) "CONSTRUCTOR" else "FUNCTION"
  val typeParameters = (if (function is FirConstructorSymbol)
    function.resolvedReturnType.type.toRegularClassSymbol(ctx.session)!!.typeParameterSymbols
  else function.typeParameterSymbols)
    .map { it.toInjektClassifier(ctx) }
  InjectableScopeOrParent(
    name = "$baseName ${function.fqName}",
    parent = parameterScopes,
    owner = function,
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting,
    ctx = ctx
  )
}

private fun FunctionParameterInjectablesScopes(
  parent: InjectablesScope,
  function: FirFunctionSymbol<*>,
  until: FirValueParameterSymbol? = null,
  containingElements: List<FirElement>,
  ctx: InjektContext
): InjectablesScope {
  val maxIndex = function.valueParameterSymbols.indexOfFirst { it == until }
  val lambdaValueParameterTypes = function.safeAs<FirAnonymousFunctionSymbol>()
    ?.fir?.typeRef?.coneType?.typeArguments?.map { it.toInjektType(ctx) }
  val funInterfaceProvideValueParameters = function.safeAs<FirAnonymousFunctionSymbol>()
    ?.let {
      containingElements.getOrNull(containingElements.indexOf(function.fir) - 3)
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
  return buildList<FirValueParameterSymbol> {
    if (function.receiverParameter?.hasAnnotation(InjektFqNames.Provide, ctx.session) == true ||
      function.receiverParameter != null &&
      (lambdaValueParameterTypes?.get(0)?.isProvide == true ||
          funInterfaceProvideValueParameters?.contains(EXTENSION_RECEIVER_INDEX) == true))
      this += injectableReceiver(
        EXTENSION_RECEIVER_INDEX,
        function.receiverParameter!!.typeRef.coneType,
        function,
        function.receiverParameter!!.source!!.startOffset,
        function.receiverParameter!!.source!!.endOffset,
        ctx
      )
    this += function.valueParameterSymbols
  }
    .filterIndexed { index, valueParameter ->
      (maxIndex == -1 || index < maxIndex) && (valueParameter.isInjectable(ctx) ||
          lambdaValueParameterTypes?.get(
            index + (if (function.receiverParameter != null) 1 else 0)
          )?.isProvide == true ||
          funInterfaceProvideValueParameters?.contains(index) == true)
    }
    .fold(parent) { acc, nextParameter ->
      FunctionParameterInjectablesScope(
        parent = acc,
        function = function,
        parameter = nextParameter,
        ctx = ctx
      )
    }
}

private fun FunctionParameterInjectablesScope(
  parent: InjectablesScope,
  function: FirFunctionSymbol<*>,
  parameter: FirValueParameterSymbol,
  ctx: InjektContext
): InjectablesScope = InjectableScopeOrParent(
  name = "FUNCTION PARAMETER ${parameter.name}",
  parent = parent,
  owner = parameter,
  initialInjectables = listOf(parameter.toInjektCallable(ctx)),
  typeParameters = function.typeParameterSymbols.map { it.toInjektClassifier(ctx) },
  nesting = if (parent.name.startsWith("FUNCTION PARAMETER")) parent.nesting
  else parent.nesting + 1,
  ctx = ctx
)

private fun PropertyInjectablesScope(
  property: FirPropertySymbol,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope = ctx.cached(
  "property_scope",
  property to parent.name
) {
  InjectableScopeOrParent(
    name = "PROPERTY ${property.fqName}",
    parent = parent,
    owner = property,
    initialInjectables = buildList {
      if (property.receiverParameter?.hasAnnotation(InjektFqNames.Provide, ctx.session) == true)
        this += injectableReceiver(
          EXTENSION_RECEIVER_INDEX,
          property.receiverParameter!!.typeRef.coneType,
          property.getterSymbol!!,
          property.receiverParameter!!.source!!.startOffset,
          property.receiverParameter!!.source!!.endOffset,
          ctx
        )
          .toInjektCallable(ctx)
    },
    typeParameters = property.typeParameterSymbols.map { it.toInjektClassifier(ctx) },
    ctx = ctx
  )
}

private fun BlockExpressionInjectablesScope(
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
    .flatMap {
      when (it) {
        is FirRegularClass -> it.symbol.collectInjectableConstructors(ctx)
          .map { it.toInjektCallable(ctx) }
        is FirCallableDeclaration -> listOf(it.symbol.toInjektCallable(ctx))
        else -> emptyList()
      }
    }
  return if (injectablesBeforePosition.isEmpty()) parent
  else InjectableScopeOrParent(
    name = "BLOCK AT ${position.source!!.startOffset}",
    parent = parent,
    initialInjectables = injectablesBeforePosition,
    nesting = if (injectablesBeforePosition.size > 1) parent.nesting
    else parent.nesting + 1,
    ctx = ctx
  )
}

fun InternalGlobalInjectablesScope(ctx: InjektContext, file: FirFileSymbol): InjectablesScope =
  ctx.cached("internal_global_scope", file) {
    InjectableScopeOrParent(
      name = "INTERNAL GLOBAL EXCEPT ${file.fir.name}",
      parent = ExternalGlobalInjectablesScope(ctx, file),
      initialInjectables = collectGlobalInjectables(ctx)
        .filter {
          it.symbol.moduleData == ctx.session.moduleData &&
              ctx.session.firProvider.getContainingFile(it.symbol)?.symbol != file
        },
      ctx = ctx
    )
  }

fun ExternalGlobalInjectablesScope(ctx: InjektContext, file: FirFileSymbol): InjectablesScope =
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

fun InjectableScopeOrParent(
  name: String,
  parent: InjectablesScope,
  owner: FirBasedSymbol<*>? = null,
  initialInjectables: List<InjektCallable> = emptyList(),
  typeParameters: List<InjektClassifier> = emptyList(),
  nesting: Int = parent.nesting.inc(),
  ctx: InjektContext
): InjectablesScope {
  return if (typeParameters.isEmpty() && initialInjectables.isEmpty()) parent
  else InjectablesScope(name, parent, owner, initialInjectables, typeParameters, nesting, ctx)
}
