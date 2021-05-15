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
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class TypeKeyChecker(private val context: InjektContext) : CallChecker, DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    if (descriptor !is CallableDescriptor) return

    if (descriptor.typeParameters.isEmpty()) return

    descriptor.overriddenDescriptors
      .filter { overriddenDescriptor ->
        var hasDifferentTypeParameters = false
        descriptor.typeParameters.forEachWith(overriddenDescriptor.typeParameters) { a, b ->
          hasDifferentTypeParameters = hasDifferentTypeParameters ||
              a.classifierInfo(this.context, context.trace).isForTypeKey !=
              b.classifierInfo(this.context, context.trace).isForTypeKey
        }
        hasDifferentTypeParameters
      }
      .toList()
      .takeIf { it.isNotEmpty() }
      ?.let {
        context.trace.report(
          Errors.CONFLICTING_OVERLOADS
            .on(declaration, it)
        )
      }
  }

  override fun check(
    resolvedCall: ResolvedCall<*>,
    reportOn: PsiElement,
    context: CallCheckerContext
  ) {
    resolvedCall
      .typeArguments
      .filterKeys {
        it.classifierInfo(this.context, context.trace)
          .isForTypeKey
      }
      .forEach { it.value.checkAllForTypeKey(reportOn, context.trace) }
  }

  private fun KotlinType.checkAllForTypeKey(reportOn: PsiElement, trace: BindingTrace) {
    if (constructor.declarationDescriptor is TypeParameterDescriptor &&
      !constructor.declarationDescriptor.cast<TypeParameterDescriptor>()
        .classifierInfo(context, trace)
        .isForTypeKey
    ) {
      trace.report(
        InjektErrors.NON_FOR_TYPE_KEY_TYPE_PARAMETER_AS_FOR_TYPE_KEY
          .on(reportOn, constructor.declarationDescriptor as TypeParameterDescriptor)
      )
    }

    arguments.forEach { it.type.checkAllForTypeKey(reportOn, trace) }
  }
}
