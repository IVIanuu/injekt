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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.findAnnotation
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.resolution.injectableConstructors
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt_shaded.Inject
import com.ivianuu.injekt_shaded.Provide
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
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.resolve.multiplatform.findExpects
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InjectableChecker(@Inject private val context: InjektContext) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext,
  ) {
    @Provide val injektContext = this.context.withTrace(context.trace)
    when (descriptor) {
      is SimpleFunctionDescriptor -> checkFunction(declaration, descriptor)
      is ConstructorDescriptor -> checkConstructor(declaration, descriptor)
      is ClassDescriptor -> checkClass(declaration, descriptor)
      is LocalVariableDescriptor -> checkLocalVariable(declaration, descriptor)
      is PropertyDescriptor -> checkProperty(declaration, descriptor)
      is TypeAliasDescriptor -> checkTypeAlias(descriptor)
    }
  }

  private fun checkFunction(
    declaration: KtDeclaration,
    descriptor: FunctionDescriptor,
    @Inject context: InjektContext
  ) {
    if (descriptor.isProvide()) {
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

  private fun checkClass(
    declaration: KtDeclaration,
    descriptor: ClassDescriptor,
    @Inject context: InjektContext
  ) {
    val provideConstructors = descriptor.injectableConstructors()
    val isProvider = provideConstructors.isNotEmpty() ||
        descriptor.hasAnnotation(injektFqNames().provide)

    if (isProvider && descriptor.kind == ClassKind.ANNOTATION_CLASS) {
      context.trace!!.report(
        InjektErrors.PROVIDE_ANNOTATION_CLASS
          .on(
            declaration.findAnnotation(injektFqNames().provide)
              ?: declaration
          )
      )
    }

    if (isProvider && descriptor.kind == ClassKind.ENUM_CLASS) {
      context.trace!!.report(
        InjektErrors.PROVIDE_ENUM_CLASS
          .on(
            declaration.findAnnotation(injektFqNames().provide)
              ?: declaration
          )
      )
    }

    if (descriptor.kind == ClassKind.INTERFACE &&
      descriptor.hasAnnotation(injektFqNames().provide) &&
      !descriptor.hasAnnotation(injektFqNames().component)) {
      context.trace!!.report(
        InjektErrors.PROVIDE_INTERFACE
          .on(
            declaration.findAnnotation(injektFqNames().provide)
              ?: declaration
          )
      )
    }

    if (isProvider && descriptor.modality == Modality.ABSTRACT &&
        !descriptor.hasAnnotation(injektFqNames().component)) {
      context.trace!!.report(
        InjektErrors.PROVIDE_ABSTRACT_CLASS
          .on(
            declaration.modalityModifier()
              ?: declaration
          )
      )
    }

    if (isProvider && descriptor.isInner) {
      context.trace!!.report(
        InjektErrors.PROVIDE_INNER_CLASS
          .on(
            declaration.modifierList
              ?.getModifier(KtTokens.INNER_KEYWORD)
              ?: declaration
          )
      )
    }

    if (descriptor.hasAnnotation(injektFqNames().provide) &&
      descriptor.unsubstitutedPrimaryConstructor
        ?.hasAnnotation(injektFqNames().provide) == true
    ) {
      context.trace!!.report(
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
    @Inject context: InjektContext
  ) {
    if (descriptor.isProvide()) {
      descriptor.valueParameters
        .checkProvideCallableDoesNotHaveInjectMarkedParameters(declaration)
    } else {
      checkExceptActual(declaration, descriptor)
    }
  }

  private fun checkProperty(
    declaration: KtDeclaration,
    descriptor: PropertyDescriptor,
    @Inject context: InjektContext
  ) {
    checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.typeParameters)
    checkReceiver(descriptor, declaration)
    checkOverrides(declaration, descriptor)
    checkExceptActual(declaration, descriptor)
  }

  private fun checkLocalVariable(
    declaration: KtDeclaration,
    descriptor: LocalVariableDescriptor,
    @Inject context: InjektContext
  ) {
    if (descriptor.isProvide() &&
      !descriptor.isDelegated &&
      !descriptor.isLateInit &&
      descriptor.findPsi().safeAs<KtProperty>()?.initializer == null) {
      context.trace!!.report(InjektErrors.PROVIDE_VARIABLE_MUST_BE_INITIALIZED
        .on(declaration))
    }
  }

  private fun checkTypeAlias(descriptor: TypeAliasDescriptor, @Inject context: InjektContext) {
    checkSpreadingTypeParametersOnNonProvideDeclaration(descriptor.declaredTypeParameters)
  }

  private fun checkReceiver(
    descriptor: CallableDescriptor,
    declaration: KtDeclaration,
    @Inject context: InjektContext
  ) {
    if (descriptor.extensionReceiverParameter?.hasAnnotation(injektFqNames().provide) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(injektFqNames().provide) == true) {
      context.trace!!.report(
        InjektErrors.PROVIDE_RECEIVER
          .on(
            declaration.safeAs<KtProperty>()
              ?.receiverTypeReference ?: declaration
          )
      )
    }

    if (descriptor.extensionReceiverParameter?.hasAnnotation(injektFqNames().inject) == true ||
      descriptor.extensionReceiverParameter?.type?.hasAnnotation(injektFqNames().inject) == true) {
      context.trace!!.report(
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
    @Inject context: InjektContext
  ) {
    val spreadParameters = typeParameters.filter {
      it.classifierInfo().isSpread
    }
    if (spreadParameters.size > 1) {
      spreadParameters
        .drop(1)
        .forEach {
          context.trace!!.report(
            InjektErrors.MULTIPLE_SPREADS
              .on(it.findPsi() ?: declaration)
          )
        }
    }
  }

  private fun checkOverrides(
    declaration: KtDeclaration,
    descriptor: CallableMemberDescriptor,
    @Inject context: InjektContext
  ) {
    descriptor.overriddenTreeAsSequence(false)
      .drop(1)
      .filterNot { isValidOverride(descriptor, it) }
      .forEach {
        context.trace!!.report(
          Errors.NOTHING_TO_OVERRIDE
            .on(declaration, descriptor)
        )
      }
  }

  private fun checkExceptActual(
    declaration: KtDeclaration,
    descriptor: MemberDescriptor,
    @Inject context: InjektContext
  ) {
    if (!descriptor.isActual) return
    descriptor.findExpects()
      .filterNot { isValidOverride(descriptor, it) }
      .forEach {
        context.trace!!.report(
          Errors.ACTUAL_WITHOUT_EXPECT
            .on(declaration.cast(), descriptor, emptyMap())
        )
      }
  }

  private fun isValidOverride(
    descriptor: MemberDescriptor,
    overriddenDescriptor: MemberDescriptor,
    @Inject context: InjektContext
  ): Boolean {
    if (overriddenDescriptor.hasAnnotation(injektFqNames().provide) && !descriptor.isProvide()) {
      return false
    }

    if (descriptor is CallableMemberDescriptor) {
      overriddenDescriptor.cast<CallableMemberDescriptor>().valueParameters
        .zip(descriptor.valueParameters)
        .forEach { (overriddenValueParameter, valueParameter) ->
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

    overriddenTypeParameters
      .zip(typeParameters)
      .forEach  { (overriddenTypeParameter, typeParameter) ->
        if (typeParameter.classifierInfo().isSpread !=
          overriddenTypeParameter.classifierInfo().isSpread) {
          return false
        }
      }

    return true
  }

  private fun checkSpreadingTypeParametersOnNonProvideDeclaration(
    typeParameters: List<TypeParameterDescriptor>,
    @Inject context: InjektContext
  ) {
    if (typeParameters.isEmpty()) return
    typeParameters
      .filter { it.classifierInfo().isSpread }
      .forEach { typeParameter ->
        context.trace!!.report(
          InjektErrors.SPREAD_ON_NON_PROVIDE_DECLARATION
            .on(typeParameter.findPsi()!!)
        )
      }
  }

  private fun List<ParameterDescriptor>.checkProvideCallableDoesNotHaveInjectMarkedParameters(
    declaration: KtDeclaration,
    @Inject context: InjektContext
  ) {
    if (isEmpty()) return
    this
      .forEach { parameter ->
        if (parameter.hasAnnotation(injektFqNames().inject)) {
          context.trace!!.report(
            InjektErrors.INJECT_PARAMETER_ON_PROVIDE_DECLARATION
              .on(parameter.findPsi() ?: declaration)
          )
        }
        if (parameter.hasAnnotation(injektFqNames().provide) &&
          parameter.findPsi().safeAs<KtParameter>()?.hasValOrVar() != true
        ) {
          context.trace!!.report(
            InjektErrors.PROVIDE_PARAMETER_ON_PROVIDE_DECLARATION
              .on(parameter.findPsi() ?: declaration)
          )
        }
      }
  }
}
