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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*

class InfoAnnotationPatcher(private val context: InjektContext) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    // requesting infos triggers saving them
    when (descriptor) {
      is ClassDescriptor -> {
        descriptor.classifierInfo(this.context, context.trace)

        descriptor.declaredTypeParameters
          .forEach { it.classifierInfo(this.context, context.trace) }

        descriptor.constructors
          .forEach { it.callableInfo(this.context, context.trace) }

        descriptor.unsubstitutedMemberScope
          .getContributedDescriptors()
          .filterIsInstance<FunctionDescriptor>()
          .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
          .forEach { callable ->
            val info = callable.callableInfo(this.context, context.trace)
            if (info != CallableInfo.Empty) {
              context.trace.record(InjektWritableSlices.WAS_FAKE_OVERRIDE, callable, Unit)
              callable.updatePrivateFinalField<CallableMemberDescriptor.Kind>(
                FunctionDescriptorImpl::class,
                "kind"
              ) { CallableMemberDescriptor.Kind.DECLARATION }
            }
          }
      }
      is CallableDescriptor -> {
        descriptor.callableInfo(this.context, context.trace)

        descriptor.typeParameters
          .forEach { it.classifierInfo(this.context, context.trace) }
      }
    }
  }
}
