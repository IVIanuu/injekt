package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.descriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor

class InjektDiagnosticSuppressor : DiagnosticSuppressor {

    override fun isSuppressed(diagnostic: Diagnostic): Boolean =
        isSuppressed(diagnostic, null)

    override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
        if (bindingContext == null) return false

        if (diagnostic.factory == Errors.UNUSED_PARAMETER) {
            val descriptor =
                (diagnostic.psiElement as KtDeclaration).descriptor<ParameterDescriptor>(
                    bindingContext)
                    ?: return false
            if (bindingContext[InjektWritableSlices.USED_GIVEN, descriptor] != null) return true
        }

        return false
    }
}
