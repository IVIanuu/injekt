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

        @JvmField
        val CREATE_IMPLEMENTATION_INVOCATION_WITHOUT_FACTORY = error()

        @JvmField
        val UNSUPPORTED_MAP_KEY_TYPE = error()

        @JvmField
        val MAP_KEY_MUST_BE_CONSTANT = error()

        @JvmField
        val NOT_A_SCOPE = error()

        @JvmField
        val NOT_A_CHILD_FACTORY = error()

        @JvmField
        val ONLY_CREATE_ALLOWED = error()

        @JvmField
        val FACTORY_MUST_BE_STATIC = error()

        @JvmField
        val NO_TYPE_PARAMETERS_ON_FACTORY = error()

        @JvmField
        val FACTORY_IMPL_MUST_BE_ABSTRACT = error()

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
        map.put(
            InjektErrors.CREATE_IMPLEMENTATION_INVOCATION_WITHOUT_FACTORY,
            "createImplementation can only be called from within a @Factory or @ChildFactory function"
        )

        map.put(
            InjektErrors.UNSUPPORTED_MAP_KEY_TYPE,
            "Unsupported map key type"
        )

        map.put(
            InjektErrors.MAP_KEY_MUST_BE_CONSTANT,
            "Map key must be a compile time constant"
        )
        map.put(
            InjektErrors.NOT_A_SCOPE,
            "Scopes must be itself annotated with @Scope"
        )
        map.put(
            InjektErrors.NOT_A_CHILD_FACTORY,
            "Not a @ChildFactory"
        )
        map.put(
            InjektErrors.ONLY_CREATE_ALLOWED,
            "Factories must have exact 1 statement: either createImplementation or createInstance"
        )
        map.put(
            InjektErrors.FACTORY_IMPL_MUST_BE_ABSTRACT,
            "createImplementation result types must be a interface or a abstract class"
        )
        map.put(
            InjektErrors.FACTORY_MUST_BE_STATIC,
            "@Factory and @ChildFactory functions must be static"
        )
        map.put(
            InjektErrors.NO_TYPE_PARAMETERS_ON_FACTORY,
            "@Factory and @ChildFactory functions cannot have type parameters"
        )
    }
}
