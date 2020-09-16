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
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.calls.callUtil.isCallableReference
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ReaderChecker : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        when (descriptor) {
            is ClassDescriptor -> checkClass(declaration, descriptor, context)
            is FunctionDescriptor -> checkFunction(declaration, descriptor, context)
            is PropertyDescriptor -> checkProperty(declaration, descriptor, context)
        }

        var readerAnnotations = 0
        if (descriptor.hasAnnotation(InjektFqNames.Given)) readerAnnotations += 1
        if (descriptor.hasAnnotation(InjektFqNames.Reader)) readerAnnotations += 1
        if (descriptor.hasAnnotation(InjektFqNames.GivenMapEntries)) readerAnnotations += 1
        if (descriptor.hasAnnotation(InjektFqNames.GivenSetElements)) readerAnnotations += 1

        if (readerAnnotations > 1 || (readerAnnotations == 1 &&
                    descriptor.hasAnnotation(InjektFqNames.Reader) &&
                    descriptor.getAnnotatedAnnotations(InjektFqNames.Effect, descriptor.module)
                        .isNotEmpty())
        ) {
            context.trace.report(
                InjektErrors.MULTIPLE_READER_ANNOTATIONS
                    .on(declaration)
            )
        }
    }

    private fun checkClass(
        declaration: KtDeclaration,
        descriptor: ClassDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.isReader()) return

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
        if (!descriptor.isReader()) return
    }

    private fun checkProperty(
        declaration: KtDeclaration,
        descriptor: PropertyDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (declaration !is KtProperty) return
        if (!descriptor.isReader()) return

        if (declaration.initializer != null) {
            context.trace.report(
                InjektErrors.READER_PROPERTY_WITH_BACKING_FIELD
                    .on(declaration)
            )
        }

        if (descriptor.isVar) {
            context.trace.report(
                InjektErrors.READER_PROPERTY_VAR
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

        if (resulting !is FunctionDescriptor) return

        if (resulting.isReader()) {
            checkCalls(reportOn, context, resolvedCall)
        }
    }

    private fun checkCalls(
        reportOn: PsiElement,
        context: CallCheckerContext,
        resolvedCall: ResolvedCall<*>
    ) {
        if (resolvedCall.call.isCallableReference()) return

        val enclosingReaderContext = findEnclosingContext(context) {
            if (it is AnonymousFunctionDescriptor) {
                val name = it.findPsi()?.parent?.parent?.parent
                    ?.safeAs<KtCallExpression>()?.calleeExpression?.text
                if (name == "runReader") return@findEnclosingContext true
            }
            it.isReader()
        }

        if (enclosingReaderContext == null) {
            context.trace.report(
                InjektErrors.FORBIDDEN_READER_CALL.on(reportOn)
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
