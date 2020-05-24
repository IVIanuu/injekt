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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
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

class ModuleChecker(
    private val typeAnnotationChecker: TypeAnnotationChecker
) : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {

        if (descriptor !is FunctionDescriptor ||
            !typeAnnotationChecker.hasTypeAnnotation(
                context.trace,
                descriptor,
                InjektFqNames.Module
            )
        ) return

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

        if (!descriptor.isInline) {
            descriptor.valueParameters.forEach { valueParameter ->
                if (valueParameter.type.hasAnnotation(InjektFqNames.ProviderDsl)) {
                    context.trace.report(
                        InjektErrors.PROVIDER_DSL_PARAMETER_WITHOUT_INLINE
                            .on(valueParameter.findPsi() ?: declaration)
                    )
                }
                if (valueParameter.type.hasAnnotation(InjektFqNames.Module)) {
                    context.trace.report(
                        InjektErrors.MODULE_PARAMETER_WITHOUT_INLINE
                            .on(valueParameter.findPsi() ?: declaration)
                    )
                }
            }
        }

        descriptor.typeParameters.forEach { typeParameter ->
            if (typeParameter.isReified) {
                context.trace.report(
                    InjektErrors.CANNOT_USE_REIFIED
                        .on(typeParameter.findPsi() ?: declaration)
                )
            }
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
                val typeAnnotations = typeAnnotationChecker.getTypeAnnotations(context.trace, it)
                InjektFqNames.Module in typeAnnotations
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

        if (resulting !is FunctionDescriptor || !typeAnnotationChecker.hasTypeAnnotation(
                context.trace, resulting, InjektFqNames.Module
            )
        ) return
        checkInvocations(resolvedCall, reportOn, context)
    }

    private fun checkInvocations(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val enclosingInjektDslFunction = findEnclosingFunctionContext(context) {
            val typeAnnotations = typeAnnotationChecker.getTypeAnnotations(context.trace, it)
            InjektFqNames.Module in typeAnnotations ||
                    InjektFqNames.Factory in typeAnnotations ||
                    InjektFqNames.ChildFactory in typeAnnotations ||
                    InjektFqNames.CompositionFactory in typeAnnotations ||
                    InjektFqNames.InstanceFactory in typeAnnotations
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
