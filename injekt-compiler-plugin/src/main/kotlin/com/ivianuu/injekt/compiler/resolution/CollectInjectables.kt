/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

@OptIn(ExperimentalStdlibApi::class)
fun KotlinType.collectInjectables(
  classBodyView: Boolean,
  ctx: Context
): List<CallableRef> = ctx.trace!!.getOrPut(InjektWritableSlices.TYPE_CHAINED_INJECTABLES, toTypeKey() to classBodyView) {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType) {
    val callable = constructor
      .declarationDescriptor!!
      .defaultType
      .memberScope
      .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
      .first()
      .toCallableRef(ctx)
      .let { callable ->
        callable.copy(
          type = arguments.last().type,
          parameterTypes = callable.parameterTypes.toMutableMap().apply {
            this[DISPATCH_RECEIVER_INDEX] = this@collectInjectables
          }
        ).substitute(
          TypeSubstitutor.create(
            constructor.parameters
              .map { it.typeConstructor }
              .zip(arguments)
              .toMap()
          )
        )
      }

    return@getOrPut listOf(callable)
  }

  // do not run any code for types which do not declare any injectables
  if (constructor.declarationDescriptor?.declaresInjectables(ctx) != true && !classBodyView)
    // at least include the companion object if it declares injectables
    return@getOrPut listOfNotNull(
      constructor.declarationDescriptor
        ?.safeAs<ClassDescriptor>()
        ?.companionObjectDescriptor
        ?.takeIf { it.declaresInjectables(ctx) }
        ?.injectableReceiver(ctx)
    )

  buildList {
    memberScope.collectInjectables(classBodyView = classBodyView, ctx = ctx) { callable ->
        add(
          if (callable.parameterTypes[DISPATCH_RECEIVER_INDEX]?.toTypeKey() != this@collectInjectables.toTypeKey()) callable.copy(
            parameterTypes = callable.parameterTypes.toMutableMap()
              .also { it[DISPATCH_RECEIVER_INDEX] = this@collectInjectables }
          ) else callable
        )
      }
  }
}

fun ResolutionScope.collectInjectables(
  classBodyView: Boolean,
  onEach: (DeclarationDescriptor) -> Unit = {},
  name: Name? = null,
  ctx: Context,
  includeNonProvideObjectsWithInjectables: Boolean = false,
  consumer: (CallableRef) -> Unit
) {
  for (declaration in getContributedDescriptors()) {
    onEach(declaration)
    if (name != null && declaration.name != name) continue

    when (declaration) {
      is ClassDescriptor -> {
        if (declaration.kind == ClassKind.OBJECT &&
          (!classBodyView || !declaration.isCompanionObject))
            declaration
              .takeIf {
                it.isProvide(ctx) ||
                    (includeNonProvideObjectsWithInjectables &&
                        it.declaresInjectables(ctx))
              }
              ?.injectableReceiver(ctx)
              ?.let(consumer)
        else {
          declaration.injectableConstructors(ctx).forEach(consumer)
          if (!classBodyView && !includeNonProvideObjectsWithInjectables)
            declaration.companionObjectDescriptor
              ?.takeIf { it.declaresInjectables(ctx) }
              ?.injectableReceiver(ctx)
              ?.let(consumer)
        }
      }
      is CallableMemberDescriptor -> {
        if (declaration.isProvide(ctx) &&
          (declaration !is PropertyDescriptor ||
              classBodyView ||
              declaration.hasAnnotation(InjektFqNames.Provide))) {
          consumer(declaration.toCallableRef(ctx))
        }
      }
      is VariableDescriptor -> {
        if (declaration.isProvide(ctx))
          consumer(declaration.toCallableRef(ctx))
      }
    }
  }
}

fun Annotated.isProvide(ctx: Context): Boolean {
  var isProvide = hasAnnotation(InjektFqNames.Provide)

  if (!isProvide && this is ClassConstructorDescriptor && isPrimary)
    isProvide = constructedClass.isProvide(ctx)

  if (!isProvide && this is ParameterDescriptor)
    isProvide = type.isProvide(ctx)

  return isProvide
}

fun ClassDescriptor.injectableConstructors(ctx: Context): List<CallableRef> =
  constructors
    .transform { constructor ->
      if (constructor.hasAnnotation(InjektFqNames.Provide) ||
        (constructor.isPrimary && hasAnnotation(InjektFqNames.Provide)))
        add(constructor.toCallableRef(ctx))
    }

fun ClassDescriptor.injectableReceiver(ctx: Context): CallableRef =
  ReceiverParameterDescriptorImpl(
    this, ImplicitClassReceiver(this),
    Annotations.EMPTY
  ).toCallableRef(ctx)

fun CallableRef.collectInjectables(
  scope: InjectablesScope,
  ctx: Context,
  chainLength: Int = 0,
  seen: MutableSet<InjectablesScope.InjectableKey> = mutableSetOf(),
  addInjectable: (CallableRef) -> Unit
) {
  if (!seen.add(InjectablesScope.InjectableKey(this, ctx))) return

  if (!scope.canSee(this, ctx) || !scope.injectablesPredicate(this)) return

  val nextCallable = if (type.isProvideFunctionType) {
    addInjectable(this)
    copy(type = type.withFrameworkKey(callable.uniqueKey(ctx), ctx))
  } else this
  addInjectable(nextCallable)

  nextCallable.type
    .collectInjectables(
      scope.allScopes.any {
        it.ownerDescriptor == nextCallable.type.constructor.declarationDescriptor
      },
      ctx
    )
    .forEach { innerCallable ->
      val nextChainLength = chainLength + 1
      innerCallable
        .copy(chainLength = nextChainLength)
        .collectInjectables(
          scope = scope,
          addInjectable = addInjectable,
          chainLength = nextChainLength,
          seen = seen,
          ctx = ctx
        )
    }
}

fun collectAllInjectables(ctx: Context): List<CallableRef> =
  memberScopeForFqName(InjektFqNames.IndicesPackage, NoLookupLocation.FROM_BACKEND, ctx)
    ?.first
    ?.getContributedFunctions("index".asNameId(), NoLookupLocation.FROM_BACKEND)
    ?.transform {
      val annotation = it.annotations.findAnnotation(InjektFqNames.Index)
        ?: return@transform
      val fqName = FqName(annotation.allValueArguments["fqName".asNameId()]!!.value.toString())
      for (injectable in injectablesForFqName(fqName, ctx)) {
        val dispatchReceiverType = injectable.parameterTypes[DISPATCH_RECEIVER_INDEX]
          .takeIf { injectable.callable !is ConstructorDescriptor }
        if (dispatchReceiverType == null)
          add(injectable)
      }
    }
    ?: emptyList()

private fun injectablesForFqName(
  fqName: FqName,
  ctx: Context
): List<CallableRef> =
  memberScopeForFqName(fqName.parent(), NoLookupLocation.FROM_BACKEND, ctx)
    ?.first
    ?.getContributedDescriptors(nameFilter = { it == fqName.shortName() })
    ?.transform { declaration ->
      when (declaration) {
        is ClassDescriptor -> addAll(declaration.injectableConstructors(ctx))
        is CallableDescriptor -> {
          if (declaration.isProvide(ctx))
            this += declaration.toCallableRef(ctx)
        }
      }
    }
    ?: emptyList()

private fun InjectablesScope.canSee(callable: CallableRef, ctx: Context): Boolean =
  callable.callable.visibility == DescriptorVisibilities.PUBLIC ||
      callable.callable.visibility == DescriptorVisibilities.LOCAL ||
      (callable.callable.visibility == DescriptorVisibilities.INTERNAL &&
          callable.callable.moduleName(ctx) == ctx.module.moduleName(ctx)) ||
      (callable.callable is ClassConstructorDescriptor &&
          callable.type.constructor.declarationDescriptor.safeAs<ClassDescriptor>()?.kind == ClassKind.OBJECT) ||
      callable.callable.parents.any { callableParent ->
        allScopes.any { it.ownerDescriptor == callableParent }
      } || (callable.callable.visibility == DescriptorVisibilities.PRIVATE &&
      callable.callable.containingDeclaration is PackageFragmentDescriptor &&
      run {
        val scopeFile = allScopes.firstNotNullOfOrNull { it.file }
        scopeFile == callable.callable.findPsi()?.containingFile
      })

private fun ClassifierDescriptor.declaresInjectables(ctx: Context): Boolean {
  if (this !is ClassDescriptor) return false
  return defaultType
    .memberScope
    .getContributedDescriptors()
    .any { it.isProvide(ctx) }
}

