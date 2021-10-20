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
import com.ivianuu.injekt.compiler.resolution.CallContext
import com.ivianuu.injekt.compiler.resolution.callContext
import com.ivianuu.injekt.compiler.resolution.collectComponentCallables
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt_shaded.Inject
import com.ivianuu.injekt_shaded.Provide
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
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
      is CallableDescriptor -> {
        @Provide val injektContext = this.context.withTrace(context.trace)
        if (descriptor.returnType?.hasAnnotation(injektFqNames().scoped) == true &&
          descriptor.isProvide() &&
          descriptor.callContext() != CallContext.DEFAULT) {
          context.trace.report(
            InjektErrors.SCOPED_WITHOUT_DEFAULT_CALL_CONTEXT
              .on(declaration)
          )
        }
      }
      is ClassDescriptor -> {
        @Provide val injektContext = this.context.withTrace(context.trace)

        if (descriptor.hasAnnotation(injektFqNames().component)) {
          if (descriptor.kind != ClassKind.INTERFACE)
            context.trace.report(InjektErrors.COMPONENT_WITHOUT_INTERFACE.on(declaration))

          descriptor.defaultType.toTypeRef()
            .collectComponentCallables()
            .map { it.callable }
            .forEach {
              if (it is PropertyDescriptor && it.isVar) {
                context.trace.report(
                  InjektErrors.COMPONENT_MEMBER_VAR
                    .on(
                      if (it.overriddenTreeUniqueAsSequence(false).count() > 1) declaration
                      else it.findPsi() ?: declaration
                    )
                )
              }
            }
        } else if (descriptor.hasAnnotation(injektFqNames().entryPoint)) {
          if (descriptor.kind != ClassKind.INTERFACE)
            context.trace.report(InjektErrors.ENTRY_POINT_WITHOUT_INTERFACE.on(declaration))

          descriptor.defaultType.toTypeRef()
            .collectComponentCallables()
            .map { it.callable }
            .forEach {
              if (it is PropertyDescriptor && it.isVar) {
                context.trace.report(
                  InjektErrors.ENTRY_POINT_MEMBER_VAR
                    .on(
                      if (it.overriddenTreeUniqueAsSequence(false).count() > 1) declaration
                      else it.findPsi() ?: declaration
                    )
                )
              }
            }
        }
      }
    }
  }
}
