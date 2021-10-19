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
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.resolution.collectComponentCallables
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt_shaded.Inject
import com.ivianuu.injekt_shaded.Provide
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence

class ComponentChecker(@Inject private val context: InjektContext) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    when (descriptor) {
      is ClassDescriptor -> {
        if (descriptor.hasAnnotation(injektFqNames().component)) {
          @Provide val injektContext = this.context.withTrace(context.trace)

          if (descriptor.kind != ClassKind.INTERFACE)
            context.trace.report(InjektErrors.COMPONENT_WITHOUT_INTERFACE.on(declaration))

          descriptor.defaultType.toTypeRef()
            .collectComponentCallables()
            .map { it.callable }
            .forEach {
              if (it is PropertyDescriptor && it.isVar) {
                context.trace.report(
                  InjektErrors.MUTABLE_COMPONENT_PROPERTY
                    .on(
                      if (it.overriddenTreeUniqueAsSequence(false).count() > 1) declaration
                      else it.findPsi() ?: declaration
                    )
                )
              }
            }
        }
      }
      is FunctionDescriptor -> {
        if (descriptor.hasAnnotation(injektFqNames().entryPoint)) {
          if (declaration !is KtFunction) return
          @Provide val injektContext = this.context.withTrace(context.trace)

          if (declaration.hasBody())
            context.trace.report(InjektErrors.ENTRY_POINT_WITH_BODY.on(declaration))

          if (declaration.receiverTypeReference == null)
            context.trace.report(InjektErrors.ENTRY_POINT_WITHOUT_RECEIVER.on(declaration))
        }
      }
      is PropertyDescriptor -> {
        if (descriptor.hasAnnotation(injektFqNames().entryPoint)) {
          if (declaration !is KtProperty) return
          @Provide val injektContext = this.context.withTrace(context.trace)

          if (declaration.getter?.hasBody() == true)
            context.trace.report(InjektErrors.ENTRY_POINT_WITH_BODY.on(declaration))

          if (declaration.receiverTypeReference == null)
            context.trace.report(InjektErrors.ENTRY_POINT_WITHOUT_RECEIVER.on(declaration))

          if (declaration.isVar)
            context.trace.report(InjektErrors.MUTABLE_COMPONENT_PROPERTY.on(declaration))
        }
      }
    }
  }
}
