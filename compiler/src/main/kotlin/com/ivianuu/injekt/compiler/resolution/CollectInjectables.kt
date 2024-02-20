/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

fun ConeKotlinType.collectInjectables(
  classBodyView: Boolean,
  session: FirSession
): List<InjektCallable> {
  return emptyList()
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType(session)) {
    TODO()
    /*val callable = classifier
      .descriptor!!
      .defaultType
      .memberScope
      .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
      .first()
      .toInjektCallable(ctx)
      .let { callable ->
        callable.copy(
          type = arguments.last(),
          parameterTypes = callable.parameterTypes.toMutableMap().apply {
            this[DISPATCH_RECEIVER_INDEX] = this@collectInjectables
          }
        ).substitute(
          classifier.typeParameters
            .zip(arguments)
            .toMap()
        )
      }

    return@cached listOf(callable)*/
  }

  /*val classSymbol = toRegularClassSymbol(session)
    ?: return emptyList()

  buildList {
    classSymbol
      .declarationSymbols

    classifier
      .descriptor
      ?.defaultType
      ?.memberScope
      ?.collectMemberInjectables(ctx) { callable ->
        val substitutionMap = if (callable.callable.safeAs<CallableMemberDescriptor>()?.kind ==
          CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
          val originalClassifier = callable.callable.cast<CallableMemberDescriptor>()
            .overriddenTreeAsSequence(false)
            .last()
            .containingDeclaration
            .cast<ClassDescriptor>()
            .toInjektClassifier(ctx)
          classifier.typeParameters.zip(arguments).toMap() + originalClassifier.typeParameters
            .zip(subtypeView(originalClassifier)!!.arguments)
        } else classifier.typeParameters.zip(arguments).toMap()
        val substituted = callable.substitute(substitutionMap)

        add(
          substituted.copy(
            parameterTypes = if (substituted.parameterTypes[DISPATCH_RECEIVER_INDEX] != this@collectInjectables) {
              substituted.parameterTypes.toMutableMap()
                .also { it[DISPATCH_RECEIVER_INDEX] = this@collectInjectables }
            } else substituted.parameterTypes
          )
        )
      }
  }*/
}

/**fun ResolutionScope.collectMemberInjectables(
  ctx: Context,
  onEach: (DeclarationDescriptor) -> Unit = {},
  consumer: (InjektCallable) -> Unit
) {
  for (declaration in getContributedDescriptors()) {
    onEach(declaration)
    if ((declaration is CallableMemberDescriptor || declaration is VariableDescriptor) &&
      declaration.isProvide(ctx))
      consumer(declaration.cast<CallableDescriptor>().toInjektCallable(ctx))
  }
}

fun Annotated.isProvide(ctx: Context): Boolean =
  hasAnnotation(InjektFqNames.Provide) ||
      this is ParameterDescriptor && type.hasAnnotation(InjektFqNames.Provide)

fun ClassDescriptor.injectableConstructors(ctx: Context): List<InjektCallable> =
  ctx.cache.cached("injectable_constructors", this) {
    constructors
      .transform { constructor ->
        if (constructor.hasAnnotation(InjektFqNames.Provide) ||
          (constructor.isPrimary && hasAnnotation(InjektFqNames.Provide)))
            add(constructor.toInjektCallable(ctx))
      }
  }

fun ClassDescriptor.injectableReceiver(tagged: Boolean, ctx: Context): InjektCallable {
  val callable = ReceiverParameterDescriptorImpl(
    this,
    ImplicitClassReceiver(this),
    Annotations.EMPTY
  ).toInjektCallable(ctx)
  return if (!tagged || callable.type.classifier.tags.isEmpty()) callable
  else {
    val taggedType = callable.type.classifier.tags.wrap(callable.type)
    callable.copy(type = taggedType, originalType = taggedType)
  }
}*/

fun InjektCallable.collectInjectables(
  scope: InjectablesScope,
  addInjectable: (InjektCallable) -> Unit,
  addSpreadingInjectable: (InjektCallable) -> Unit,
  session: FirSession
) {
  if (!scope.canSee(this, session) ||
    !scope.allScopes.all { it.injectablesPredicate(this) }) return

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

  /*nextCallable
    .type
    .collectInjectables(
      nextCallable.type.classifier.descriptor?.parentsWithSelf
        ?.mapNotNull { it.findPsi() }
        ?.any { callableParent -> scope.allScopes.any { it.owner == callableParent } } == true,
      session
    )
    .forEach { innerCallable ->
      innerCallable
        .copy(
          chainFqName = nextCallable.chainFqName.child(innerCallable.chainFqName.shortName()),
          type = if (nextCallable.type.isNullableType()) innerCallable.type.withNullability(true)
          else innerCallable.type,
          originalType = if (nextCallable.type.isNullableType()) innerCallable.type.withNullability(true)
          else innerCallable.type,
          parameterTypes = if (nextCallable.type.isNullableType() &&
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
          addSpreadingInjectable = addSpreadingInjectable,
          session = session
        )
    }*/
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
  .filter { declarationSymbol ->
    declarationSymbol is FirConstructorSymbol &&
        ((declarationSymbol.isPrimary &&
            hasAnnotation(InjektFqNames.Provide, session)) ||
            declarationSymbol.hasAnnotation(InjektFqNames.Provide, session))
  }

fun collectGlobalInjectables(session: FirSession): List<InjektCallable> = collectPackagesWithInjectables(session)
  .flatMap { collectPackageInjectables(it, session) }

fun collectPackagesWithInjectables(session: FirSession): Set<FqName> =
  session.symbolProvider.getTopLevelFunctionSymbols(
    InjektFqNames.InjectablesLookup.packageName,
    InjektFqNames.InjectablesLookup.callableName
  ).mapTo(mutableSetOf()) {
    it.valueParameterSymbols.first().resolvedReturnType.classId!!.packageFqName
  }

fun collectPackageInjectables(packageFqName: FqName, session: FirSession): List<InjektCallable> =
  if (packageFqName !in collectPackagesWithInjectables(session)) emptyList()
  else buildList {
    fun collectClassInjectables(classSymbol: FirClassSymbol<*>) {
      for (declarationSymbol in classSymbol.declarationSymbols) {
        if (declarationSymbol is FirConstructorSymbol &&
          ((declarationSymbol.isPrimary &&
              classSymbol.hasAnnotation(InjektFqNames.Provide, session)) ||
              declarationSymbol.hasAnnotation(InjektFqNames.Provide, session)))
          add(declarationSymbol.toInjektCallable())

        if (declarationSymbol is FirClassSymbol<*>)
          collectClassInjectables(declarationSymbol)
      }
    }

    session.symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName)
      ?.mapNotNull {
        session.symbolProvider.getRegularClassSymbolByClassId(ClassId(packageFqName, it.asNameId()))
      }
      ?.forEach { collectClassInjectables(it) }

    session.symbolProvider.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName)
      ?.flatMap { name ->
        session.symbolProvider.getTopLevelCallableSymbols(packageFqName, name)
      }
      ?.filter { it.hasAnnotation(InjektFqNames.Provide, session) }
      ?.forEach { add(it.toInjektCallable()) }
  }
