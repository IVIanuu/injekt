/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class, DfaInternals::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun ElementInjectablesScope(
  position: FirElement,
  containingElements: List<FirElement>,
  ctx: InjektContext,
): InjectablesScope {
  val scopeOwner = containingElements.last { it.isScopeOwner(position, containingElements) }

  fun createScope(): InjectablesScope {
    val parentScope = containingElements
      .take(containingElements.indexOf(scopeOwner))
      .let { parentElements ->
        parentElements
          .lastOrNull { it.isScopeOwner(position, parentElements) }
          ?.let { ElementInjectablesScope(it, parentElements, ctx) }
      }

    return when (scopeOwner) {
      is FirFile -> FileInjectablesScope(scopeOwner.symbol, ctx)
      is FirClass -> ClassInjectablesScope(
        scopeOwner.symbol,
        parentScope!!,
        ctx
      )
      /*is KtConstructor<*> ->
        if (scopeOwner.bodyExpression.let { it == null || it !in position.parents })
        ConstructorPreInitInjectablesScope(
          scopeOwner.descriptor(ctx)!!,
          parentScope!!,
          ctx
        ) else FunctionInjectablesScope(scopeOwner.descriptor(ctx)!!, parentScope!!, ctx)*/
      is FirFunction -> FunctionInjectablesScope(
        scopeOwner.symbol,
        parentScope!!,
        ctx
      )
      /*is KtParameter -> ValueParameterDefaultValueInjectablesScope(
        scopeOwner.descriptor(ctx)!!,
        parentScope!!,
        ctx
      )*/
      is FirProperty -> PropertyInjectablesScope(scopeOwner.symbol, parentScope!!, ctx)
      /*is KtSuperTypeList -> scopeOwner.getParentOfType<KtClassOrObject>(false)
        ?.descriptor<ClassDescriptor>(ctx)
        ?.unsubstitutedPrimaryConstructor
        ?.let { ConstructorPreInitInjectablesScope(it, parentScope!!, ctx) }
        ?: parentScope!!*/
      /*is KtClassInitializer -> ClassInitInjectablesScope(
        clazz = scopeOwner.getParentOfType<KtClassOrObject>(false)!!.descriptor(ctx)!!,
        parent = parentScope!!,
        position = position,
        ctx = ctx
      )
      is KtClassBody -> scopeOwner.getParentOfType<KtClassOrObject>(false)
        ?.descriptor<ClassDescriptor>(ctx)
        ?.unsubstitutedPrimaryConstructor
        ?.let { FunctionInjectablesScope(it, parentScope!!, ctx) }
        ?: parentScope!!*/
      is FirBlock -> BlockExpressionInjectablesScope(scopeOwner, position, parentScope!!, ctx)
      else -> throw AssertionError("Unexpected scope owner $scopeOwner")
    }
  }

  return if (scopeOwner !is KtBlockExpression)
    ctx.cached("element_scope", scopeOwner) { createScope() }
  else
    createScope()
}

private fun FirElement.isScopeOwner(
  position: FirElement,
  containingElements: List<FirElement>,
): Boolean {
  if (
    this is FirBlock ||
    this is FirFile ||
    this is FirProperty
    ) return true

  if (this is FirFunction && containingElements.none { it in valueParameters })
    return true

  if (this is FirClassSymbol<*>)
    return true

  return false
}

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

private fun FileInitInjectablesScope(position: FirElement, ctx: InjektContext): InjectablesScope {
  /*val file = position

  val visibleInjectableDeclarations = file
    .declarations
    .transform { declaration ->
      if (declaration.endOffset < position.startOffset &&
          declaration is KtNamedDeclaration) {
        declaration.descriptor<DeclarationDescriptor>(ctx)
          ?.takeIf { it.isProvide(ctx) }
          ?.let { add(it) }
      }
    }

  val injectableDeclaration = visibleInjectableDeclarations.lastOrNull()

  return InjectableScopeOrParent(
    name = "FILE INIT ${file.name} at ${injectableDeclaration?.name}",
    parent = InternalGlobalInjectablesScope(ctx, file),
    owner = file,
    injectablesPredicate = {
      val psiProperty = it.callable!!.findPsi().safeAs<KtProperty>() ?: return@InjectableScopeOrParent true
      psiProperty.containingFile != file ||
          psiProperty.delegateExpressionOrInitializer == null ||
          it.callable in visibleInjectableDeclarations
    },
    ctx = ctx,
    initialInjectables = collectPackageInjectables(file.packageFqName, ctx)
      .filter { it.callable!!.findPsi()?.containingFile == file }
  )*/
  TODO()
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
      ctx
    ).toInjektCallable(ctx)),
    typeParameters = clazz.typeParameterSymbols.map { it.toInjektClassifier(ctx) },
    ctx = ctx
  )
}

private fun ClassInitInjectablesScope(
  clazz: ClassDescriptor,
  parent: InjectablesScope,
  position: KtElement,
  ctx: InjektContext
): InjectablesScope {
  /*val psiClass = clazz.findPsi()!!
  val visibleInjectableDeclarations = psiClass
    .cast<KtClassOrObject>()
    .declarations
    .transform { declaration ->
      if (declaration.endOffset < position.startOffset &&
        declaration is KtNamedDeclaration) {
        declaration.descriptor<DeclarationDescriptor>(ctx)
          ?.takeIf { it.isProvide(ctx) }
          ?.let { add(it) }
      }
    }

  val injectableDeclaration = visibleInjectableDeclarations.lastOrNull()

  val name = if (clazz.isCompanionObject)
    "COMPANION INIT ${clazz.containingDeclaration.fqNameSafe} at ${injectableDeclaration?.name}"
  else "CLASS INIT ${clazz.fqNameSafe} at ${injectableDeclaration?.name}"

  val classInitScope = InjectableScopeOrParent(
    name = name,
    parent = parent,
    owner = clazz.findPsi().cast(),
    initialInjectables = listOf(clazz.injectableReceiver(ctx)),
    injectablesPredicate = {
      val psiProperty = it.callable!!.findPsi().safeAs<KtProperty>() ?: return@InjectableScopeOrParent true
      psiProperty.getParentOfType<KtClass>(false) != psiClass ||
          psiProperty.delegateExpressionOrInitializer == null ||
          it.callable in visibleInjectableDeclarations
    },
    typeParameters = clazz.declaredTypeParameters.map { it.toInjektClassifier(ctx) },
    ctx = ctx
  )

  val primaryConstructor = clazz.unsubstitutedPrimaryConstructor

  return if (primaryConstructor == null) classInitScope
  else FunctionInjectablesScope(primaryConstructor, classInitScope, ctx)*/
  TODO()
}

private fun ConstructorPreInitInjectablesScope(
  constructor: ConstructorDescriptor,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope {
  /*val parameterScopes = FunctionParameterInjectablesScopes(
    parent = ClassCompanionInjectablesScope(constructor.constructedClass, parent, ctx),
    function = constructor,
    until = null,
    ctx = ctx
  )
  val typeParameters = constructor.constructedClass.declaredTypeParameters.map {
    it.toInjektClassifier(ctx)
  }
  return InjectableScopeOrParent(
    name = "CONSTRUCTOR PRE INIT ${constructor.fqNameSafe}",
    parent = parameterScopes,
    owner = constructor.findPsi().cast(),
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting,
    ctx = ctx
  )*/ TODO()
}

private fun ValueParameterDefaultValueInjectablesScope(
  valueParameter: ValueParameterDescriptor,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope {
  /*val function = valueParameter.containingDeclaration.cast<FunctionDescriptor>()
  val parameterScopes = FunctionParameterInjectablesScopes(
    if (function is ConstructorDescriptor) ClassCompanionInjectablesScope(function.constructedClass, parent, ctx)
    else parent,
    function,
    valueParameter,
    ctx
  )
  return InjectableScopeOrParent(
    name = "DEFAULT VALUE ${valueParameter.fqNameSafe}",
    parent = parameterScopes,
    owner = function.findPsi().cast(),
    typeParameters = function.typeParameters.map { it.toInjektClassifier(ctx) },
    ctx = ctx
  )*/ TODO()
}

private fun FunctionInjectablesScope(
  function: FirFunctionSymbol<*>,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope = ctx.cached(
  "function_scope",
  function to parent.name
) {
  val parameterScopes = FunctionParameterInjectablesScopes(parent, function, null, ctx)
  val baseName = if (function is FirConstructorSymbol) "CONSTRUCTOR" else "FUNCTION"
  val typeParameters = (if (function is FirConstructorSymbol)
    function.resolvedReturnType.cast<FirClassSymbol<*>>().typeParameterSymbols
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
  ctx: InjektContext
): InjectablesScope {
  val maxIndex = function.valueParameterSymbols.indexOfFirst { it == until }

  return buildList<FirValueParameterSymbol> {
    if (function.dispatchReceiverType != null)
      this += injectableReceiver(DISPATCH_RECEIVER_INDEX, function.dispatchReceiverType!!, function, ctx)
    if (function.receiverParameter != null)
      this += injectableReceiver(EXTENSION_RECEIVER_INDEX, function.receiverParameter!!.typeRef.coneType, function, ctx)
    this += function.valueParameterSymbols
  }
    .filterIndexed { index, valueParameter ->
      (maxIndex == -1 || index < maxIndex) && valueParameter.isInjectable(ctx)
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
      if (property.dispatchReceiverType != null)
        this += injectableReceiver(DISPATCH_RECEIVER_INDEX, property.dispatchReceiverType!!, property.getterSymbol!!, ctx)
          .toInjektCallable(ctx)

      if (property.receiverParameter != null)
        this += injectableReceiver(EXTENSION_RECEIVER_INDEX, property.receiverParameter!!.typeRef.coneType, property.getterSymbol!!, ctx)
          .toInjektCallable(ctx)
    },
    typeParameters = property.typeParameterSymbols.map { it.toInjektClassifier(ctx) },
    ctx = ctx
  )
}

private fun PropertyInitInjectablesScope(
  property: PropertyDescriptor,
  parent: InjectablesScope,
  position: KtElement,
  ctx: InjektContext
): InjectablesScope {
  /*val finalParent = if (property.containingDeclaration is ClassDescriptor) {
    ClassInitInjectablesScope(
      clazz = property.containingDeclaration.cast(),
      parent = parent,
      position = position,
      ctx = ctx
    )
  } else {
    TODO()// FileInitInjectablesScope(position = position, ctx = ctx)
  }

  return InjectableScopeOrParent(
    name = "PROPERTY INIT ${property.fqNameSafe}",
    parent = finalParent,
    owner = property.findPsi().cast(),
    typeParameters = property.typeParameters.map { it.toInjektClassifier(ctx) },
    ctx = ctx
  )*/ TODO()
}

private fun LocalVariableInjectablesScope(
  variable: LocalVariableDescriptor,
  parent: InjectablesScope,
  ctx: InjektContext
): InjectablesScope = ctx.cached(
  "local_variable_scope",
  variable to parent.name
) {
  InjectableScopeOrParent(
    name = "LOCAL VARIABLE ${variable.fqNameSafe}",
    parent = parent,
    owner = variable.findPsi().cast(),
    nesting = parent.nesting,
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
          declaration.symbol?.isInjectable(ctx) == true
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
      parent = ExternalGlobalInjectablesScope(ctx),
      initialInjectables = collectGlobalInjectables(ctx)
        .filter {
          it.symbol.moduleData == ctx.session.moduleData &&
              ctx.session.firProvider.getContainingFile(it.symbol)?.symbol != file
        },
      ctx = ctx
    )
  }

fun ExternalGlobalInjectablesScope(ctx: InjektContext): InjectablesScope =
  ctx.cached("external_global_scope", Unit) {
    InjectablesScope(
      name = "EXTERNAL GLOBAL",
      parent = null,
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
  injectablesPredicate: (InjektCallable) -> Boolean = { true },
  typeParameters: List<InjektClassifier> = emptyList(),
  nesting: Int = parent.nesting.inc(),
  ctx: InjektContext
): InjectablesScope {
  val finalInitialInjectables = initialInjectables.filter(injectablesPredicate)
  return if (typeParameters.isEmpty() && finalInitialInjectables.isEmpty()) parent
  else InjectablesScope(name, parent, owner, finalInitialInjectables, injectablesPredicate, typeParameters, nesting, ctx)
}
