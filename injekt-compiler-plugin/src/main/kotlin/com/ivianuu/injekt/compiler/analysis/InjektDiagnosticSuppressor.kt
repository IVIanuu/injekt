package com.ivianuu.injekt.compiler.analysis

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor

class InjektDiagnosticSuppressor : DiagnosticSuppressor {

    override fun isSuppressed(diagnostic: Diagnostic): Boolean =
        isSuppressed(diagnostic, null)

    override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
        if (bindingContext == null) return false

        if (diagnostic.factory == Errors.UNUSED_PARAMETER) {

        }

        return false
    }
}
