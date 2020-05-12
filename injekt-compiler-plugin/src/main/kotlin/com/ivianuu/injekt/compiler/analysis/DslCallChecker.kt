package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class DslCallChecker(
    private val typeAnnotationChecker: TypeAnnotationChecker
) : CallChecker {
    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall.resultingDescriptor.fqNameSafe !in InjektFqNames.DslNames) return
        val enclosingInjektDslFunction = findEnclosingModuleFunctionContext(context) {
            val typeAnnotations = typeAnnotationChecker.getTypeAnnotations(context.trace, it)
            InjektFqNames.Module in typeAnnotations ||
                    InjektFqNames.Factory in typeAnnotations ||
                    InjektFqNames.ChildFactory in typeAnnotations
        }

        if (enclosingInjektDslFunction != null &&
            resolvedCall.resultingDescriptor.valueParameters.isEmpty() &&
            resolvedCall.typeArguments.values.single().isTypeParameter() &&
            !enclosingInjektDslFunction.isInline
        ) {
            context.trace.report(
                InjektErrors.GENERIC_BINDING_WITHOUT_INLINE_AND_DEFINITION
                    .on(reportOn)
            )
        }

        if (enclosingInjektDslFunction == null) {
            context.trace.report(
                InjektErrors.FORBIDDEN_DSL_FUNCTION_INVOCATION
                    .on(reportOn)
            )
        }
    }
}