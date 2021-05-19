/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.multiplatform.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjectableChecker(private val context: InjektContext) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext,
  ) {
    when (descriptor) {
      is SimpleFunctionDescriptor -> checkFunction(declaration, descriptor, context.trace)
      is ConstructorDescriptor -> checkConstructor(declaration, descriptor, context.trace)
      is ClassDescriptor -> checkClass(declaration, descriptor, context.trace)
      is PropertyDescriptor -> checkProperty(declaration, descriptor, context.trace)
      is TypeAliasDescriptor -> checkTypeAlias(descriptor, context.trace)
    }
  }

  private fun checkFunction(
    declaration: KtDeclaration,
    descriptor: FunctionDescriptor,
    trace: BindingTrace
  ) {
    if (descriptor.isProvide(this.context, trace)) {
      descriptor.valueParameters
        .checkProvideCallableDoesNotHaveInjectMarkedParameters(declaration, trace)
      checkSpreadingInjectable(declaration, descriptor.typeParameters, trace)
    } else {
      checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters, trace)
    }
    checkOverrides(declaration, descriptor, trace)
    checkExceptActual(declaration, descriptor, trace)
    checkReceiver(descriptor, declaration, trace)
  }

  private fun checkClass(
    declaration: KtDeclaration,
    descriptor: ClassDescriptor,
    trace: BindingTrace
  ) {
    val provideConstructors = descriptor.provideConstructors(context, trace)
    val isProvider = provideConstructors.isNotEmpty() ||
        descriptor.hasAnnotation(InjektFqNames.Provide)

    if (isProvider && descriptor.kind == ClassKind.ANNOTATION_CLASS) {
      trace.report(
        InjektErrors.PROVIDE_ANNOTATION_CLASS
          .on(
            declaration.findAnnotation(InjektFqNames.Provide)
              ?: declaration
          )
      )
    }

    if (isProvider && descriptor.kind == ClassKind.ENUM_CLASS) {
      trace.report(
        InjektErrors.PROVIDE_ENUM_CLASS
          .on(
            declaration.findAnnotation(InjektFqNames.Provide)
              ?: declaration
          )
      )
    }

    if (descriptor.kind == ClassKind.INTERFACE && descriptor.hasAnnotation(InjektFqNames.Provide)) {
      trace.report(
        InjektErrors.PROVIDE_INTERFACE
          .on(
            declaration.findAnnotation(InjektFqNames.Provide)
              ?: declaration
          )
      )
    }

    if (isProvider && descriptor.modality == Modality.ABSTRACT) {
      trace.report(
        InjektErrors.PROVIDE_ABSTRACT_CLASS
          .on(
            declaration.modalityModifier()
              ?: declaration
          )
      )
    }

    if (isProvider && descriptor.isInner) {
      trace.report(
        InjektErrors.PROVIDE_INNER_CLASS
          .on(
            declaration.modifierList
              ?.getModifier(KtTokens.INNER_KEYWORD)
              ?: declaration
          )
      )
    }

    if (descriptor.hasAnnotation(InjektFqNames.Provide) &&
      descriptor.unsubstitutedPrimaryConstructor
        ?.hasAnnotation(InjektFqNames.Provide) == true
    ) {
      trace.report(
        InjektErrors.PROVIDE_ON_CLASS_WITH_PRIMARY_PROVIDE_CONSTRUCTOR
          .on(
            declaration.findAnnotation(InjektFqNames.Provide)
              ?: declaration
          )
      )
    }

    if (isProvider) {
      checkSpreadingInjectable(declaration, descriptor.declaredTypeParameters, trace)
    } else {
      checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.declaredTypeParameters, trace)
    }

    checkExceptActual(declaration, descriptor, trace)
  }

  private fun checkConstructor(
    declaration: KtDeclaration,
    descriptor: ConstructorDescriptor,
    trace: BindingTrace
  ) {
    if (descriptor.isProvide(this.context, trace)) {
      descriptor.valueParameters
        .checkProvideCallableDoesNotHaveInjectMarkedParameters(declaration, trace)
    } else {
      checkExceptActual(declaration, descriptor, trace)
    }
  }

  private fun checkProperty(
    declaration: KtDeclaration,
    descriptor: PropertyDescriptor,
    trace: BindingTrace
  ) {
    checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters, trace)
    checkReceiver(descriptor, declaration, trace)
    checkOverrides(declaration, descriptor, trace)
    checkExceptActual(declaration, descriptor, trace)
  }

  private fun checkTypeAlias(
    descriptor: TypeAliasDescriptor,
    trace: BindingTrace
  ) {
    checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.declaredTypeParameters, trace)
  }

  private fun checkReceiver(
    descriptor: CallableDescriptor,
    declaration: KtDeclaration,
    trace: BindingTrace
  ) {
    if (descriptor.extensionReceiverParameter?.hasAnnotation(InjektFqNames.Provide) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(InjektFqNames.Provide) == true) {
      trace.report(
        InjektErrors.PROVIDE_RECEIVER
          .on(
            declaration.safeAs<KtProperty>()
              ?.receiverTypeReference ?: declaration
          )
      )
    }

    if (descriptor.extensionReceiverParameter?.hasAnnotation(InjektFqNames.Inject) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(InjektFqNames.Inject) == true) {
      trace.report(
        InjektErrors.INJECT_RECEIVER
          .on(
            declaration.safeAs<KtProperty>()
              ?.receiverTypeReference ?: declaration
          )
      )
    }
  }

  private fun checkSpreadingInjectable(
    declaration: KtDeclaration,
    typeParameters: List<TypeParameterDescriptor>,
    trace: BindingTrace
  ) {
    val spreadParameters = typeParameters.filter {
      it.classifierInfo(context, trace)
        .isSpread
    }
    if (spreadParameters.size > 1) {
      spreadParameters
        .drop(1)
        .forEach {
          trace.report(
            InjektErrors.MULTIPLE_SPREADS
              .on(it.findPsi() ?: declaration)
          )
        }
    }
  }

  private fun checkOverrides(
    declaration: KtDeclaration,
    descriptor: CallableMemberDescriptor,
    trace: BindingTrace
  ) {
    descriptor.overriddenTreeAsSequence(false)
      .drop(1)
      .filterNot { isValidOverride(descriptor, it, trace) }
      .forEach {
        trace.report(
          Errors.NOTHING_TO_OVERRIDE
            .on(declaration, descriptor)
        )
      }
  }

  private fun checkExceptActual(
    declaration: KtDeclaration,
    descriptor: MemberDescriptor,
    trace: BindingTrace
  ) {
    if (!descriptor.isActual) return
    descriptor.findExpects()
      .filterNot { isValidOverride(descriptor, it, trace) }
      .forEach {
        trace.report(
          Errors.ACTUAL_WITHOUT_EXPECT
            .on(declaration.cast(), descriptor, emptyMap())
        )
      }
  }

  private fun isValidOverride(
    descriptor: MemberDescriptor,
    overriddenDescriptor: MemberDescriptor,
    trace: BindingTrace
  ): Boolean {
    if (overriddenDescriptor.hasAnnotation(InjektFqNames.Provide) &&
      !descriptor.hasAnnotation(InjektFqNames.Provide)) {
      return false
    }

    if (descriptor is CallableMemberDescriptor) {
      overriddenDescriptor.cast<CallableMemberDescriptor>().valueParameters
        .forEachWith(descriptor.valueParameters) { overriddenValueParameter, valueParameter ->
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

    overriddenTypeParameters
      .forEachWith(typeParameters) { overriddenTypeParameter, typeParameter ->
        if (typeParameter.classifierInfo(context, trace).isSpread !=
          overriddenTypeParameter.classifierInfo(context, trace).isSpread) {
          return false
        }
      }

    return true
  }

  private fun checkSpreadingTypeParametersOnNonProvideDeclaration(
    typeParameters: List<TypeParameterDescriptor>,
    trace: BindingTrace
  ) {
    if (typeParameters.isEmpty()) return
    typeParameters
      .asSequence()
      .filter { it.classifierInfo(context, trace).isSpread }
      .forEach { typeParameter ->
        trace.report(
          InjektErrors.SPREAD_ON_NON_PROVIDE_DECLARATION
            .on(typeParameter.findPsi()!!)
        )
      }
  }

  private fun List<ParameterDescriptor>.checkProvideCallableDoesNotHaveInjectMarkedParameters(
    declaration: KtDeclaration,
    trace: BindingTrace,
  ) {
    if (isEmpty()) return
    this
      .asSequence()
      .filter { it.hasAnnotation(InjektFqNames.Inject) }
      .forEach {
        trace.report(
          InjektErrors.INJECT_PARAMETER_ON_PROVIDE_DECLARATION
            .on(it.findPsi() ?: declaration)
        )
      }
  }
}
