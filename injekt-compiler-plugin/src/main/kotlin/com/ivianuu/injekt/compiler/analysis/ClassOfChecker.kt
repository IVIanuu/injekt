package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class ClassOfChecker(private val typeAnnotationChecker: TypeAnnotationChecker) : CallChecker {

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() != "com.ivianuu.injekt.classOf") return

        val enclosingInjektDslFunction = findEnclosingModuleFunctionContext(context) {
            val typeAnnotations = typeAnnotationChecker.getTypeAnnotations(context.trace, it)
            InjektFqNames.Module in typeAnnotations ||
                    InjektFqNames.Factory in typeAnnotations ||
                    InjektFqNames.ChildFactory in typeAnnotations
        }
        if (enclosingInjektDslFunction == null) {
            context.trace.report(
                InjektErrors.CLASS_OF_OUTSIDE_OF_MODULE
                    .on(reportOn)
            )
        } else {
            val typeArgument = resolvedCall.typeArguments.values.single()
            if (!typeArgument.isTypeParameter()) {
                context.trace.report(
                    InjektErrors.CLASS_OF_WITH_CONCRETE_TYPE
                        .on(reportOn)
                )
            }

            if (!enclosingInjektDslFunction.isInline) {
                context.trace.report(
                    InjektErrors.CLASS_OF_CALLING_MODULE_MUST_BE_INLINE
                        .on(reportOn)
                )
            }

            enclosingInjektDslFunction.typeParameters.forEach {
                if (it.isReified) {
                    context.trace.report(
                        InjektErrors.MODULE_CANNOT_USE_REIFIED
                            .on(it.findPsi() ?: reportOn)
                    )
                }
            }
        }
    }

}
