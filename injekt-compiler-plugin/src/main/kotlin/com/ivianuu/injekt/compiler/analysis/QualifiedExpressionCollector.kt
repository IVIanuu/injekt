package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.QualifiedExpressionsStore
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class QualifiedExpressionCollector : CallChecker {

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val callElement = resolvedCall.call.callElement
        if (callElement !is KtCallExpression) return
        val qualifiers = callElement.getAnnotationEntries()
            .mapNotNull { context.trace[BindingContext.ANNOTATION, it] }
        if (qualifiers.isEmpty()) return
        QualifiedExpressionsStore.putQualifiers(
            callElement.containingFile.name,
            callElement.startOffsetSkippingComments,
            callElement.endOffset,
            qualifiers
        )
    }

}
