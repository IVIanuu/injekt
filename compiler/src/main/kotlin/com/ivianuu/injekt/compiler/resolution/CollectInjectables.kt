/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

fun InjektType.collectModuleInjectables(
  ctx: InjektContext
): List<InjektCallable> = ctx.cached("module_injectables", this) {
  buildList {
    fun InjektType.visit() {
      val classSymbol = classifier.symbol.safeAs<FirClassSymbol<*>>() ?: return

      superTypes.forEach { it.visit() }

      for (declaration in classSymbol.declarationSymbols) {
        if (declaration !is FirConstructorSymbol &&
          declaration is FirCallableSymbol<*> &&
          !declaration.isOverride &&
          (declaration.hasAnnotation(InjektFqNames.Provide, ctx.session) ||
              (declaration.name.asString() == "invoke" && isProvideFunctionType(ctx)))) {
          val substitutionMap = classifier.typeParameters
            .zip(subtypeView(declaration.dispatchReceiverType?.toInjektType(ctx)?.classifier!!)!!.arguments)
            .toMap()
          this@buildList += declaration.toInjektCallable(ctx)
            .substitute(substitutionMap)
            .let { callable ->
              if (callable.parameterTypes[DISPATCH_RECEIVER_INDEX] == this) callable
              else callable.copy(
                parameterTypes = callable.parameterTypes.toMutableMap()
                  .also { it[DISPATCH_RECEIVER_INDEX] = this }
              )
            }
        }
      }
    }

    visit()
  }
}

fun InjektCallable.collectModuleInjectables(
  scope: InjectablesScope,
  addInjectable: (InjektCallable) -> Unit,
  addAddOnInjectable: (InjektCallable) -> Unit,
  ctx: InjektContext
) {
  if (!scope.canSee(this, ctx) || !scope.allScopes.all { it.injectablesPredicate(this) }) return

  if (typeArguments.any { it.key.isAddOn && it.value == it.key.defaultType }) {
    addAddOnInjectable(this)
    return
  }

  if (type.isUnconstrained(scope.allStaticTypeParameters)) return

  val nextCallable = copy(type = type.copy(uniqueId = UUID.randomUUID().toString()))
  addInjectable(nextCallable)

  nextCallable
    .type
    .collectModuleInjectables(ctx)
    .forEach { innerCallable ->
      innerCallable
        .copy(
          chainFqName = nextCallable.chainFqName.child(innerCallable.chainFqName.shortName()),
          type = if (nextCallable.type.isNullableType) innerCallable.type.withNullability(true)
          else innerCallable.type,
          originalType = if (nextCallable.type.isNullableType) innerCallable.type.withNullability(true)
          else innerCallable.type,
          parameterTypes = if (nextCallable.type.isNullableType &&
            DISPATCH_RECEIVER_INDEX in innerCallable.parameterTypes) innerCallable.parameterTypes
            .toMutableMap().apply {
              put(
                DISPATCH_RECEIVER_INDEX,
                innerCallable.parameterTypes[DISPATCH_RECEIVER_INDEX]!!.withNullability(true)
              )
            } else innerCallable.parameterTypes
        )
        .collectModuleInjectables(
          scope = scope,
          addInjectable = addInjectable,
          addAddOnInjectable = addAddOnInjectable,
          ctx = ctx
        )
    }
}

private fun InjectablesScope.canSee(callable: InjektCallable, ctx: InjektContext): Boolean =
  /*callable.callable!!.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          callable.callable.moduleName(ctx) == ctx.session.moduleData.name.asString()) ||
      (callable.callable is ClassConstructorDescriptor &&
          callable.type.unwrapTags().classifier.isObject) ||
      callable.callable.parentsWithSelf.mapNotNull { it.findPsi() }.any { callableParent ->
        allScopes.any { it.owner == callableParent }
      } ||
      (callable.callable.findPsi()?.isTopLevelKtOrJavaMember() == true &&
          callable.callable.findPsi()!!.containingFile in allScopes.mapNotNull { it.owner?.containingFile })*/ true

fun collectGlobalInjectables(ctx: InjektContext): List<InjektCallable> = collectPackagesWithInjectables(ctx)
  .flatMap { collectPackageInjectables(it, ctx) }

fun collectPackageInjectables(packageFqName: FqName, ctx: InjektContext): List<InjektCallable> =
  ctx.cached("injectables_in_package", packageFqName) {
    if (packageFqName !in collectPackagesWithInjectables(ctx)) emptyList()
    else buildList {
      fun collectClassInjectables(classSymbol: FirClassSymbol<*>) {
        for (declarationSymbol in classSymbol.declarationSymbols) {
          if (declarationSymbol is FirConstructorSymbol &&
            ((declarationSymbol.isPrimary &&
                classSymbol.hasAnnotation(InjektFqNames.Provide, ctx.session)) ||
                declarationSymbol.hasAnnotation(InjektFqNames.Provide, ctx.session)))
            this += declarationSymbol.toInjektCallable(ctx)

          if (declarationSymbol is FirClassSymbol<*>)
            collectClassInjectables(declarationSymbol)
        }
      }

      for (declaration in collectDeclarationsInFqName(packageFqName, ctx)) {
        if (declaration is FirRegularClassSymbol) collectClassInjectables(declaration)
        if (declaration is FirCallableSymbol<*> &&
          declaration.hasAnnotation(InjektFqNames.Provide, ctx.session))
          this += declaration.toInjektCallable(ctx)
      }
    }
  }

fun collectPackagesWithInjectables(ctx: InjektContext): Set<FqName> =
  ctx.cached("packages_with_injectables", Unit) {
    ctx.session.symbolProvider.getTopLevelFunctionSymbols(
      InjektFqNames.InjectablesLookup.packageName,
      InjektFqNames.InjectablesLookup.callableName
    ).mapTo(mutableSetOf()) {
      it.valueParameterSymbols.first().resolvedReturnType.classId!!.packageFqName
    }
  }
