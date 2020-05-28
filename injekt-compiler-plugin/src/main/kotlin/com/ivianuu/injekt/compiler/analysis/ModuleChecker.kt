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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.isCallableReference
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf

class ModuleChecker : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is FunctionDescriptor || !descriptor.hasAnnotation(InjektFqNames.Module)) return

        (descriptor.dispatchReceiverParameter?.value?.type?.constructor?.declarationDescriptor as? ClassDescriptor)?.let {
            if (it.kind != ClassKind.OBJECT) {
                context.trace.report(
                    InjektErrors.MUST_BE_STATIC
                        .on(declaration)
                )
            }
        }

        if (descriptor.extensionReceiverParameter != null) {
            context.trace.report(
                InjektErrors.CANNOT_BE_EXTENSION
                    .on(declaration)
            )
        }

        if (descriptor.visibility == Visibilities.LOCAL) {
            context.trace.report(
                InjektErrors.CANNOT_BE_LOCAL
                    .on(declaration)
            )
        }

        if (descriptor.returnType != null && descriptor.returnType != descriptor.builtIns.unitType) {
            context.trace.report(
                InjektErrors.RETURN_TYPE_NOT_ALLOWED_FOR_MODULE.on(declaration)
            )
        }

        if (descriptor.isSuspend) {
            context.trace.report(
                InjektErrors.CANNOT_BE_SUSPEND
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

        if (resulting.fqNameSafe.asString() == "com.ivianuu.injekt.composition.installIn") {
            if (resolvedCall.typeArguments.values.single()?.constructor?.declarationDescriptor?.annotations
                    ?.hasAnnotation(InjektFqNames.CompositionComponent) != true
            ) {
                context.trace.report(
                    InjektErrors.NOT_A_COMPOSITION_COMPONENT
                        .on(reportOn)
                )
            }

            val enclosingModule = findEnclosingFunctionContext(context) {
                it.hasAnnotation(InjektFqNames.Module)
            }

            if (enclosingModule == null) {
                context.trace.report(
                    InjektErrors.INSTALL_IN_CALL_WITHOUT_MODULE
                        .on(reportOn)
                )
            } else {
                if (enclosingModule.valueParameters.isNotEmpty()) {
                    context.trace.report(
                        InjektErrors.COMPOSITION_MODULE_CANNOT_HAVE_VALUE_PARAMETERS
                            .on(reportOn)
                    )
                }
                if (enclosingModule.typeParameters.isNotEmpty()) {
                    context.trace.report(
                        InjektErrors.COMPOSITION_MODULE_CANNOT_HAVE_TYPE_PARAMETERS
                            .on(reportOn)
                    )
                }
            }
        }

        if (resulting !is FunctionDescriptor || !resulting.hasAnnotation(InjektFqNames.Module)) return
        checkInvocations(resolvedCall, reportOn, context)
    }

    private fun checkInvocations(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val enclosingInjektDslFunction = findEnclosingFunctionContext(context) {
            it.hasAnnotation(InjektFqNames.Module) ||
                    it.hasAnnotation(InjektFqNames.Factory) ||
                    it.hasAnnotation(InjektFqNames.ChildFactory) ||
                    it.hasAnnotation(InjektFqNames.CompositionFactory) ||
                    it.hasAnnotation(InjektFqNames.InstanceFactory)
        }

        when {
            enclosingInjektDslFunction != null -> {
                var isConditional = false

                var walker: PsiElement? = resolvedCall.call.callElement
                while (walker != null) {
                    val parent = walker.parent
                    if (parent is KtIfExpression ||
                        parent is KtForExpression ||
                        parent is KtWhenExpression ||
                        parent is KtTryExpression ||
                        parent is KtCatchClause ||
                        parent is KtWhileExpression
                    ) {
                        isConditional = true
                    }
                    walker = try {
                        walker.parent
                    } catch (e: Throwable) {
                        null
                    }
                }

                if (isConditional) {
                    context.trace.report(
                        InjektErrors.CONDITIONAL_NOT_ALLOWED_IN_MODULE_AND_FACTORIES.on(reportOn)
                    )
                }

                if (context.scope.parentsWithSelf.any {
                        it.isScopeForDefaultParameterValuesOf(
                            enclosingInjektDslFunction
                        )
                    }) {
                    context.trace.report(
                        Errors.UNSUPPORTED.on(
                            reportOn,
                            "@Module function calls in a context of default parameter value"
                        )
                    )
                }
            }
            resolvedCall.call.isCallableReference() -> {
                // do nothing: we can get callable reference to suspend function outside suspend context
            }
            else -> {
                context.trace.report(
                    InjektErrors.FORBIDDEN_MODULE_INVOCATION.on(reportOn)
                )
            }
        }
    }
}
