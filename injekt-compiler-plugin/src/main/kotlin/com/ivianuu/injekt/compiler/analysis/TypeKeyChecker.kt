/*
 * Copyright 2020 Manuel Wrage
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
import com.ivianuu.injekt.compiler.forEachWith
import com.ivianuu.injekt.compiler.isForTypeKey
import com.ivianuu.injekt.compiler.resolution.isGiven
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.cast

class TypeKeyChecker(private val context: InjektContext) : CallChecker, DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor is TypeAliasDescriptor) {
            descriptor.declaredTypeParameters
                .filter { it.isForTypeKey(this.context, context.trace) }
                .forEach {
                    context.trace.report(
                        InjektErrors.FOR_TYPE_KEY_TYPE_PARAMETER_ON_TYPE_ALIAS
                            .on(it.findPsi()!!)
                    )
                }
        }

        if (descriptor !is CallableDescriptor) return

        if (descriptor.typeParameters.isEmpty()) return

        descriptor.overriddenDescriptors
            .filter { overriddenDescriptor ->
                var hasDifferentTypeParameters = false
                descriptor.typeParameters.forEachWith(overriddenDescriptor.typeParameters) { a, b ->
                    hasDifferentTypeParameters = hasDifferentTypeParameters || a.isForTypeKey(this.context, context.trace) !=
                            b.isForTypeKey(this.context, context.trace)
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
            .filterKeys { it.isForTypeKey(this.context, context.trace) }
            .forEach { it.value.checkAllForTypeKey(reportOn, context.trace) }
    }

    private fun KotlinType.checkAllForTypeKey(
        reportOn: PsiElement,
        trace: BindingTrace
    ) {
        if (constructor.declarationDescriptor is TypeParameterDescriptor &&
            !constructor.declarationDescriptor.cast<TypeParameterDescriptor>()
                .isForTypeKey(context, trace)) {
            trace.report(
                InjektErrors.NON_FOR_TYPE_KEY_TYPE_PARAMETER_AS_FOR_TYPE_KEY
                    .on(reportOn, constructor.declarationDescriptor as TypeParameterDescriptor)
            )
        }

        arguments.forEach { it.type.checkAllForTypeKey(reportOn, trace) }
    }
}
