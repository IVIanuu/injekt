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
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class ReaderChecker(
    private val typeAnnotationChecker: TypeAnnotationChecker
) : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is FunctionDescriptor || !typeAnnotationChecker.hasTypeAnnotation(
                context.trace,
                descriptor,
                InjektFqNames.Reader
            )
        ) return

        val parentMemberScope = (descriptor.containingDeclaration as? ClassDescriptor)
            ?.unsubstitutedMemberScope
            ?: (descriptor.containingDeclaration as? PackageFragmentDescriptor)
                ?.getMemberScope()

        if ((parentMemberScope?.getContributedDescriptors()
                ?.filterIsInstance<FunctionDescriptor>()
                ?.filter { it.name == descriptor.name }
                ?.size ?: 0) > 1
        ) {
            context.trace.report(
                InjektErrors.MULTIPLE_DECLARATIONS_WITH_SAME_NAME
                    .on(declaration)
            )
        }

        if (descriptor.modality != Modality.FINAL) {
            context.trace.report(
                InjektErrors.MUST_BE_FINAL
                    .on(declaration)
            )
        }

        if (descriptor.isTailrec) {
            context.trace.report(
                InjektErrors.CANNOT_HAVE_TAILREC_MODIFIER
                    .on(declaration)
            )
        }
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val resulting = resolvedCall.resultingDescriptor

        if (resulting.fqNameSafe.asString() == "com.ivianuu.injekt.composition.reader") {
            val receiver = resolvedCall.extensionReceiver!!.type
            if (receiver.constructor.declarationDescriptor?.annotations?.hasAnnotation(InjektFqNames.CompositionComponent) != true &&
                !receiver.isTypeParameter()
            ) {
                context.trace.report(
                    InjektErrors.NOT_A_COMPOSITION_COMPONENT
                        .on(reportOn)
                )
            }
        }

        if (resulting !is FunctionDescriptor || !typeAnnotationChecker.hasTypeAnnotation(
                context.trace,
                resulting,
                InjektFqNames.Reader
            )
        ) return
        checkInvocations(reportOn, context)
    }

    private fun checkInvocations(
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val enclosingReaderFunction = findEnclosingFunctionContext(context) {
            typeAnnotationChecker.hasTypeAnnotation(context.trace, it, InjektFqNames.Reader)
        }

        if (enclosingReaderFunction == null) {
            context.trace.report(
                InjektErrors.FORBIDDEN_READER_INVOCATION.on(reportOn)
            )
        }
    }
}
