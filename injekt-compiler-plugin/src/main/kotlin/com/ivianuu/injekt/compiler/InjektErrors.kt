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
        val FORBIDDEN_READER_INVOCATION = error(
            "@Reader functions can only be invoked inside a enclosing @Reader scope"
        )

        @JvmField
        val COMPONENT_FACTORY_WITH_TYPE_PARAMETERS = error(
            "@Component.Factory cannot contain type parameters"
        )

        @JvmField
        val COMPONENT_FACTORY_SINGLE_FUNCTION = error(
            "@Component.Factory must contain a single function with a @Component return type"
        )

        @JvmField
        val NOT_A_COMPONENT = error(
            "Type must be annotated with @Component"
        )

        @JvmField
        val COMPONENT_WITH_TYPE_PARAMETERS = error(
            "@Component cannot have type parameters"
        )

        @JvmField
        val ANNOTATED_BINDING_CANNOT_BE_ABSTRACT = error(
            "Annotated bindings cannot be an interface or abstract"
        )

        @JvmField
        val READER_MUST_BE_FINAL = error(
            "@Reader must be final"
        )

        @JvmField
        val UNSCOPED_WITH_SCOPED = error(
            "@Unscoped cannot be combined with @Scope annotated annotations"
        )

        @JvmField
        val EITHER_CLASS_OR_CONSTRUCTOR = error(
            "Either the class or a constructor may be annotated"
        )

        @JvmField
        val MULTIPLE_CONSTRUCTORS_ANNOTATED = error(
            "Only 1 one constructor may be annotated"
        )

        @JvmField
        val MULTIPLE_CONSTRUCTORS = error(
            "Can't choose a constructor. Annotate the right one"
        )

        @JvmField
        val READER_CLASS_CANNOT_BE_INTERFACE = error(
            "@Reader class cannot be an interface"
        )

        @JvmField
        val READER_CLASS_CANNOT_BE_OBJECT = error(
            "@Reader class cannot be an object"
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
