/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            "Only @Factory, @ChildFactory, @CompositionFactory or @Module functions can invoke @Module functions"
        )

        @JvmField
        val FORBIDDEN_DSL_FUNCTION_INVOCATION = error(
            "Only @Factory, @ChildFactory, @CompositionFactory or @Module functions can invoke this function"
        )

        @JvmField
        val CONDITIONAL_NOT_ALLOWED_IN_MODULE_AND_FACTORIES = error(
            "Conditional logic is not allowed inside @Factory, @ChildFactory, @CompositionFactory and @Module functions"
        )

        @JvmField
        val RETURN_TYPE_NOT_ALLOWED_FOR_MODULE = error(
            "@Module functions cannot return anything"
        )

        @JvmField
        val CREATE_WITHOUT_FACTORY = error(
            "create can only be called from within a @Factory, @ChildFactory, @CompositionFactory or @InstanceFactory function"
        )

        @JvmField
        val INLINE_FACTORY_CALL_MUST_HAVE_CONCRETE_TYPE = error(
            "Inlined @Factory call cannot contain type parameters"
        )

        @JvmField
        val NOT_A_COMPOSITION_COMPONENT = error(
            "Type must be annotated with @CompositionComponent"
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
            "A factories last statement must be either a create() call"
        )

        @JvmField
        val FACTORY_RETURN_TYPE_MUST_BE_ABSTRACT = error(
            "@Factory return types must be a interface or a abstract class"
        )

        @JvmField
        val FACTORY_WITH_TYPE_PARAMETERS_MUST_BE_INLINE = error(
            "@Factory functions with type parameters must be marked with inline"
        )

        @JvmField
        val CHILD_AND_COMPOSITION_FACTORY_CANNOT_HAVE_TYPE_PARAMETERS = error(
            "@ChildFactory or @CompositionFactory functions cannot have type parameters"
        )

        @JvmField
        val CHILD_AND_COMPOSITION_FACTORY_CANNOT_BE_INLINE = error(
            "@ChildFactory or @CompositionFactory cannot be marked with inline"
        )

        @JvmField
        val MODULE_PARAMETER_WITHOUT_INLINE = error(
            "@Factory or @ChildFactory or @Module functions with @Module function parameters must be marked with inline"
        )

        @JvmField
        val IMPL_SUPER_TYPE_MUST_HAVE_EMPTY_CONSTRUCTOR = error(
            "Implementation super class must have an empty constructor"
        )

        @JvmField
        val PROVISION_PROPERTY_CANNOT_BE_VAR = error(
            "Provision property cannot be mutable properties"
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
        val CANNOT_INVOKE_CHILD_OR_COMPOSITION_FACTORIES = error(
            "Cannot invoke @ChildFactory or @CompositionFactory functions"
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
            "A function can only be annotated with one of @Factory, @ChildFactory, @CompositionFactory or @Module"
        )

        @JvmField
        val CANNOT_BE_SUSPEND = error(
            "@Factory, @ChildFactory, @CompositionFactory or @Module cannot be suspend"
        )

        @JvmField
        val CLASS_OF_OUTSIDE_OF_MODULE = error(
            "classOf() can only be called from inside @Factory, @ChildFactory, @CompositionFactory or @Module functions"
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

        @JvmField
        val BINDING_ADAPTER_WITHOUT_COMPANION = error(
            "@BindingAdapter annotated class needs a companion object."
        )

        @JvmField
        val BINDING_ADAPTER_COMPANION_WITHOUT_MODULE = error(
            "@BindingAdapter companion needs a single @Module function."
        )

        @JvmField
        val BINDING_ADAPTER_MODULE_CANNOT_HAVE_VALUE_PARAMETERS = error(
            "@BindingAdapter module cannot have value parameters."
        )

        @JvmField
        val BINDING_ADAPTER_MODULE_NEEDS_1_TYPE_PARAMETER = error(
            "@BindingAdapter module must have 1 type parameter."
        )

        @JvmField
        val NOT_IN_BINDING_ADAPTER_BOUNDS = error(
            "Annotated class is not in @BindingAdapter bounds."
        )

        @JvmField
        val MULTIPLE_BINDING_ADAPTER = error(
            "A class may be only annotated with 1 @BindingAdapter class"
        )

        @JvmField
        val BINDING_ADAPTER_WITH_TRANSIENT_OR_SCOPED = error(
            "@BindingAdapter cannot be combined with @Transient or @Scope annotations."
        )

        @JvmField
        val TRANSIENT_WITH_SCOPED = error(
            "@Transient cannot be combined with @Scope annotated annotations."
        )

        @JvmField
        val MULTIPLE_SCOPES = error(
            "A class may be only annotated with 1 @Scope class"
        )

        @JvmField
        val COMPOSITION_MODULE_CANNOT_HAVE_VALUE_PARAMETERS = error(
            "Composition @Module functions cannot have value parameters."
        )

        @JvmField
        val COMPOSITION_MODULE_CANNOT_HAVE_TYPE_PARAMETERS = error(
            "Composition @Module functions cannot have type parameters."
        )

        @JvmField
        val INSTALL_IN_CALL_WITHOUT_MODULE = error(
            "installIn<T>() can only be called from inside a @Module function"
        )

        @JvmField
        val PARENT_CALL_WITHOUT_COMPOSITION_FACTORY = error(
            "parent<T>() can only be called from inside a @CompositionFactory function"
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
