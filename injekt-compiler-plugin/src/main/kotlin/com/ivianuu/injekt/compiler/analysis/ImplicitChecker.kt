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
import com.ivianuu.injekt.compiler.isMarkedAsImplicit
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtCallExpression
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
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ImplicitChecker : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        when (descriptor) {
            is ClassDescriptor -> checkClass(declaration, descriptor, context)
            is FunctionDescriptor -> checkFunction(declaration, descriptor, context)
        }

        var implicitAnnotations = 0
        if (descriptor.hasAnnotation(InjektFqNames.Given)) implicitAnnotations += 1
        if (descriptor.hasAnnotation(InjektFqNames.Reader)) implicitAnnotations += 1
        if (descriptor.hasAnnotation(InjektFqNames.MapEntries)) implicitAnnotations += 1
        if (descriptor.hasAnnotation(InjektFqNames.SetElements)) implicitAnnotations += 1

        if (implicitAnnotations > 1) {
            context.trace.report(
                InjektErrors.MULTIPLE_IMPLICIT_ANNOTATIONS
                    .on(declaration)
            )
        }
    }

    private fun checkClass(
        declaration: KtDeclaration,
        descriptor: ClassDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.isMarkedAsImplicit() &&
            descriptor.constructors.none { it.isMarkedAsImplicit() }
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
        if (!descriptor.isMarkedAsImplicit() &&
            (descriptor !is ConstructorDescriptor ||
                    !descriptor.constructedClass.isMarkedAsImplicit())
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

        if (resulting.isMarkedAsImplicit()) {
            checkInvocations(reportOn, context)
        }

        if (resulting is ConstructorDescriptor &&
            (resulting.constructedClass.isMarkedAsImplicit())
        ) {
            checkInvocations(reportOn, context)
        }
    }

    private fun checkInvocations(
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val enclosingReaderContext = findEnclosingContext(context) {
            if (it is AnonymousFunctionDescriptor) {
                val name = it.findPsi()?.parent?.parent?.parent
                    ?.safeAs<KtCallExpression>()?.calleeExpression?.text
                if (name == "runReader") return@findEnclosingContext true
            }

            it.isMarkedAsImplicit() ||
                    (it is PropertyGetterDescriptor &&
                            it.correspondingProperty.isMarkedAsImplicit()) ||
                    (it is ConstructorDescriptor &&
                            it.constructedClass.isMarkedAsImplicit() ||
                            (it is ClassDescriptor && it.constructors.any {
                                it.isMarkedAsImplicit()
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
