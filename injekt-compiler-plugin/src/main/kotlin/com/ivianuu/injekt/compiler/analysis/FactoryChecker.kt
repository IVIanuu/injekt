package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isCallableReference
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf

class FactoryChecker : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor is FunctionDescriptor && (descriptor.annotations.hasAnnotation(InjektFqNames.Factory) ||
                    descriptor.annotations.hasAnnotation(InjektFqNames.ChildFactory))
        ) {
            if (descriptor.typeParameters.isNotEmpty()) {
                context.trace.report(InjektErrors.NO_TYPE_PARAMETERS_ON_FACTORY.on(declaration))
            }

            checkFactoryFunctionHasOnlyCreateImplementationStatement(
                declaration as KtFunction,
                descriptor,
                context
            )
        }
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        when (resolvedCall.resultingDescriptor.fqNameSafe.asString()) {
            "com.ivianuu.injekt.childFactory" -> {
                val referencedFunction =
                    resolvedCall.valueArgumentsByIndex!!.singleOrNull()
                        ?.arguments
                        ?.single()
                        ?.getArgumentExpression()
                        ?.let { it as? KtCallableReferenceExpression }
                        ?.callableReference
                        ?.getResolvedCall(context.trace.bindingContext)
                        ?.resultingDescriptor

                if (referencedFunction?.annotations?.hasAnnotation(InjektFqNames.ChildFactory) != true) {
                    context.trace.report(InjektErrors.NOT_A_CHILD_FACTORY.on(reportOn))
                }
            }
            "com.ivianuu.injekt.createImplementation" -> {
                checkCreateImplementationInvocation(resolvedCall, reportOn, context)
            }
        }
    }

    private fun checkFactoryFunctionHasOnlyCreateImplementationStatement(
        element: KtFunction,
        descriptor: FunctionDescriptor,
        context: DeclarationCheckerContext
    ) {
        fun report() {
            context.trace.report(InjektErrors.ONLY_CREATE_ALLOWED.on(element))
        }

        val expression = if (element.bodyExpression != null) element.bodyExpression else
            element.bodyBlockExpression?.statements?.singleOrNull()

        val returnedExpression = when (expression) {
            is KtBlockExpression -> expression.statements.singleOrNull()
                ?.let { it as? KtReturnExpression }?.returnedExpression
            is KtReturnExpression -> expression.returnedExpression
            else -> expression
        }
        if (returnedExpression !is KtCallExpression) {
            report()
            return
        }

        val resolvedCall = returnedExpression.getResolvedCall(context.trace.bindingContext)
        if (resolvedCall == null) {
            report()
            return
        }

        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() != "com.ivianuu.injekt.createImplementation" ||
            resolvedCall.resultingDescriptor.fqNameSafe.asString() != "com.ivianuu.injekt.createImplementation"
        ) {
            report()
        }
    }

    private fun checkCreateImplementationInvocation(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val type =
            resolvedCall.typeArguments.values.singleOrNull()?.constructor?.declarationDescriptor as? ClassDescriptor

        if (type?.modality != Modality.ABSTRACT) {
            context.trace.report(
                InjektErrors.FACTORY_IMPL_MUST_BE_ABSTRACT
                    .on(reportOn)
            )
            return
        }

        val enclosingModuleFunction = findEnclosingModuleFunctionContext(context) {
            it.annotations.hasAnnotation(InjektFqNames.Factory) ||
                    it.annotations.hasAnnotation(InjektFqNames.ChildFactory)
        }

        when {
            enclosingModuleFunction != null -> {
                if (context.scope.parentsWithSelf.any {
                        it.isScopeForDefaultParameterValuesOf(
                            enclosingModuleFunction
                        )
                    }) {
                    context.trace.report(
                        Errors.UNSUPPORTED.on(
                            reportOn,
                            "createImplementation function calls in a context of default parameter value"
                        )
                    )
                }
            }
            resolvedCall.call.isCallableReference() -> {
                // do nothing: we can get callable reference to suspend function outside suspend context
            }
            else -> {
                context.trace.report(
                    InjektErrors.CREATE_IMPLEMENTATION_INVOCATION_WITHOUT_FACTORY.on(reportOn)
                )
            }
        }
    }
}
