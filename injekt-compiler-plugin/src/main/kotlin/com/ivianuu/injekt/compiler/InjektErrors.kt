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
        val MAP = DiagnosticFactoryToRendererMap("Injekt")

        private fun error(message: String) =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, message) }

        @JvmField
        val FORBIDDEN_MODULE_INVOCATION = error(
            "Only @Factory, @ChildFactory or @Module functions can invoke @Module functions"
        )
        @JvmField
        val CONDITIONAL_NOT_ALLOWED_IN_MODULE_AND_FACTORIES = error(
            "Conditional logic is not allowed inside @Factory, @ChildFactory and @Module functions"
        )
        @JvmField
        val RETURN_TYPE_NOT_ALLOWED_FOR_MODULE = error(
            "@Module functions cannot return anything"
        )
        @JvmField
        val CREATE_IMPl_WITHOUT_FACTORY = error(
            "createImpl can only be called from within a @Factory or @ChildFactory function"
        )
        @JvmField
        val CREATE_INSTANCE_IN_CHILD_FACTORY = error(
            "createInstance cannot be called in a @ChildFactory function"
        )
        @JvmField
        val UNSUPPORTED_MAP_KEY_TYPE = error(
            "Unsupported map key type"
        )
        @JvmField
        val MAP_KEY_MUST_BE_CONSTANT = error(
            "Map key must be a compile time constant"
        )
        @JvmField
        val NOT_A_SCOPE = error(
            "Scopes must be itself annotated with @Scope"
        )
        @JvmField
        val NOT_A_CHILD_FACTORY = error(
            "Not a @ChildFactory"
        )
        @JvmField
        val LAST_STATEMENT_MUST_BE_CREATE = error(
            "A factories last statement must be either a createImpl() or a createInstance() call"
        )
        @JvmField
        val FACTORY_IMPL_MUST_BE_ABSTRACT = error(
            "createImpl result types must be a interface or a abstract class"
        )

        @JvmField
        val FACTORY_WITH_TYPE_PARAMETERS_MUST_BE_INLINE = error(
            "@Factory or @ChildFactory functions with type parameters must be marked with inline"
        )
        @JvmField
        val IMPL_SUPER_TYPE_MUST_HAVE_EMPTY_CONSTRUCTOR = error(
            "Implementation super class must have an empty constructor"
        )
        @JvmField
        val IMPL_CANNOT_CONTAIN_VARS = error(
            "createImpl result types cannot contain mutable properties"
        )
        @JvmField
        val PROVISION_FUNCTION_CANNOT_HAVE_VALUE_PARAMETERS = error(
            "Provision functions cannot have value parameters"
        )
        @JvmField
        val PROVISION_FUNCTION_CANNOT_HAVE_TYPE_PARAMETERS = error(
            "Provision functions cannot have type parameters"
        )
        @JvmField
        val PROVISION_FUNCTION_CANNOT_BE_SUSPEND = error(
            "Provision functions cannot be suspend"
        )
        @JvmField
        val INJECT_MUST_BE_LATEINIT_VAR = error(
            "@Inject property must be lateinit var"
        )
        @JvmField
        val CANNOT_INVOKE_CHILD_FACTORIES = error(
            "Cannot invoke @ChildFactory functions"
        )
        @JvmField
        val MUST_HAVE_RUNTIME_RETENTION = error(
            "Annotated class must have runtime retention"
        )
        @JvmField
        val MISSING_QUALIFIER_TARGETS = error(
            "@Qualifier must be annotated with @Target(AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION)"
        )
        @JvmField
        val ANNOTATED_BINDING_CANNOT_BE_ABSTRACT = error(
            "Annotated bindings cannot be an interface or abstract"
        )
        @JvmField
        val EITHER_MODULE_OR_FACTORY = error(
            "A function can only be annotated with one of @Factory, @ChildFactory or @Module"
        )
        @JvmField
        val CANNOT_BE_SUSPEND = error(
            "@Factory, @ChildFactory or @Module cannot be suspend"
        )
        @JvmField
        val CLASS_OF_OUTSIDE_OF_MODULE = error(
            "classOf() can only be called from inside @Factory, @ChildFactory or @Module functions"
        )
        @JvmField
        val CLASS_OF_WITH_CONCRETE_TYPE = error(
            "classOf() should be only called with generic types use class literals instead"
        )
        @JvmField
        val CLASS_OF_CALLING_MODULE_MUST_BE_INLINE = error(
            "classOf() calling functions be marked with inline"
        )
        @JvmField
        val MODULE_CANNOT_USE_REIFIED = error(
            "@Module functions cannot use reified"
        )
        @JvmField
        val GENERIC_BINDING_WITHOUT_INLINE_AND_DEFINITION = error(
            "Binding functions with a generic type can only be used inside a inline module or with a definition"
        )
        @JvmField
        val DEFINITION_PARAMETER_WITHOUT_INLINE = error(
            "@Module functions with definition parameters must be marked with inline"
        )

        init {
            Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
                InjektErrors::class.java,
                InjektDefaultErrorMessages
            )
        }

        object InjektDefaultErrorMessages : DefaultErrorMessages.Extension {
            override fun getMap() = MAP
        }

    }
}
