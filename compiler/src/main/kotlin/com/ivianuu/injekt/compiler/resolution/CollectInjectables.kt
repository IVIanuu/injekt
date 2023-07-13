/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.cached
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.injekt.compiler.transform
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun TypeRef.collectInjectables(
  classBodyView: Boolean,
  ctx: Context
): List<CallableRef> = ctx.cached("type_injectables", this to classBodyView) {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType) {
    val callable = classifier
      .descriptor!!
      .defaultType
      .memberScope
      .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
      .first()
      .toCallableRef(ctx)
      .let { callable ->
        callable.copy(
          type = arguments.last(),
          parameterTypes = callable.parameterTypes.toMutableMap().apply {
            this[DISPATCH_RECEIVER_INDEX] = this@collectInjectables
          }
        ).substitute(
          classifier.typeParameters
            .zip(
              arguments
                .map { it.copy(isInject = true) }
            )
            .toMap()
        )
      }

    return@cached listOf(callable)
  }

  buildList {
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
            .toClassifierRef(ctx)
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
  }
}

fun ResolutionScope.collectMemberInjectables(
  ctx: Context,
  onEach: (DeclarationDescriptor) -> Unit = {},
  consumer: (CallableRef) -> Unit
) {
  for (declaration in getContributedDescriptors()) {
    onEach(declaration)
    when (declaration) {
      is CallableMemberDescriptor -> {
        if (declaration.isProvide())
          consumer(declaration.toCallableRef(ctx))
      }
      is VariableDescriptor -> {
        if (declaration.isProvide())
          consumer(declaration.toCallableRef(ctx))
      }
    }
  }
}

fun Annotated.isProvide(): Boolean =
  (hasAnnotation(InjektFqNames.Provide) || (this is ParameterDescriptor) && type.isProvide()) || isInject()

fun Annotated.isInject(): Boolean = hasAnnotation(InjektFqNames.Inject) ||
    (this is ParameterDescriptor && type.isInject())

fun ClassDescriptor.injectableConstructors(ctx: Context): List<CallableRef> =
  ctx.cached("injectable_constructors", this) {
    constructors
      .transform { constructor ->
        if (constructor.hasAnnotation(InjektFqNames.Provide) ||
          (constructor.isPrimary && hasAnnotation(InjektFqNames.Provide)))
            add(constructor.toCallableRef(ctx))
      }
  }

fun ClassDescriptor.injectableReceiver(tagged: Boolean, ctx: Context): CallableRef {
  val callable = ReceiverParameterDescriptorImpl(
    this,
    ImplicitClassReceiver(this),
    Annotations.EMPTY
  ).toCallableRef(ctx)
  return if (!tagged || callable.type.classifier.tags.isEmpty()) callable
  else {
    val taggedType = callable.type.classifier.tags.wrap(callable.type)
    callable.copy(type = taggedType, originalType = taggedType)
  }
}

fun CallableRef.collectInjectables(
  scope: InjectablesScope,
  addInjectable: (CallableRef) -> Unit,
  addSpreadingInjectable: (CallableRef) -> Unit,
  seen: MutableSet<InjectablesScope.InjectableKey> = mutableSetOf(),
  ctx: Context
) {
  if (!seen.add(InjectablesScope.InjectableKey(this, ctx))) return

  if (!scope.canSee(this, ctx) || !scope.allScopes.all { it.injectablesPredicate(this) }) return

  if (typeParameters.any { it.isSpread && typeArguments[it] == it.defaultType }) {
    addSpreadingInjectable(this)
    return
  }

  val nextCallable = if (!type.isProvideFunctionType) this
  else copy(type = type.copy(frameworkKey = callable.uniqueKey(ctx)))
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
        .collectInjectables(
          scope = scope,
          addInjectable = addInjectable,
          addSpreadingInjectable = addSpreadingInjectable,
          seen = seen,
          ctx = ctx
        )
    }
}

fun collectGlobalInjectables(ctx: Context): List<CallableRef> = buildList {
  packagesWithInjectables(ctx)
    .forEach { collectPackageInjectables(it, ctx).forEach { add(it) } }
}

fun collectPackageInjectables(
  packageFqName: FqName,
  ctx: Context
): List<CallableRef> = ctx.cached("package_injectables", packageFqName) {
    if (packageFqName !in packagesWithInjectables(ctx)) return@cached emptyList()

    val injectables = mutableListOf<CallableRef>()

    fun collectInjectables(scope: MemberScope) {
      scope.collectMemberInjectables(
        ctx = ctx,
        onEach = { declaration ->
          if (declaration is ClassDescriptor) {
            collectInjectables(declaration.unsubstitutedInnerClassesScope)

            if (declaration.kind == ClassKind.OBJECT && declaration.isProvide())
              injectables += declaration.injectableReceiver(true, ctx)
            else
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

private fun InjectablesScope.canSee(callable: CallableRef, ctx: Context): Boolean =
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
