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
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.getArgumentDescriptor
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.moduleName
import com.ivianuu.injekt.compiler.primaryConstructorPropertyValueParameter
import com.ivianuu.injekt.compiler.transform
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isNullableType
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.UUID

fun KotlinType.collectInjectables(
  classBodyView: Boolean,
  ctx: Context
): List<CallableRef> = ctx.cached("type_injectables", this to classBodyView) {
  // special case to support @Provide () -> Foo
  if (isProvideFunctionType(ctx)) {
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
          buildSubstitutor {
            putAll(
              constructor.parameters
                .map { it.typeConstructor }
                .zip(arguments.map { it.type.unwrap() })
                .toMap()
            )
          }
        )
      }

    return@cached listOf(callable)
  }

  buildList {
    constructor
      .declarationDescriptor
      ?.defaultType
      ?.memberScope
      ?.collectMemberInjectables(ctx) { callable ->
        val substituted = callable.substitute(
          buildSubstitutor {
            putAll(
              if (callable.callable.safeAs<CallableMemberDescriptor>()?.kind ==
                CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                val originalClassifier = callable.callable.cast<CallableMemberDescriptor>()
                  .overriddenTreeAsSequence(false)
                  .last()
                  .containingDeclaration
                  .cast<ClassDescriptor>()
                constructor.parameters
                  .map { it.typeConstructor }
                  .zip(arguments.map { it.type.unwrap() })
                  .toMap() +
                    originalClassifier.declaredTypeParameters
                      .map { it.typeConstructor }
                      .zip(subtypeView(originalClassifier)!!.arguments.map { it.type.unwrap() })
              } else constructor.parameters.map { it.typeConstructor }.zip(arguments.map { it.type.unwrap() }).toMap()
            )
          }
        )

        add(
          substituted.copy(
            parameterTypes = substituted.parameterTypes.toMutableMap()
              .also { it[DISPATCH_RECEIVER_INDEX] = this@collectInjectables }
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
    if ((declaration is CallableMemberDescriptor || declaration is VariableDescriptor) &&
      declaration.isProvide(ctx))
      consumer(declaration.cast<CallableDescriptor>().toCallableRef(ctx))
  }
}

fun Annotated.isProvide(ctx: Context): Boolean =
  hasAnnotationForInjection(InjektFqNames.Provide, ctx) || isInject(ctx)

fun Annotated.isInject(ctx: Context): Boolean =
  hasAnnotationForInjection(InjektFqNames.Inject, ctx)

private fun Annotated.hasAnnotationForInjection(fqName: FqName, ctx: Context): Boolean =
  ctx.cached(
    "annotation_for_injection_$fqName",
    if (this is KotlinType) System.identityHashCode(this) else this
  ) {
    hasAnnotation(fqName) ||
        (this is ParameterDescriptor && type.hasAnnotation(fqName)) ||
        (this is PropertyDescriptor &&
            primaryConstructorPropertyValueParameter(ctx)?.hasAnnotationForInjection(fqName, ctx) == true) ||
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
              ?.singleOrNull { it.injektIndex(ctx) == injektIndex(ctx) }
              ?.hasAnnotationForInjection(fqName, ctx) == true)
  }

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
  val classifierInfo = classifierInfo(ctx)
  return if (!tagged || classifierInfo.tags.isEmpty()) callable
  else {
    val taggedType = classifierInfo.tags.wrap(callable.type)
    callable.copy(type = taggedType, originalType = taggedType)
  }
}

fun CallableRef.collectInjectables(
  scope: InjectablesScope,
  addInjectable: (CallableRef) -> Unit,
  addSpreadingInjectable: (CallableRef) -> Unit,
  ctx: Context
) {
  if (!scope.canSee(this, ctx) || !scope.allScopes.all { it.injectablesPredicate(this) }) return

  if (typeParameters.any { it.hasAnnotation(InjektFqNames.Spread) && typeArguments[it] == it.defaultType }) {
    addSpreadingInjectable(this)
    return
  }

  if (type.isUnconstrained(scope.allStaticTypeParameters)) return

  val nextCallable = copy(type = type.withFrameworkKey(UUID.randomUUID().toString()))
  addInjectable(nextCallable)

  nextCallable
    .type
    .collectInjectables(
      nextCallable.type.constructor.declarationDescriptor?.parentsWithSelf
        ?.mapNotNull { it.findPsi() }
        ?.any { callableParent -> scope.allScopes.any { it.owner == callableParent } } == true,
      ctx
    )
    .forEach { innerCallable ->
      innerCallable
        .copy(
          callableFqName = nextCallable.callableFqName.child(innerCallable.callableFqName.shortName()),
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
          ctx = ctx
        )
    }
}

fun collectGlobalInjectables(ctx: Context): List<CallableRef> = packagesWithInjectables(ctx)
  .flatMap { collectPackageInjectables(it, ctx) }

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

            if (declaration.kind == ClassKind.OBJECT && declaration.isProvide(ctx))
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
          callable.type.unwrapTags().constructor.declarationDescriptor.safeAs<ClassDescriptor>()?.kind == ClassKind.OBJECT) ||
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
