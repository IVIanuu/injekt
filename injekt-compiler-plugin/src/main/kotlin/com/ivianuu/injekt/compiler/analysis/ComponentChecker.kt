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

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.resolution.CallContext
import com.ivianuu.injekt.compiler.resolution.callContext
import com.ivianuu.injekt.compiler.resolution.collectComponentCallables
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.injekt_shaded.Inject
import com.ivianuu.injekt_shaded.Provide
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

class ComponentChecker(@Inject private val baseCtx: Context) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    @Provide val ctx = baseCtx.withTrace(context.trace)
    when (descriptor) {
      is CallableDescriptor -> {
        if (descriptor.hasAnnotation(injektFqNames().scoped) &&
          descriptor.isProvide() &&
          descriptor.callContext() != CallContext.DEFAULT) {
          trace()!!.report(
            InjektErrors.SCOPED_WITHOUT_DEFAULT_CALL_CONTEXT
              .on(declaration)
          )
        }
      }
      is ClassDescriptor -> {
        if (descriptor.hasAnnotation(injektFqNames().component)) {
          if (descriptor.modality != Modality.ABSTRACT)
            trace()!!.report(InjektErrors.NON_ABSTRACT_COMPONENT.on(declaration))

          descriptor.checkComponentCallables(InjektErrors.ENTRY_POINT_MEMBER_VAR)
        } else if (descriptor.hasAnnotation(injektFqNames().entryPoint)) {
          if (descriptor.kind != ClassKind.INTERFACE)
            trace()!!.report(InjektErrors.ENTRY_POINT_WITHOUT_INTERFACE.on(declaration))

          descriptor.checkComponentCallables(InjektErrors.ENTRY_POINT_MEMBER_VAR)
        }
      }
    }
  }

  private fun ClassDescriptor.checkComponentCallables(
    factory: DiagnosticFactory0<PsiElement>,
    @Inject ctx: Context
  ) {
    defaultType.toTypeRef()
      .collectComponentCallables()
      .map { it.callable }
      .forEach {
        if (it is PropertyDescriptor && it.isVar) {
          trace()!!.report(factory.on(it.findPsi() ?: findPsi()!!))
        }
      }
  }
}
