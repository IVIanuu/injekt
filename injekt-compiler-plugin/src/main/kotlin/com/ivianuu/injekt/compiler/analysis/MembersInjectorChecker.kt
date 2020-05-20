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

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class MembersInjectorChecker : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        when (descriptor) {
            is PropertyDescriptor -> checkProperty(declaration, descriptor, context)
            is FunctionDescriptor -> checkFunction(declaration, descriptor, context)
        }
    }

    private fun checkProperty(
        declaration: KtDeclaration,
        descriptor: PropertyDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (declaration !is KtProperty) return
        val delegateCall = declaration.delegateExpression
            ?.getResolvedCall(context.trace.bindingContext) ?: return
        if (delegateCall.resultingDescriptor.fqNameSafe.asString() != "com.ivianuu.injekt.inject") return

        val dispatchReceiverClass = descriptor.dispatchReceiverParameter
            ?.type?.constructor?.declarationDescriptor as? ClassDescriptor

        if (dispatchReceiverClass == null ||
            dispatchReceiverClass.kind != ClassKind.CLASS
        ) {
            context.trace.report(
                InjektErrors.INJECT_PROPERTY_PARENT_MUST_BE_CLASS
                    .on(declaration)
            )
        }

        if (descriptor.modality != Modality.FINAL) {
            context.trace.report(
                InjektErrors.INJECT_PROPERTY_MUST_BE_FINAL
                    .on(declaration)
            )
        }

        if (descriptor.extensionReceiverParameter != null) {
            context.trace.report(
                InjektErrors.INJECT_PROPERTY_CANNOT_BE_EXTENSION
                    .on(declaration)
            )
        }
    }

    private fun checkFunction(
        declaration: KtDeclaration,
        descriptor: FunctionDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.annotations.hasAnnotation(InjektFqNames.Inject)) return

        if (descriptor.returnType != descriptor.builtIns.unitType) {
            context.trace.report(
                InjektErrors.RETURN_TYPE_NOT_ALLOWED_FOR_INJECT
                    .on(declaration)
            )
        }

        val dispatchReceiverClass = descriptor.dispatchReceiverParameter
            ?.type?.constructor?.declarationDescriptor as? ClassDescriptor

        if (dispatchReceiverClass == null ||
            dispatchReceiverClass.kind != ClassKind.CLASS
        ) {
            context.trace.report(
                InjektErrors.INJECT_FUNCTION_PARENT_MUST_BE_CLASS
                    .on(declaration)
            )
        }

        if (descriptor.typeParameters.isNotEmpty()) {
            context.trace.report(
                InjektErrors.INJECT_FUNCTION_CANNOT_HAVE_TYPE_PARAMETERS
                    .on(declaration)
            )
        }

        if (descriptor.modality == Modality.ABSTRACT) {
            context.trace.report(
                InjektErrors.INJECT_FUNCTION_CANNOT_BE_ABSTRACT
                    .on(declaration)
            )
        }
    }
}
