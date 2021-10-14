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

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.findAnnotation
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektFqNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

class TagChecker(@Inject private val context: InjektContext) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    @Provide val injektContext = this.context.withTrace(context.trace)

    if (descriptor.hasAnnotation(injektFqNames().tag) && descriptor is ClassDescriptor) {
      if (descriptor.unsubstitutedPrimaryConstructor?.valueParameters?.isNotEmpty() == true) {
        context.trace.report(
          InjektErrors.TAG_WITH_VALUE_PARAMETERS
            .on(declaration)
        )
      }
    } else {
      val tags = descriptor.getAnnotatedAnnotations(injektFqNames().tag)
      if (tags.isNotEmpty() && descriptor !is ClassDescriptor &&
        descriptor !is ConstructorDescriptor
      ) {
        context.trace.report(
          InjektErrors.TAG_ON_NON_CLASS_AND_NON_TYPE
            .on(
              declaration.findAnnotation(tags.first().fqName!!)
                ?: declaration
            )
        )
      }
    }
  }
}
