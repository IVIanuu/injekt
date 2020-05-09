package com.ivianuu.injekt.compiler.analysis

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor

abstract class AbstractDiagnosticSuppressor : DiagnosticSuppressor {
    override fun isSuppressed(diagnostic: Diagnostic) =
        isSuppressed(diagnostic, null)
}
