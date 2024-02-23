/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.frontend.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

fun InjektType.collectInjectables(
  classBodyView: Boolean,
  ctx: Context
): List<InjektCallable> = ctx.cached("type_injectables", this to classBodyView) {
  buildList {
    classifier
      .descriptor
      ?.defaultType
      ?.memberScope
      ?.collectMemberInjectables(ctx, this@collectInjectables) { callable ->
        val substitutionMap = if (callable.callable.safeAs<CallableMemberDescriptor>()?.kind ==
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
        )
      }
  }
}

fun ResolutionScope.collectMemberInjectables(
  ctx: Context,
  type: InjektType? = null,
  onEach: (DeclarationDescriptor) -> Unit = {},
  consumer: (InjektCallable) -> Unit
) {
  for (declaration in getContributedDescriptors()) {
    onEach(declaration)
    if ((declaration is CallableMemberDescriptor || declaration is VariableDescriptor) &&
      (declaration.isProvide(ctx) ||
          (declaration.name.asString() == "invoke" &&
              type?.isProvideFunctionType(ctx) == true)))
      consumer(declaration.cast<CallableDescriptor>().toInjektCallable(ctx))
  }
}

fun Annotated.isProvide(ctx: Context): Boolean =
  hasAnnotation(InjektFqNames.Provide) ||
      (this is ParameterDescriptor && type.hasAnnotation(InjektFqNames.Provide)) ||
      (this is ValueParameterDescriptor &&
          index in containingDeclaration.cast<CallableDescriptor>().callableInfo(ctx).injectParameters) ||
      (this is ParameterDescriptor &&
          containingDeclaration.safeAs<FunctionDescriptor>()
            ?.findPsi()
            ?.safeAs<KtFunction>()
            ?.getArgumentDescriptor(ctx)
            ?.containingDeclaration
            ?.returnType
            ?.memberScope
            ?.getContributedDescriptors()
            ?.filterIsInstance<FunctionDescriptor>()
            ?.singleOrNull { it.modality == Modality.ABSTRACT }
            ?.valueParameters
            ?.singleOrNull { it.injektIndex() == injektIndex() }
            ?.isProvide(ctx) == true)

fun ClassDescriptor.injectableConstructors(ctx: Context): List<InjektCallable> =
  ctx.cached("injectable_constructors", this) {
    constructors
      .transform { constructor ->
        if (constructor.hasAnnotation(InjektFqNames.Provide) ||
          (constructor.isPrimary && hasAnnotation(InjektFqNames.Provide)))
            add(constructor.toInjektCallable(ctx))
      }
  }

fun ClassDescriptor.injectableReceiver(ctx: Context): InjektCallable =
  ReceiverParameterDescriptorImpl(
    this,
    ImplicitClassReceiver(this),
    Annotations.EMPTY
  ).toInjektCallable(ctx)

fun InjektCallable.collectInjectables(
  scope: InjectablesScope,
  addInjectable: (InjektCallable) -> Unit,
  addAddOnInjectable: (InjektCallable) -> Unit,
  ctx: Context
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
      nextCallable.type.classifier.descriptor?.parentsWithSelf
        ?.mapNotNull { it.findPsi() }
        ?.any { callableParent -> scope.allScopes.any { it.owner == callableParent } } == true,
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

fun collectGlobalInjectables(ctx: Context): List<InjektCallable> = packagesWithInjectables(ctx)
  .flatMap { collectPackageInjectables(it, ctx) }

fun collectPackageInjectables(
  packageFqName: FqName,
  ctx: Context
): List<InjektCallable> = ctx.cached("package_injectables", packageFqName) {
    if (packageFqName !in packagesWithInjectables(ctx)) return@cached emptyList()

    val injectables = mutableListOf<InjektCallable>()

    fun collectInjectables(scope: MemberScope) {
      scope.collectMemberInjectables(
        ctx = ctx,
        onEach = { declaration ->
          if (declaration is ClassDescriptor) {
            collectInjectables(declaration.unsubstitutedInnerClassesScope)
            injectables += declaration.injectableConstructors(ctx)
          }
        }
      ) {
        injectables += it
      }
    }

    collectInjectables(memberScopeForFqName(packageFqName, NoLookupLocation.FROM_BACKEND, ctx)!!)

    injectables
  }

private fun InjectablesScope.canSee(callable: InjektCallable, ctx: Context): Boolean =
  callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          callable.callable.moduleName(ctx) == ctx.module.moduleName(ctx)) ||
      (callable.callable is ClassConstructorDescriptor &&
          callable.type.unwrapTags().classifier.isObject) ||
      callable.callable.parentsWithSelf.mapNotNull { it.findPsi() }.any { callableParent ->
        allScopes.any { it.owner == callableParent }
      } ||
      (callable.callable.findPsi()?.isTopLevelKtOrJavaMember() == true &&
          callable.callable.findPsi()!!.containingFile in allScopes.mapNotNull { it.owner?.containingFile })

fun packagesWithInjectables(ctx: Context): Set<FqName> =
  ctx.cached("packages_with_injectables", Unit) {
    memberScopeForFqName(InjektFqNames.InjectablesPackage, NoLookupLocation.FROM_BACKEND, ctx)
      ?.getContributedFunctions(InjektFqNames.InjectablesLookup.shortName(), NoLookupLocation.FROM_BACKEND)
      ?.mapTo(mutableSetOf()) {
        it.valueParameters.first().type.constructor.declarationDescriptor!!.containingPackage()!!
      } ?: emptySet()
  }
