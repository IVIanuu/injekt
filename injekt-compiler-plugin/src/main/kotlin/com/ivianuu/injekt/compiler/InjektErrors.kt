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
        val ONLY_ONE_SCOPE_ANNOTATION = error()

        @JvmField
        val IMPLICIT_MODULE_MUST_BE_STATIC = error()

        @JvmField
        val IMPLICIT_MODULE_CANNOT_HAVE_VALUE_PARAMETERS = error()

        @JvmField
        val IMPLICIT_MODULE_CANNOT_HAVE_TYPE_PARAMETERS = error()

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
            InjektErrors.ONLY_ONE_SCOPE_ANNOTATION,
            "Can only have 1 @Scope annotation"
        )
        map.put(
            InjektErrors.IMPLICIT_MODULE_MUST_BE_STATIC,
            "Implicit modules must be static"
        )
        map.put(
            InjektErrors.IMPLICIT_MODULE_CANNOT_HAVE_VALUE_PARAMETERS,
            "Implicit modules cannot have value parameters"
        )
        map.put(
            InjektErrors.IMPLICIT_MODULE_CANNOT_HAVE_TYPE_PARAMETERS,
            "Implicit modules cannot have type parameters"
        )
    }
}