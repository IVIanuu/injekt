package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
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
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class ModuleChecker : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is FunctionDescriptor ||
            !descriptor.annotations.hasAnnotation(InjektFqNames.Module)
        ) return
        if (descriptor.returnType != null && descriptor.returnType != descriptor.builtIns.unitType) {
            context.trace.report(
                InjektErrors.RETURN_TYPE_NOT_ALLOWED_FOR_MODULE.on(declaration)
            )
        }

        if (descriptor.visibility == Visibilities.LOCAL) {
            context.trace.report(
                InjektErrors.CANNOT_BE_LOCAL
                    .on(declaration)
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
                if (valueParameter.type.isFunctionType &&
                    valueParameter.type.arguments.firstOrNull()?.type?.constructor?.declarationDescriptor?.fqNameSafe == InjektFqNames.ProviderDsl
                ) {
                    context.trace.report(
                        InjektErrors.DEFINITION_PARAMETER_WITHOUT_INLINE
                            .on(valueParameter.findPsi() ?: declaration)
                    )
                }
            }
        }
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val resulting = resolvedCall.resultingDescriptor
        if (resulting !is FunctionDescriptor ||
            !resulting.annotations.hasAnnotation(InjektFqNames.Module)
        ) return
        checkInvocations(resolvedCall, reportOn, context)
    }

    private fun checkInvocations(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val enclosingInjektDslFunction = findEnclosingModuleFunctionContext(context) {
            it.annotations.hasAnnotation(InjektFqNames.Module) ||
                    it.annotations.hasAnnotation(InjektFqNames.Factory) ||
                    it.annotations.hasAnnotation(InjektFqNames.ChildFactory)
        }

        if (enclosingInjektDslFunction != null &&
            (resolvedCall.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.scoped" ||
                    resolvedCall.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.transient") &&
            resolvedCall.resultingDescriptor.valueParameters.isEmpty() &&
            resolvedCall.typeArguments.values.single().isTypeParameter() &&
            !enclosingInjektDslFunction.isInline
        ) {
            context.trace.report(
                InjektErrors.GENERIC_BINDING_WITHOUT_INLINE_AND_DEFINITION
                    .on(reportOn)
            )
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
