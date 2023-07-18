/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektClassIds
import com.ivianuu.injekt.compiler.getTags
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.reportError
import com.ivianuu.injekt.compiler.resolution.injectableConstructors
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.resolve.multiplatform.findExpects
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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
    if (descriptor.hasAnnotation(InjektClassIds.Provide))
      checkSpreadingInjectable(declaration, descriptor.typeParameters, ctx)
    else
      checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters, ctx)
    checkOverrides(declaration, descriptor, ctx)
    checkExceptActual(declaration, descriptor, ctx)
    checkReceiver(descriptor, declaration, ctx)
    checkTagUsage(descriptor, declaration, ctx)
  }

  private fun checkClass(
    declaration: KtDeclaration,
    descriptor: ClassDescriptor,
    ctx: Context
  ) {
    val provideConstructors = descriptor.injectableConstructors(ctx)
    val isProvider = provideConstructors.isNotEmpty() ||
        descriptor.hasAnnotation(InjektClassIds.Provide)

    if (isProvider && descriptor.kind == ClassKind.ANNOTATION_CLASS)
      ctx.reportError(
        descriptor.annotations.findAnnotation(InjektClassIds.Provide)
          ?.source?.getPsi() ?: declaration,
        "annotation class cannot be injectable"
      )

    if (isProvider && descriptor.kind == ClassKind.ENUM_CLASS)
      ctx.reportError(
        descriptor.annotations.findAnnotation(InjektClassIds.Provide)
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
      descriptor.hasAnnotation(InjektClassIds.Provide))
      ctx.reportError(
        descriptor.annotations.findAnnotation(InjektClassIds.Provide)
          ?.source?.getPsi() ?: declaration,
        "interface cannot be injectable"
      )

    if (isProvider && descriptor.modality == Modality.ABSTRACT)
      ctx.reportError(
        declaration.modalityModifier()
          ?: declaration,
        "abstract class cannot be injectable"
      )

    if (descriptor.hasAnnotation(InjektClassIds.Provide) &&
      descriptor.unsubstitutedPrimaryConstructor?.hasAnnotation(InjektClassIds.Provide) == true
    )
      ctx.reportError(
        descriptor.annotations.findAnnotation(InjektClassIds.Provide)
          ?.source?.getPsi() ?: declaration,
        "class cannot be marked with @Provide if it has a @Provide primary constructor"
      )

    if (isProvider)
      checkSpreadingInjectable(declaration, descriptor.declaredTypeParameters, ctx)
    else
      checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.declaredTypeParameters, ctx)

    checkExceptActual(declaration, descriptor, ctx)

    if (descriptor.hasAnnotation(InjektClassIds.Tag) &&
      descriptor.unsubstitutedPrimaryConstructor?.valueParameters?.isNotEmpty() == true)
      ctx.reportError(
        descriptor.annotations.findAnnotation(InjektClassIds.Tag)
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
    checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters, ctx)
    checkReceiver(descriptor, declaration, ctx)
    checkOverrides(declaration, descriptor, ctx)
    checkExceptActual(declaration, descriptor, ctx)
    checkTagUsage(descriptor, declaration, ctx)
  }

  private fun checkLocalVariable(
    declaration: KtDeclaration,
    descriptor: LocalVariableDescriptor,
    ctx: Context
  ) {
    if (descriptor.hasAnnotation(InjektClassIds.Provide) &&
      !descriptor.isDelegated &&
      !descriptor.isLateInit &&
      descriptor.findPsi().safeAs<KtProperty>()?.initializer == null)
      ctx.reportError(declaration, "injectable variable must be initialized, delegated or marked with lateinit")

    checkTagUsage(descriptor, declaration, ctx)
  }

  private fun checkReceiver(
    descriptor: CallableDescriptor,
    declaration: KtDeclaration,
    ctx: Context
  ) {
    if (descriptor.extensionReceiverParameter?.hasAnnotation(InjektClassIds.Inject) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(InjektClassIds.Inject) == true)
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
    val spreadParameters = typeParameters.filter { it.hasAnnotation(InjektClassIds.Spread) }
    if (spreadParameters.size > 1)
      spreadParameters
        .drop(1)
        .forEach {
          ctx.reportError(
            it.annotations.findAnnotation(InjektClassIds.Spread)
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
    if (overriddenDescriptor.hasAnnotation(InjektClassIds.Provide) &&
      !descriptor.hasAnnotation(InjektClassIds.Provide))
      return false

    if (descriptor is CallableMemberDescriptor)
      for ((index, overriddenValueParameter) in
      overriddenDescriptor.cast<CallableMemberDescriptor>().valueParameters.withIndex()) {
      val valueParameter = descriptor.valueParameters[index]
      if (overriddenValueParameter.hasAnnotation(InjektClassIds.Inject) !=
        valueParameter.hasAnnotation(InjektClassIds.Inject))
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
      if (typeParameter.hasAnnotation(InjektClassIds.Spread) !=
        overriddenTypeParameter.hasAnnotation(InjektClassIds.Spread))
        return false
    }

    return true
  }

  private fun checkSpreadingTypeParametersOnNonProvideDeclaration(
    typeParameters: List<TypeParameterDescriptor>,
    ctx: Context
  ) {
    if (typeParameters.isEmpty()) return
    for (typeParameter in typeParameters)
      if (typeParameter.hasAnnotation(InjektClassIds.Spread))
        ctx.reportError(
          typeParameter.annotations.findAnnotation(InjektClassIds.Spread)
            ?.source?.getPsi() ?: typeParameter.findPsi()!!,
          "a @Spread type parameter is only supported on @Provide functions and @Provide classes"
        )
  }

  private fun checkTagUsage(
    descriptor: DeclarationDescriptor,
    declaration: KtDeclaration,
    ctx: Context,
  ) {
    if (descriptor.getTags().any { it.fqName != InjektClassIds.Composable })
      ctx.reportError(declaration, "tags are only supported on classes, constructors and return types")
  }
}
