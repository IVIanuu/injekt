/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
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
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
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

context(Context) class InjectableChecker : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext,
  ) = with(withTrace(context.trace)) {
    when (descriptor) {
      is SimpleFunctionDescriptor -> checkFunction(declaration, descriptor)
      is ConstructorDescriptor -> checkConstructor(declaration, descriptor)
      is ClassDescriptor -> checkClass(declaration, descriptor)
      is LocalVariableDescriptor -> checkLocalVariable(declaration, descriptor)
      is PropertyDescriptor -> checkProperty(declaration, descriptor)
    }
  }

  context(Context) private fun checkFunction(
    declaration: KtDeclaration,
    descriptor: FunctionDescriptor
  ) {
    if (descriptor.hasAnnotation(InjektFqNames.Provide)) {
      descriptor.valueParameters
        .checkProvideCallableDoesNotHaveInjectMarkedParameters(declaration)
      checkSpreadingInjectable(declaration, descriptor.typeParameters)
    } else {
      checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters)
    }
    checkOverrides(declaration, descriptor)
    checkExceptActual(declaration, descriptor)
    checkReceiver(descriptor, declaration)
  }

  context(Context) private fun checkClass(declaration: KtDeclaration, descriptor: ClassDescriptor) {
    val provideConstructors = descriptor.injectableConstructors()
    val isProvider = provideConstructors.isNotEmpty() ||
        descriptor.hasAnnotation(InjektFqNames.Provide)

    if (isProvider && descriptor.kind == ClassKind.ANNOTATION_CLASS) {
      reportError(
        descriptor.annotations.findAnnotation(InjektFqNames.Provide)
          ?.source?.getPsi() ?: declaration,
        "annotation class cannot be injectable"
      )
    }

    if (isProvider && descriptor.kind == ClassKind.ENUM_CLASS) {
      reportError(
        descriptor.annotations.findAnnotation(InjektFqNames.Provide)
          ?.source?.getPsi() ?: declaration,
        "enum class cannot be injectable"
      )
    }

    if (isProvider && descriptor.isInner) {
      reportError(
        declaration.modifierList
          ?.getModifier(KtTokens.INNER_KEYWORD)
          ?: declaration,
        "inner class cannot be injectable"
      )
    }

    if (descriptor.kind == ClassKind.INTERFACE &&
      descriptor.hasAnnotation(InjektFqNames.Provide)) {
      reportError(
        descriptor.annotations.findAnnotation(InjektFqNames.Provide)
          ?.source?.getPsi() ?: declaration,
        "interface cannot be injectable"
      )
    }

    if (isProvider && descriptor.modality == Modality.ABSTRACT) {
      reportError(
        declaration.modalityModifier()
          ?: declaration,
        "abstract class cannot be injectable"
      )
    }

    if (descriptor.hasAnnotation(InjektFqNames.Provide) &&
      descriptor.unsubstitutedPrimaryConstructor?.hasAnnotation(InjektFqNames.Provide) == true
    ) {
      reportError(
        descriptor.annotations.findAnnotation(InjektFqNames.Provide)
          ?.source?.getPsi() ?: declaration,
        "class cannot be marked with @Provide if it has a @Provide primary constructor"
      )
    }

    if (isProvider) {
      checkSpreadingInjectable(declaration, descriptor.declaredTypeParameters)
    } else {
      checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.declaredTypeParameters)
    }

    checkExceptActual(declaration, descriptor)
  }

  context(Context) private fun checkConstructor(
    declaration: KtDeclaration,
    descriptor: ConstructorDescriptor
  ) {
    if (descriptor.hasAnnotation(InjektFqNames.Provide) ||
      (descriptor.isPrimary &&
          descriptor.constructedClass.hasAnnotation(InjektFqNames.Provide))) {
            descriptor.valueParameters
              .checkProvideCallableDoesNotHaveInjectMarkedParameters(declaration)
    }

    checkExceptActual(declaration, descriptor)
  }

  context(Context) private fun checkProperty(
    declaration: KtDeclaration,
    descriptor: PropertyDescriptor
  ) {
    checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters)
    checkReceiver(descriptor, declaration)
    checkOverrides(declaration, descriptor)
    checkExceptActual(declaration, descriptor)
  }

  context(Context) private fun checkLocalVariable(
    declaration: KtDeclaration,
    descriptor: LocalVariableDescriptor
  ) {
    if (descriptor.hasAnnotation(InjektFqNames.Provide) &&
      !descriptor.isDelegated &&
      !descriptor.isLateInit &&
      descriptor.findPsi().safeAs<KtProperty>()?.initializer == null) {
      reportError(declaration, "injectable variable must be initialized, delegated or marked with lateinit")
    }
  }

  context(Context) private fun checkReceiver(
    descriptor: CallableDescriptor,
    declaration: KtDeclaration
  ) {
    if (descriptor.extensionReceiverParameter?.hasAnnotation(InjektFqNames.Provide) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(InjektFqNames.Provide) == true)
      reportError(
        declaration.safeAs<KtFunction>()?.receiverTypeReference
          ?: declaration.safeAs<KtProperty>()?.receiverTypeReference
          ?: declaration,
        "receiver is automatically provided"
      )

    if (descriptor.extensionReceiverParameter?.hasAnnotation(InjektFqNames.Inject) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(InjektFqNames.Inject) == true)
      reportError(
        declaration.safeAs<KtFunction>()?.receiverTypeReference
          ?: declaration.safeAs<KtProperty>()?.receiverTypeReference
          ?: declaration,
        "receiver cannot be injected"
      )
  }

  context(Context) private fun checkSpreadingInjectable(
    declaration: KtDeclaration,
    typeParameters: List<TypeParameterDescriptor>
  ) {
    val spreadParameters = typeParameters.filter { it.hasAnnotation(InjektFqNames.Spread) }
    if (spreadParameters.size > 1) {
      spreadParameters
        .drop(1)
        .forEach {
          reportError(
            it.annotations.findAnnotation(InjektFqNames.Spread)
              ?.source?.getPsi() ?: declaration,
            "a declaration may have only one @Spread type parameter"
          )
        }
    }
  }

  context(Context) private fun checkOverrides(
    declaration: KtDeclaration,
    descriptor: CallableMemberDescriptor
  ) {
    descriptor.overriddenTreeAsSequence(false)
      .drop(1)
      .filterNot { isValidOverride(descriptor, it) }
      .forEach {
        trace!!.report(
          Errors.NOTHING_TO_OVERRIDE
            .on(declaration, descriptor)
        )
      }
  }

  context(Context) private fun checkExceptActual(
    declaration: KtDeclaration,
    descriptor: MemberDescriptor
  ) {
    if (!descriptor.isActual) return
    descriptor.findExpects()
      .filterNot { isValidOverride(descriptor, it) }
      .forEach {
        trace!!.report(
          Errors.ACTUAL_WITHOUT_EXPECT
            .on(
              declaration.cast(),
              descriptor,
              emptyMap()
            )
        )
      }
  }

  context(Context) private fun isValidOverride(
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

  context(Context) private fun checkSpreadingTypeParametersOnNonProvideDeclaration(
    typeParameters: List<TypeParameterDescriptor>
  ) {
    if (typeParameters.isEmpty()) return
    for (typeParameter in typeParameters) {
      if (typeParameter.hasAnnotation(InjektFqNames.Spread))
        reportError(
          typeParameter.annotations.findAnnotation(InjektFqNames.Spread)
            ?.source?.getPsi() ?: typeParameter.findPsi()!!,
          "a @Spread type parameter is only supported on @Provide functions and @Provide classes"
        )
    }
  }

  context(Context)
  private fun List<ParameterDescriptor>.checkProvideCallableDoesNotHaveInjectMarkedParameters(
    declaration: KtDeclaration
  ) {
    if (isEmpty()) return
    for (parameter in this) {
      if (parameter.hasAnnotation(InjektFqNames.Inject)) {
        reportError(
          parameter.annotations.findAnnotation(InjektFqNames.Inject)
            ?.source?.getPsi() ?: declaration,
          "parameters of a injectable are automatically treated as inject parameters"
        )
      }
      if (parameter.hasAnnotation(InjektFqNames.Provide) &&
        parameter.findPsi().safeAs<KtParameter>()?.hasValOrVar() != true
      ) {
        reportError(
          parameter.annotations.findAnnotation(InjektFqNames.Provide)
            ?.source?.getPsi() ?: declaration,
          "parameters of a injectable are automatically provided"
        )
      }
    }
  }
}
