package com.ivianuu.injekt.compiler.checkers

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.supertypes

@Given
class ReaderContextChecker : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is ClassDescriptor) return

        if (descriptor.hasAnnotation(InjektFqNames.Context)) {
            if (descriptor.kind != ClassKind.INTERFACE) {
                context.trace.report(
                    InjektErrors.CONTEXT_MUST_BE_AN_INTERFACE
                        .on(declaration)
                )
            }

            if (descriptor.declaredTypeParameters.isNotEmpty()) {
                context.trace.report(
                    InjektErrors.CONTEXT_WITH_TYPE_PARAMETERS
                        .on(declaration)
                )
            }

            if (descriptor.getAllDeclarations().isNotEmpty()) {
                context.trace.report(
                    InjektErrors.CONTEXT_WITH_TYPE_PARAMETERS
                        .on(declaration)
                )
            }
        }
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() != "com.ivianuu.injekt.runReader")
            return

        if (!resolvedCall.extensionReceiver!!.type.constructor.declarationDescriptor!!
                .hasAnnotation(InjektFqNames.Context)
        ) {
            context.trace.report(
                InjektErrors.NOT_A_CONTEXT
                    .on(reportOn)
            )
        }
    }

    private fun ClassDescriptor.getAllDeclarations(): Set<DeclarationDescriptor> {
        val declarations = mutableSetOf<DeclarationDescriptor>()
        fun ClassDescriptor.collect() {
            declarations += unsubstitutedMemberScope.getContributedDescriptors(
                DescriptorKindFilter.CALLABLES
            )
                .filter {
                    (it is FunctionDescriptor
                            && it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() != true)
                            || it is PropertyDescriptor
                }
            defaultType.supertypes()
                .mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }
                .forEach { it.collect() }
        }

        collect()

        return declarations
    }

}
