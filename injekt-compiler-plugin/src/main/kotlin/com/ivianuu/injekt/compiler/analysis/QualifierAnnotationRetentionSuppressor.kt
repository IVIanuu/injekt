package com.ivianuu.injekt.compiler.analysis

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

class QualifierAnnotationRetentionSuppressor : AbstractDiagnosticSuppressor() {
    override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
        if (diagnostic.factory == Errors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION ||
            diagnostic.factory == Errors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION_WARNING
        ) {
            val qualifierClass = diagnostic.psiElement.getParentOfType<KtClass>(false)
            if (qualifierClass != null && qualifierClass.annotationEntries.any {
                    "@Qualifier" in it.text
                }) {
                return true
            }
        }

        return false
    }
}
