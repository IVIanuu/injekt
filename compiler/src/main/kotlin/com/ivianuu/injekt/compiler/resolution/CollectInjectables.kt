/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(DfaInternals::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

fun InjektType.collectInjectables(
  classBodyView: Boolean,
  ctx: InjektContext
): List<InjektCallable> = ctx.cached("type_injectables", this to classBodyView) {
  buildList {
    /*classifier
      .descriptor
      ?.defaultType
      ?.memberScope
      ?.collectMemberInjectables(ctx, this@collectInjectables) { callable ->
        /*val substitutionMap = if (callable.callable.safeAs<CallableMemberDescriptor>()?.kind ==
          CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
          val originalClassifier = callable.callable.cast<CallableMemberDescriptor>()
            .overriddenTreeAsSequence(false)
            .last()
            .containingDeclaration
            .cast<ClassDescriptor>()
            .toInjektClassifier(ctx)
          buildMap {
            classifier.typeParameters.zip(arguments).forEach { put(it.first, it.second) }
            originalClassifier.typeParameters
              .zip(subtypeView(originalClassifier)!!.arguments)
              .forEach { put(it.first, it.second) }
          }
        } else classifier.typeParameters.zip(arguments).toMap()
        val substituted = callable.substitute(substitutionMap)

        this += substituted.copy(
          parameterTypes = if (substituted.parameterTypes[DISPATCH_RECEIVER_INDEX] != this@collectInjectables) {
            substituted.parameterTypes.toMutableMap()
              .also { it[DISPATCH_RECEIVER_INDEX] = this@collectInjectables }
          } else substituted.parameterTypes
        )*/
        TODO()
      }*/
  }
}

fun InjektCallable.collectInjectables(
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
    .collectInjectables(
      scope.allScopes.any { it.owner?.symbol == nextCallable.type.classifier.symbol },
      ctx
    )
    .forEach { innerCallable ->
      innerCallable
        .copy(
          callableFqName = nextCallable.callableFqName.child(innerCallable.callableFqName.shortName()),
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
        .collectInjectables(
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
  if (packageFqName !in collectPackagesWithInjectables(ctx)) emptyList()
  else buildList {
    fun collectClassInjectables(classSymbol: FirClassSymbol<*>) {
      for (declarationSymbol in classSymbol.declarationSymbols) {
        if (declarationSymbol is FirConstructorSymbol &&
          ((declarationSymbol.isPrimary &&
              classSymbol.hasAnnotation(InjektFqNames.Provide, ctx.session)) ||
              declarationSymbol.hasAnnotation(InjektFqNames.Provide, ctx.session)))
          add(declarationSymbol.toInjektCallable(ctx))

        if (declarationSymbol is FirClassSymbol<*>)
          collectClassInjectables(declarationSymbol)
      }
    }

    ctx.session.symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName)
      ?.mapNotNull {
        ctx.session.symbolProvider.getRegularClassSymbolByClassId(ClassId(packageFqName, it))
      }
      ?.forEach { collectClassInjectables(it) }

    ctx.session.symbolProvider.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName)
      ?.flatMap { name ->
        ctx.session.symbolProvider.getTopLevelCallableSymbols(packageFqName, name)
      }
      ?.filter { it.hasAnnotation(InjektFqNames.Provide, ctx.session) }
      ?.forEach { add(it.toInjektCallable(ctx)) }
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
