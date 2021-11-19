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
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt.compiler.resolution.isSubTypeOf
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.isSealed
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
        if (ctx.componentClassifier != null) {
          val classifier = descriptor.toClassifierRef()

          if (classifier.defaultType.isSubTypeOf(ctx.componentClassifier!!.defaultType)) {
            if (descriptor.modality != Modality.ABSTRACT)
              trace()!!.report(InjektErrors.NON_ABSTRACT_COMPONENT.on(declaration))
          } else if (classifier.defaultType.isSubTypeOf(ctx.entryPointClassifier!!.defaultType)) {
            if (descriptor.kind != ClassKind.INTERFACE)
              trace()!!.report(InjektErrors.ENTRY_POINT_WITHOUT_INTERFACE.on(declaration))
          }
        }
      }
    }
  }
}
