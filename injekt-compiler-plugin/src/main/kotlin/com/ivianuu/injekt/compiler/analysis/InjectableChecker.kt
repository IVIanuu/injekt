/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.shaded_injekt.*
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
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjectableChecker(@Inject private val baseCtx: Context) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext,
  ) {
    @Provide val ctx = baseCtx.withTrace(context.trace)
    trace()!!.record(InjektWritableSlices.INJEKT_CONTEXT, Unit, ctx)
    when (descriptor) {
      is SimpleFunctionDescriptor -> checkFunction(declaration, descriptor)
      is ConstructorDescriptor -> checkConstructor(declaration, descriptor)
      is ClassDescriptor -> checkClass(declaration, descriptor)
      is LocalVariableDescriptor -> checkLocalVariable(declaration, descriptor)
      is PropertyDescriptor -> checkProperty(declaration, descriptor)
    }
  }

  private fun checkFunction(
    declaration: KtDeclaration,
    descriptor: FunctionDescriptor,
    @Inject ctx: Context
  ) {
    if (descriptor.isProvide()) {
      descriptor.valueParameters
        .checkProvideCallableDoesNotHaveInjectMarkedParameters(declaration)
      checkSpreadingInjectable(declaration, descriptor.typeParameters)
    } else {
      checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters)
    }
    descriptor.valueParameters
      .checkHasNotMoreThanOneInjectAnnotatedParameter(declaration)
    checkOverrides(declaration, descriptor)
    checkExceptActual(declaration, descriptor)
    checkReceiver(descriptor, declaration)
  }

  private fun checkClass(
    declaration: KtDeclaration,
    descriptor: ClassDescriptor,
    @Inject ctx: Context
  ) {
    val provideConstructors = descriptor.injectableConstructors()
    val isProvider = provideConstructors.isNotEmpty() ||
        descriptor.hasAnnotation(injektFqNames().provide)

    if (isProvider && descriptor.kind == ClassKind.ANNOTATION_CLASS) {
      trace()!!.report(
        InjektErrors.PROVIDE_ANNOTATION_CLASS
          .on(
            declaration.findAnnotation(injektFqNames().provide)
              ?: declaration
          )
      )
    }

    if (isProvider && descriptor.kind == ClassKind.ENUM_CLASS) {
      trace()!!.report(
        InjektErrors.PROVIDE_ENUM_CLASS
          .on(
            declaration.findAnnotation(injektFqNames().provide)
              ?: declaration
          )
      )
    }

    if (isProvider && descriptor.isInner) {
      trace()!!.report(
        InjektErrors.PROVIDE_INNER_CLASS
          .on(
            declaration.modifierList
              ?.getModifier(KtTokens.INNER_KEYWORD)
              ?: declaration
          )
      )
    }

    if (descriptor.kind == ClassKind.INTERFACE &&
      descriptor.hasAnnotation(injektFqNames().provide)) {
      trace()!!.report(
        InjektErrors.PROVIDE_INTERFACE
          .on(
            declaration.findAnnotation(injektFqNames().provide)
              ?: declaration
          )
      )
    }

    if (isProvider && descriptor.modality == Modality.ABSTRACT) {
      trace()!!.report(
        InjektErrors.PROVIDE_ABSTRACT_CLASS
          .on(
            declaration.modalityModifier()
              ?: declaration
          )
      )
    }

    if (descriptor.hasAnnotation(injektFqNames().provide) &&
      descriptor.unsubstitutedPrimaryConstructor
        ?.hasAnnotation(injektFqNames().provide) == true
    ) {
      trace()!!.report(
        InjektErrors.PROVIDE_ON_CLASS_WITH_PRIMARY_PROVIDE_CONSTRUCTOR
          .on(
            declaration.findAnnotation(injektFqNames().provide)
              ?: declaration
          )
      )
    }

    if (isProvider) {
      checkSpreadingInjectable(declaration, descriptor.declaredTypeParameters)
    } else {
      checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.declaredTypeParameters)
    }

    checkExceptActual(declaration, descriptor)
  }

  private fun checkConstructor(
    declaration: KtDeclaration,
    descriptor: ConstructorDescriptor,
    @Inject ctx: Context
  ) {
    if (descriptor.isProvide()) {
      descriptor.valueParameters
        .checkProvideCallableDoesNotHaveInjectMarkedParameters(declaration)
    }

    checkExceptActual(declaration, descriptor)

    descriptor.valueParameters
      .checkHasNotMoreThanOneInjectAnnotatedParameter(declaration)
  }

  private fun checkProperty(
    declaration: KtDeclaration,
    descriptor: PropertyDescriptor,
    @Inject ctx: Context
  ) {
    checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters)
    checkReceiver(descriptor, declaration)
    checkOverrides(declaration, descriptor)
    checkExceptActual(declaration, descriptor)
  }

  private fun checkLocalVariable(
    declaration: KtDeclaration,
    descriptor: LocalVariableDescriptor,
    @Inject ctx: Context
  ) {
    if (descriptor.isProvide() &&
      !descriptor.isDelegated &&
      !descriptor.isLateInit &&
      descriptor.findPsi().safeAs<KtProperty>()?.initializer == null) {
      trace()!!.report(InjektErrors.PROVIDE_VARIABLE_MUST_BE_INITIALIZED.on(declaration))
    }
  }

  private fun checkReceiver(
    descriptor: CallableDescriptor,
    declaration: KtDeclaration,
    @Inject ctx: Context
  ) {
    if (descriptor.extensionReceiverParameter?.hasAnnotation(injektFqNames().provide) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(injektFqNames().provide) == true) {
      trace()!!.report(
        InjektErrors.PROVIDE_RECEIVER
          .on(
            declaration.safeAs<KtFunction>()?.receiverTypeReference
              ?: declaration.safeAs<KtProperty>()?.receiverTypeReference
              ?: declaration
          )
      )
    }

    if (descriptor.extensionReceiverParameter?.hasAnnotation(injektFqNames().inject) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(injektFqNames().inject) == true) {
      trace()!!.report(
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
    @Inject ctx: Context
  ) {
    val spreadParameters = typeParameters.filter {
      it.classifierInfo().isSpread
    }
    if (spreadParameters.size > 1) {
      spreadParameters
        .drop(1)
        .forEach {
          trace()!!.report(
            InjektErrors.MULTIPLE_SPREADS
              .on(
                it.findPsi()
                  ?.safeAs<KtAnnotated>()
                  ?.findAnnotation(injektFqNames().spread)
                  ?: it.findPsi()
                  ?: declaration
              )
          )
        }
    }
  }

  private fun checkOverrides(
    declaration: KtDeclaration,
    descriptor: CallableMemberDescriptor,
    @Inject ctx: Context
  ) {
    descriptor.overriddenTreeAsSequence(false)
      .drop(1)
      .filterNot { isValidOverride(descriptor, it) }
      .forEach {
        trace()!!.report(
          Errors.NOTHING_TO_OVERRIDE
            .on(declaration, descriptor)
        )
      }
  }

  private fun checkExceptActual(
    declaration: KtDeclaration,
    descriptor: MemberDescriptor,
    @Inject ctx: Context
  ) {
    if (!descriptor.isActual) return
    descriptor.findExpects()
      .filterNot { isValidOverride(descriptor, it) }
      .forEach {
        trace()!!.report(
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
    overriddenDescriptor: MemberDescriptor,
    @Inject ctx: Context
  ): Boolean {
    if (overriddenDescriptor.hasAnnotation(injektFqNames().provide) && !descriptor.isProvide()) {
      return false
    }

    if (descriptor is CallableMemberDescriptor) {
      for ((index, overriddenValueParameter) in
      overriddenDescriptor.cast<CallableMemberDescriptor>().valueParameters.withIndex()) {
        val valueParameter = descriptor.valueParameters[index]
        if (overriddenValueParameter.hasAnnotation(injektFqNames().inject) !=
          valueParameter.hasAnnotation(injektFqNames().inject)) {
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
      if (typeParameter.classifierInfo().isSpread !=
        overriddenTypeParameter.classifierInfo().isSpread) {
        return false
      }
    }

    return true
  }

  private fun checkSpreadingTypeParametersOnNonProvideDeclaration(
    typeParameters: List<TypeParameterDescriptor>,
    @Inject ctx: Context
  ) {
    if (typeParameters.isEmpty()) return
    for (typeParameter in typeParameters) {
      if (typeParameter.classifierInfo().isSpread)
        trace()!!.report(
          InjektErrors.SPREAD_ON_NON_PROVIDE_DECLARATION
            .on(
              typeParameter.findPsi()
                ?.safeAs<KtAnnotated>()
                ?.findAnnotation(injektFqNames().spread)
                ?: typeParameter.findPsi()!!
            )
        )
    }
  }

  private fun List<ParameterDescriptor>.checkProvideCallableDoesNotHaveInjectMarkedParameters(
    declaration: KtDeclaration,
    @Inject ctx: Context
  ) {
    if (isEmpty()) return
    for (parameter in this) {
      if (parameter.hasAnnotation(injektFqNames().inject)) {
        trace()!!.report(
          InjektErrors.INJECT_PARAMETER_ON_PROVIDE_DECLARATION
            .on(
              parameter.findPsi()
                ?.safeAs<KtAnnotated>()
                ?.findAnnotation(injektFqNames().inject)
                ?: parameter.findPsi()
                ?: declaration
            )
        )
      }
      if (parameter.hasAnnotation(injektFqNames().provide) &&
        parameter.findPsi().safeAs<KtParameter>()?.hasValOrVar() != true
      ) {
        trace()!!.report(
          InjektErrors.PROVIDE_PARAMETER_ON_PROVIDE_DECLARATION
            .on(
              parameter.findPsi()
                ?.safeAs<KtAnnotated>()
                ?.findAnnotation(injektFqNames().provide)
                ?: parameter.findPsi()
                ?: declaration
            )
        )
      }
    }
  }

  private fun List<ParameterDescriptor>.checkHasNotMoreThanOneInjectAnnotatedParameter(
    declaration: KtDeclaration,
    @Inject ctx: Context
  ) {
    if (isEmpty()) return
    this
      .filter { it.hasAnnotation(injektFqNames().inject) }
      .drop(1)
      .forEach {
        trace()!!.report(
          InjektErrors.MULTIPLE_INJECT_PARAMETERS
            .on(
              it.findPsi()
                ?.safeAs<KtAnnotated>()
                ?.findAnnotation(injektFqNames().inject)
                ?: it.findPsi()
                ?: declaration
            )
        )
      }
  }
}
