package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

interface InjektErrors {
    companion object {

        @JvmField
        val MODULE_INVOCATION_IN_NON_MODULE = error()

        @JvmField
        val CONDITIONAL_NOT_ALLOWED_IN_MODULE = error()

        @JvmField
        val RETURN_TYPE_NOT_ALLOWED_FOR_MODULE = error()

        private fun error() = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)

        init {
            Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
                InjektErrors::class.java,
                InjektDefaultErrorMessages
            )
        }
    }
}

object InjektDefaultErrorMessages : DefaultErrorMessages.Extension {
    private val map = DiagnosticFactoryToRendererMap("Injekt")
    override fun getMap(): DiagnosticFactoryToRendererMap = map

    init {
        map.put(
            InjektErrors.MODULE_INVOCATION_IN_NON_MODULE,
            "Functions which invoke @Module functions must be marked with the @Module " +
                    "annotation"
        )
        map.put(
            InjektErrors.CONDITIONAL_NOT_ALLOWED_IN_MODULE,
            "Conditional logic is not allowed around a @Module function call"
        )
        map.put(
            InjektErrors.RETURN_TYPE_NOT_ALLOWED_FOR_MODULE,
            "@Module functions cannot return anything"
        )
    }
}