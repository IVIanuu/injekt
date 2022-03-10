/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

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

class InjectableChecker(private val baseCtx: Context) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext,
  ) {
    val ctx = baseCtx.withTrace(context.trace)
    ctx.trace!!.record(InjektWritableSlices.INJEKT_CONTEXT, Unit, ctx)
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
    if (descriptor.hasAnnotation(InjektFqNames.Provide)) {
      if (descriptor.isSuspend)
        ctx.trace!!.report(
          InjektErrors.PROVIDE_SUSPEND_FUNCTION
            .on(
              descriptor.annotations.findAnnotation(InjektFqNames.Provide)
                ?.source?.getPsi() ?: declaration
            )
        )
      if (descriptor.hasAnnotation(InjektFqNames.Composable))
        ctx.trace!!.report(
          InjektErrors.PROVIDE_COMPOSABLE_FUNCTION
            .on(
              descriptor.annotations.findAnnotation(InjektFqNames.Provide)
                ?.source?.getPsi() ?: declaration
            )
        )

      descriptor.valueParameters
        .checkProvideCallableDoesNotHaveInjectMarkedParameters(declaration, ctx)
      checkSpreadingInjectable(declaration, descriptor.typeParameters, ctx)
    } else {
      checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters, ctx)
    }
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

    if (isProvider && descriptor.kind == ClassKind.ANNOTATION_CLASS) {
      ctx.trace!!.report(
        InjektErrors.PROVIDE_ANNOTATION_CLASS
          .on(
            descriptor.annotations.findAnnotation(InjektFqNames.Provide)
              ?.source?.getPsi() ?: declaration
          )
      )
    }

    if (isProvider && descriptor.kind == ClassKind.ENUM_CLASS) {
      ctx.trace!!.report(
        InjektErrors.PROVIDE_ENUM_CLASS
          .on(
            descriptor.annotations.findAnnotation(InjektFqNames.Provide)
              ?.source?.getPsi() ?: declaration
          )
      )
    }

    if (isProvider && descriptor.isInner) {
      ctx.trace!!.report(
        InjektErrors.PROVIDE_INNER_CLASS
          .on(
            declaration.modifierList
              ?.getModifier(KtTokens.INNER_KEYWORD)
              ?: declaration
          )
      )
    }

    if (descriptor.kind == ClassKind.INTERFACE &&
      descriptor.hasAnnotation(InjektFqNames.Provide)) {
      ctx.trace!!.report(
        InjektErrors.PROVIDE_INTERFACE
          .on(
            descriptor.annotations.findAnnotation(InjektFqNames.Provide)
              ?.source?.getPsi() ?: declaration
          )
      )
    }

    if (isProvider && descriptor.modality == Modality.ABSTRACT) {
      ctx.trace!!.report(
        InjektErrors.PROVIDE_ABSTRACT_CLASS
          .on(
            declaration.modalityModifier()
              ?: declaration
          )
      )
    }

    if (descriptor.hasAnnotation(InjektFqNames.Provide) &&
      descriptor.unsubstitutedPrimaryConstructor
        ?.hasAnnotation(InjektFqNames.Provide) == true
    ) {
      ctx.trace!!.report(
        InjektErrors.PROVIDE_ON_CLASS_WITH_PRIMARY_PROVIDE_CONSTRUCTOR
          .on(
            descriptor.annotations.findAnnotation(InjektFqNames.Provide)
              ?.source?.getPsi() ?: declaration
          )
      )
    }

    if (isProvider) {
      checkSpreadingInjectable(declaration, descriptor.declaredTypeParameters, ctx)
    } else {
      checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.declaredTypeParameters, ctx)
    }

    checkExceptActual(declaration, descriptor, ctx)
  }

  private fun checkConstructor(
    declaration: KtDeclaration,
    descriptor: ConstructorDescriptor,
    ctx: Context
  ) {
    if (descriptor.hasAnnotation(InjektFqNames.Provide) ||
      (descriptor.isPrimary &&
          descriptor.constructedClass.hasAnnotation(InjektFqNames.Provide))) {
            descriptor.valueParameters
              .checkProvideCallableDoesNotHaveInjectMarkedParameters(declaration, ctx)
    }

    checkExceptActual(declaration, descriptor, ctx)
  }

  private fun checkProperty(
    declaration: KtDeclaration,
    descriptor: PropertyDescriptor,
    ctx: Context
  ) {
    if (descriptor.hasAnnotation(InjektFqNames.Provide) &&
      descriptor.getter?.hasAnnotation(InjektFqNames.Composable) == true)
      ctx.trace!!.report(
        InjektErrors.PROVIDE_COMPOSABLE_PROPERTY
          .on(
            descriptor.getter?.annotations?.findAnnotation(InjektFqNames.Provide)
              ?.source?.getPsi() ?: declaration
          )
      )

    checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters, ctx)
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
      descriptor.findPsi().safeAs<KtProperty>()?.initializer == null) {
      ctx.trace!!.report(InjektErrors.PROVIDE_VARIABLE_MUST_BE_INITIALIZED.on(declaration))
    }
  }

  private fun checkReceiver(
    descriptor: CallableDescriptor,
    declaration: KtDeclaration,
    ctx: Context
  ) {
    if (descriptor.extensionReceiverParameter?.hasAnnotation(InjektFqNames.Provide) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(InjektFqNames.Provide) == true) {
      ctx.trace!!.report(
        InjektErrors.PROVIDE_RECEIVER
          .on(
            declaration.safeAs<KtFunction>()?.receiverTypeReference
              ?: declaration.safeAs<KtProperty>()?.receiverTypeReference
              ?: declaration
          )
      )
    }

    if (descriptor.extensionReceiverParameter?.hasAnnotation(InjektFqNames.Inject) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(InjektFqNames.Inject) == true) {
      ctx.trace!!.report(
        InjektErrors.INJECT_RECEIVER
          .on(
            declaration.safeAs<KtFunction>()?.receiverTypeReference
              ?: declaration.safeAs<KtProperty>()?.receiverTypeReference
              ?: declaration
          )
      )
    }
  }

  private fun checkSpreadingInjectable(
    declaration: KtDeclaration,
    typeParameters: List<TypeParameterDescriptor>,
    ctx: Context
  ) {
    val spreadParameters = typeParameters.filter { it.hasAnnotation(InjektFqNames.Spread) }
    if (spreadParameters.size > 1) {
      spreadParameters
        .drop(1)
        .forEach {
          ctx.trace!!.report(
            InjektErrors.MULTIPLE_SPREADS
              .on(
                it.annotations.findAnnotation(InjektFqNames.Spread)
                  ?.source?.getPsi() ?: declaration
              )
          )
        }
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
      !descriptor.hasAnnotation(InjektFqNames.Provide)) {
        return false
    }

    if (descriptor is CallableMemberDescriptor) {
      for ((index, overriddenValueParameter) in
      overriddenDescriptor.cast<CallableMemberDescriptor>().valueParameters.withIndex()) {
        val valueParameter = descriptor.valueParameters[index]
        if (overriddenValueParameter.hasAnnotation(InjektFqNames.Inject) !=
          valueParameter.hasAnnotation(InjektFqNames.Inject)) {
          return false
        }
      }
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
        overriddenTypeParameter.hasAnnotation(InjektFqNames.Spread)) {
        return false
      }
    }

    return true
  }

  private fun checkSpreadingTypeParametersOnNonProvideDeclaration(
    typeParameters: List<TypeParameterDescriptor>,
    ctx: Context
  ) {
    if (typeParameters.isEmpty()) return
    for (typeParameter in typeParameters) {
      if (typeParameter.hasAnnotation(InjektFqNames.Spread))
        ctx.trace!!.report(
          InjektErrors.SPREAD_ON_NON_PROVIDE_DECLARATION
            .on(
              typeParameter.annotations.findAnnotation(InjektFqNames.Spread)
                ?.source?.getPsi() ?: typeParameter.findPsi()!!
            )
        )
    }
  }

  private fun List<ParameterDescriptor>.checkProvideCallableDoesNotHaveInjectMarkedParameters(
    declaration: KtDeclaration,
    ctx: Context
  ) {
    if (isEmpty()) return
    for (parameter in this) {
      if (parameter.hasAnnotation(InjektFqNames.Inject)) {
        ctx.trace!!.report(
          InjektErrors.INJECT_PARAMETER_ON_PROVIDE_DECLARATION
            .on(
              parameter.annotations.findAnnotation(InjektFqNames.Inject)
                ?.source?.getPsi() ?: declaration
            )
        )
      }
      if (parameter.hasAnnotation(InjektFqNames.Provide) &&
        parameter.findPsi().safeAs<KtParameter>()?.hasValOrVar() != true
      ) {
        ctx.trace!!.report(
          InjektErrors.PROVIDE_PARAMETER_ON_PROVIDE_DECLARATION
            .on(
              parameter.annotations.findAnnotation(InjektFqNames.Provide)
                ?.source?.getPsi() ?: declaration
            )
        )
      }
    }
  }
}
