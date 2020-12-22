package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType

class KeyChecker : CallChecker {
    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        resolvedCall
            .typeArguments
            .filterKeys { it.hasAnnotation(InjektFqNames.ForKey) }
            .forEach { it.value.checkAllForKey(reportOn, context.trace) }
    }

    private fun KotlinType.checkAllForKey(
        reportOn: PsiElement,
        trace: BindingTrace
    ) {
        if (constructor.declarationDescriptor is TypeParameterDescriptor &&
                !constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.ForKey)) {
            trace.report(
                InjektErrors.NON_FOR_KEY_TYPE_PARAMETER_AS_FOR_KEY
                    .on(reportOn, constructor.declarationDescriptor as TypeParameterDescriptor)
            )
        }

        arguments.forEach { it.type.checkAllForKey(reportOn, trace) }
    }
}
