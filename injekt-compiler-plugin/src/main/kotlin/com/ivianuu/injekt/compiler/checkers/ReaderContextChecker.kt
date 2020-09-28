package com.ivianuu.injekt.compiler.checkers

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given
class ReaderContextChecker : CallChecker {

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() != "com.ivianuu.injekt.runReader")
            return

        if (resolvedCall.extensionReceiver?.type?.constructor?.declarationDescriptor?.fqNameSafe ==
            InjektFqNames.Context
        ) {
            context.trace.report(
                InjektErrors.MUST_BE_A_CONCRETE_CONTEXT_TYPE
                    .on(reportOn)
            )
        }
    }

}
