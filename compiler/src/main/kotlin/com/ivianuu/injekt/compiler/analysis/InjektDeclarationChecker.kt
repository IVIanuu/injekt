/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.multiplatform.*
import org.jetbrains.kotlin.resolve.source.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjektDeclarationChecker(private val baseCtx: Context) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext,
  ) {
    val ctx = baseCtx.withTrace(context.trace)
    when (descriptor) {
      is SimpleFunctionDescriptor -> checkFunction(declaration, descriptor, ctx)
      is ConstructorDescriptor -> checkConstructor(declaration, descriptor, ctx)
      is ClassDescriptor -> checkClass(declaration, descriptor, ctx)
      is LocalVariableDescriptor -> checkLocalVariable(declaration, descriptor, ctx)
      is PropertyDescriptor -> checkProperty(declaration, descriptor, ctx)
    }
  }

  private fun checkFunction(
    declaration: KtDeclaration,
    descriptor: FunctionDescriptor,
    ctx: Context
  ) {
    if (descriptor.hasAnnotation(InjektFqNames.Provide))
      checkSpreadingInjectable(declaration, descriptor.typeParameters, ctx)
    checkOverrides(declaration, descriptor, ctx)
    checkExceptActual(declaration, descriptor, ctx)
    checkReceiver(descriptor, declaration, ctx)
  }

  private fun checkClass(
    declaration: KtDeclaration,
    descriptor: ClassDescriptor,
    ctx: Context
  ) {
    val provideConstructors = descriptor.injectableConstructors(ctx)
    val isProvider = provideConstructors.isNotEmpty() ||
        descriptor.hasAnnotation(InjektFqNames.Provide)

    if (isProvider && descriptor.kind == ClassKind.ANNOTATION_CLASS)
      ctx.reportError(
        descriptor.annotations.findAnnotation(InjektFqNames.Provide)
          ?.source?.getPsi() ?: declaration,
        "annotation class cannot be injectable"
      )

    if (isProvider && descriptor.kind == ClassKind.ENUM_CLASS)
      ctx.reportError(
        descriptor.annotations.findAnnotation(InjektFqNames.Provide)
          ?.source?.getPsi() ?: declaration,
        "enum class cannot be injectable"
      )

    if (isProvider && descriptor.isInner)
      ctx.reportError(
        declaration.modifierList
          ?.getModifier(KtTokens.INNER_KEYWORD)
          ?: declaration,
        "inner class cannot be injectable"
      )

    if (descriptor.kind == ClassKind.INTERFACE &&
      descriptor.hasAnnotation(InjektFqNames.Provide))
      ctx.reportError(
        descriptor.annotations.findAnnotation(InjektFqNames.Provide)
          ?.source?.getPsi() ?: declaration,
        "interface cannot be injectable"
      )

    if (isProvider && descriptor.modality == Modality.ABSTRACT)
      ctx.reportError(
        declaration.modalityModifier()
          ?: declaration,
        "abstract class cannot be injectable"
      )

    if (descriptor.hasAnnotation(InjektFqNames.Provide) &&
      descriptor.unsubstitutedPrimaryConstructor?.hasAnnotation(InjektFqNames.Provide) == true
    )
      ctx.reportError(
        descriptor.annotations.findAnnotation(InjektFqNames.Provide)
          ?.source?.getPsi() ?: declaration,
        "class cannot be marked with @Provide if it has a @Provide primary constructor"
      )

    if (isProvider)
      checkSpreadingInjectable(declaration, descriptor.declaredTypeParameters, ctx)

    checkExceptActual(declaration, descriptor, ctx)

    if (descriptor.hasAnnotation(InjektFqNames.Tag) &&
      descriptor.unsubstitutedPrimaryConstructor?.valueParameters?.isNotEmpty() == true)
      ctx.reportError(
        descriptor.annotations.findAnnotation(InjektFqNames.Tag)
          ?.source?.getPsi() ?: declaration,
        "tag cannot have value parameters"
      )
  }

  private fun checkConstructor(
    declaration: KtDeclaration,
    descriptor: ConstructorDescriptor,
    ctx: Context
  ) {
    checkExceptActual(declaration, descriptor, ctx)
  }

  private fun checkProperty(
    declaration: KtDeclaration,
    descriptor: PropertyDescriptor,
    ctx: Context
  ) {
    checkReceiver(descriptor, declaration, ctx)
    checkOverrides(declaration, descriptor, ctx)
    checkExceptActual(declaration, descriptor, ctx)
  }

  private fun checkLocalVariable(
    declaration: KtDeclaration,
    descriptor: LocalVariableDescriptor,
    ctx: Context
  ) {
    if (descriptor.hasAnnotation(InjektFqNames.Provide) &&
      !descriptor.isDelegated &&
      !descriptor.isLateInit &&
      descriptor.findPsi().safeAs<KtProperty>()?.initializer == null)
      ctx.reportError(declaration, "injectable variable must be initialized, delegated or marked with lateinit")
  }

  private fun checkReceiver(
    descriptor: CallableDescriptor,
    declaration: KtDeclaration,
    ctx: Context
  ) {
    if (descriptor.extensionReceiverParameter?.hasAnnotation(InjektFqNames.Inject) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(InjektFqNames.Inject) == true)
      ctx.reportError(
        declaration.safeAs<KtFunction>()?.receiverTypeReference
          ?: declaration.safeAs<KtProperty>()?.receiverTypeReference
          ?: declaration,
        "receiver cannot be injected"
      )
  }

  private fun checkSpreadingInjectable(
    declaration: KtDeclaration,
    typeParameters: List<TypeParameterDescriptor>,
    ctx: Context
  ) {
    val spreadParameters = typeParameters.filter { it.hasAnnotation(InjektFqNames.Spread) }
    if (spreadParameters.size > 1)
      spreadParameters
        .drop(1)
        .forEach {
          ctx.reportError(
            it.annotations.findAnnotation(InjektFqNames.Spread)
              ?.source?.getPsi() ?: declaration,
            "a declaration may have only one @Spread type parameter"
          )
        }
  }

  private fun checkOverrides(
    declaration: KtDeclaration,
    descriptor: CallableMemberDescriptor,
    ctx: Context
  ) {
    descriptor.overriddenTreeAsSequence(false)
      .drop(1)
      .filterNot { isValidOverride(descriptor, it) }
      .forEach {
        ctx.trace!!.report(
          Errors.NOTHING_TO_OVERRIDE
            .on(declaration, descriptor)
        )
      }
  }

  private fun checkExceptActual(
    declaration: KtDeclaration,
    descriptor: MemberDescriptor,
    ctx: Context
  ) {
    if (!descriptor.isActual) return
    descriptor.findExpects()
      .filterNot { isValidOverride(descriptor, it) }
      .forEach {
        ctx.trace!!.report(
          Errors.ACTUAL_WITHOUT_EXPECT
            .on(
              declaration.cast(),
              descriptor,
              emptyMap()
            )
        )
      }
  }

  private fun isValidOverride(
    descriptor: MemberDescriptor,
    overriddenDescriptor: MemberDescriptor
  ): Boolean {
    if (overriddenDescriptor.hasAnnotation(InjektFqNames.Provide) &&
      !descriptor.hasAnnotation(InjektFqNames.Provide))
      return false

    if (descriptor is CallableMemberDescriptor)
      for ((index, overriddenValueParameter) in
      overriddenDescriptor.cast<CallableMemberDescriptor>().valueParameters.withIndex()) {
      val valueParameter = descriptor.valueParameters[index]
      if (overriddenValueParameter.hasAnnotation(InjektFqNames.Inject) !=
        valueParameter.hasAnnotation(InjektFqNames.Inject))
        return false
    }

    val (typeParameters, overriddenTypeParameters) = when (descriptor) {
      is CallableMemberDescriptor ->
        descriptor.typeParameters to overriddenDescriptor.cast<CallableMemberDescriptor>()
          .typeParameters
      is ClassifierDescriptorWithTypeParameters ->
        descriptor.declaredTypeParameters to overriddenDescriptor.cast<ClassifierDescriptorWithTypeParameters>()
          .declaredTypeParameters
      else -> emptyList<TypeParameterDescriptor>() to emptyList()
    }

    for ((index, overriddenTypeParameter) in overriddenTypeParameters.withIndex()) {
      val typeParameter = typeParameters[index]
      if (typeParameter.hasAnnotation(InjektFqNames.Spread) !=
        overriddenTypeParameter.hasAnnotation(InjektFqNames.Spread))
        return false
    }

    return true
  }
}
