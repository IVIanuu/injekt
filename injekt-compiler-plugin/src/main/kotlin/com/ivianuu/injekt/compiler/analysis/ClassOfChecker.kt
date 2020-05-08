package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ClassOfChecker : CallChecker {

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.classOf") {
            val enclosingInjektDslFunction = findEnclosingModuleFunctionContext(context) {
                it.annotations.hasAnnotation(InjektFqNames.Module) ||
                        it.annotations.hasAnnotation(InjektFqNames.Factory) ||
                        it.annotations.hasAnnotation(InjektFqNames.ChildFactory)
            }
            if (enclosingInjektDslFunction == null) {
                context.trace.report(
                    InjektErrors.CLASS_OF_OUTSIDE_OF_MODULE
                        .on(reportOn)
                )
            }
        }
    }

}
