/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import java.util.*

fun ConeKotlinType.collectModuleInjectables(session: FirSession): List<InjektCallable> =
  buildList {
    val classSymbol = toRegularClassSymbol(session) ?: return@buildList
    for (declaration in classSymbol.declarationSymbols) {
      if (declaration !is FirConstructorSymbol &&
        declaration is FirCallableSymbol<*> &&
        declaration.hasAnnotation(InjektFqNames.Provide, session))
        this += declaration.toInjektCallable(session)
    }
  }

fun InjektCallable.collectInjectables(
  scope: InjectablesScope,
  addInjectable: (InjektCallable) -> Unit,
  addSpreadingInjectable: (InjektCallable) -> Unit,
  session: FirSession
) {
  if (!scope.canSee(this, session)) return

  if (callable.typeParameterSymbols.any {
    it.hasAnnotation(InjektFqNames.Spread, session) &&
        typeArguments[it] == it.toConeType()
  }) {
    addSpreadingInjectable(this)
    return
  }

  if (type.isUnconstrained(scope.allStaticTypeParameters)) return

  val nextCallable = copy(type = type.withFrameworkKey(UUID.randomUUID().toString()))
  addInjectable(nextCallable)

  nextCallable
    .type
    .collectModuleInjectables(session)
    .forEach { innerCallable ->
      innerCallable
        .copy(
          chainFqName = nextCallable.chainFqName.child(innerCallable.chainFqName.shortName()),
          type = if (nextCallable.type.isNullable) innerCallable.type
            .withNullability(ConeNullability.NULLABLE, session.typeContext)
          else innerCallable.type,
          originalType = if (nextCallable.type.isNullable) innerCallable.type
            .withNullability(ConeNullability.NULLABLE, session.typeContext)
          else innerCallable.type,
          parameterTypes = if (nextCallable.type.isNullable &&
            DISPATCH_RECEIVER_INDEX in innerCallable.parameterTypes) innerCallable.parameterTypes
            .toMutableMap().apply {
              put(
                DISPATCH_RECEIVER_INDEX,
                innerCallable.parameterTypes[DISPATCH_RECEIVER_INDEX]!!
                  .withNullability(ConeNullability.NULLABLE, session.typeContext)
              )
            } else innerCallable.parameterTypes
        )
        .collectInjectables(
          scope = scope,
          addInjectable = addInjectable,
          addSpreadingInjectable = addSpreadingInjectable,
          session = session
        )
    }
}

private fun InjectablesScope.canSee(callable: InjektCallable, session: FirSession): Boolean =
  true
  /*callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          callable.callable.moduleName(ctx) == ctx.module.moduleName(ctx)) ||
      (callable.callable is ClassConstructorDescriptor &&
          callable.type.unwrapTags().classifier.isObject) ||
      callable.callable.parentsWithSelf.mapNotNull { it.findPsi() }.any { callableParent ->
        allScopes.any { it.owner == callableParent }
      } ||
      (callable.callable.findPsi()?.isTopLevelKtOrJavaMember() == true &&
          callable.callable.findPsi()!!.containingFile in allScopes.mapNotNull { it.owner?.containingFile })*/

fun FirRegularClassSymbol.collectInjectableConstructors(session: FirSession) = declarationSymbols
  .mapNotNull { declarationSymbol ->
    if (declarationSymbol is FirConstructorSymbol &&
      ((declarationSymbol.isPrimary &&
          hasAnnotation(InjektFqNames.Provide, session)) ||
          declarationSymbol.hasAnnotation(InjektFqNames.Provide, session)))
      declarationSymbol.toInjektCallable(session)
    else null
  }

fun collectGlobalInjectables(session: FirSession): List<InjektCallable> = collectPackagesWithInjectables(session)
  .flatMap { collectPackageInjectables(it, session) }

fun collectPackageInjectables(packageFqName: FqName, session: FirSession): List<InjektCallable> =
  if (packageFqName !in collectPackagesWithInjectables(session)) emptyList()
  else buildList {
    fun collectClassInjectables(classSymbol: FirClassSymbol<*>) {
      for (declarationSymbol in classSymbol.declarationSymbols) {
        if (declarationSymbol is FirConstructorSymbol &&
          ((declarationSymbol.isPrimary &&
              classSymbol.hasAnnotation(InjektFqNames.Provide, session)) ||
              declarationSymbol.hasAnnotation(InjektFqNames.Provide, session)))
          add(declarationSymbol.toInjektCallable(session))

        if (declarationSymbol is FirClassSymbol<*>)
          collectClassInjectables(declarationSymbol)
      }
    }

    session.symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName)
      ?.mapNotNull {
        session.symbolProvider.getRegularClassSymbolByClassId(ClassId(packageFqName, it))
      }
      ?.forEach { collectClassInjectables(it) }

    session.symbolProvider.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName)
      ?.flatMap { name ->
        session.symbolProvider.getTopLevelCallableSymbols(packageFqName, name)
      }
      ?.filter { it.hasAnnotation(InjektFqNames.Provide, session) }
      ?.forEach { add(it.toInjektCallable(session)) }
  }

fun collectPackagesWithInjectables(session: FirSession): Set<FqName> =
  session.symbolProvider.getTopLevelFunctionSymbols(
    InjektFqNames.InjectablesLookup.packageName,
    InjektFqNames.InjectablesLookup.callableName
  ).mapTo(mutableSetOf()) {
    it.valueParameterSymbols.first().resolvedReturnType.classId!!.packageFqName
  }
