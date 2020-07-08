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
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.utils.addToStdlib.cast

class ReaderChecker(
    private val typeAnnotationChecker: TypeAnnotationChecker
) : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        when (descriptor) {
            is ClassDescriptor -> checkClass(declaration, descriptor, context)
            is FunctionDescriptor -> checkFunction(declaration, descriptor, context)
        }
    }

    private fun checkClass(
        declaration: KtDeclaration,
        descriptor: ClassDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.hasAnnotation(InjektFqNames.Reader) &&
            descriptor.constructors.none { it.hasAnnotation(InjektFqNames.Reader) }
        ) return

        if (descriptor.kind == ClassKind.INTERFACE) {
            context.trace.report(
                InjektErrors.READER_CLASS_CANNOT_BE_INTERFACE
                    .on(declaration)
            )
        }

        if (descriptor.kind == ClassKind.OBJECT) {
            context.trace.report(
                InjektErrors.READER_CLASS_CANNOT_BE_OBJECT
                    .on(declaration)
            )
        }
    }

    private fun checkFunction(
        declaration: KtDeclaration,
        descriptor: FunctionDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!typeAnnotationChecker.hasTypeAnnotation(
                context.trace,
                descriptor,
                InjektFqNames.Reader
            )
        ) return

        if (descriptor.modality != Modality.FINAL) {
            context.trace.report(
                InjektErrors.READER_MUST_BE_FINAL
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

        if (resulting.fqNameSafe.asString() == "com.ivianuu.injekt.runReader") {
            val receiver = resolvedCall.extensionReceiver!!.type
            if (receiver.constructor.declarationDescriptor?.annotations
                    ?.hasAnnotation(InjektFqNames.Component) != true &&
                !receiver.isTypeParameter()
            ) {
                context.trace.report(
                    InjektErrors.NOT_A_COMPONENT
                        .on(reportOn)
                )
            }
        }

        if (resulting !is FunctionDescriptor) return

        if (typeAnnotationChecker.hasTypeAnnotation(
                context.trace,
                resulting,
                InjektFqNames.Reader
            )
        ) {
            checkInvocations(reportOn, context)
        }

        if (resulting is ConstructorDescriptor &&
            (resulting.constructedClass.hasAnnotation(InjektFqNames.Reader))
        ) {
            checkInvocations(reportOn, context)
        }
    }

    private fun checkInvocations(
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val enclosingReaderContext = findEnclosingContext(context) {
            typeAnnotationChecker.hasTypeAnnotation(context.trace, it, InjektFqNames.Reader) ||
                    (it is ClassDescriptor && it.hasAnnotation(InjektFqNames.Reader)) ||
                    (it is ConstructorDescriptor &&
                            it.constructedClass.hasAnnotation(InjektFqNames.Reader) ||
                            (it is ClassDescriptor && it.constructors.any {
                                it.hasAnnotation(InjektFqNames.Reader)
                            }))
        }

        if (enclosingReaderContext == null) {
            context.trace.report(
                InjektErrors.FORBIDDEN_READER_INVOCATION.on(reportOn)
            )
        }
    }

    private inline fun findEnclosingContext(
        context: CallCheckerContext,
        predicate: (DeclarationDescriptor) -> Boolean
    ): DeclarationDescriptor? = context.scope
        .parentsWithSelf.firstOrNull {
            it is LexicalScope && predicate(it.ownerDescriptor)
        }?.cast<LexicalScope>()?.ownerDescriptor

}
