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
        val FORBIDDEN_READER_CALL = error(
            "@Reader functions can only be invoked inside a enclosing @Reader scope"
        )

        @JvmField
        val CONTEXT_MUST_BE_AN_INTERFACE = error(
            "@Context must be an interface"
        )

        @JvmField
        val NOT_A_CONTEXT = error(
            "Must be a @Context type"
        )

        @JvmField
        val CONTEXT_WITH_TYPE_PARAMETERS = error(
            "@Context cannot have type parameters"
        )

        @JvmField
        val GIVEN_CLASS_CANNOT_BE_ABSTRACT = error(
            "@Given class cannot be an interface or abstract"
        )

        @JvmField
        val EITHER_CLASS_OR_CONSTRUCTOR_GIVEN = error(
            "Either the class or a constructor may be annotated with @Given"
        )

        @JvmField
        val MULTIPLE_GIVEN_ANNOTATED_CONSTRUCTORS = error(
            "Only 1 one constructor may be annotated with @Given"
        )

        @JvmField
        val MULTIPLE_CONSTRUCTORS_ON_GIVEN_CLASS = error(
            "Can't choose a constructor. Annotate the right one with @Given"
        )

        @JvmField
        val READER_CLASS_CANNOT_BE_INTERFACE = error(
            "@Reader class cannot be an interface"
        )

        @JvmField
        val READER_CLASS_CANNOT_BE_OBJECT = error(
            "@Reader class cannot be an object"
        )

        @JvmField
        val READER_PROPERTY_WITH_BACKING_FIELD = error(
            "@Reader property cannot have a backing field"
        )

        @JvmField
        val READER_PROPERTY_VAR = error(
            "@Reader property cannot have be a var"
        )

        @JvmField
        val MULTIPLE_READER_ANNOTATIONS = error(
            "A declaration can only be annotated with one of @Reader, @Given, @GivenMapEntries, @GivenSetElements or @Effect annotated annotations"
        )

        @JvmField
        val EFFECT_WITHOUT_COMPANION = error(
            "@Effect annotated class needs a companion object"
        )

        @JvmField
        val EFFECT_FUNCTION_CANNOT_HAVE_VALUE_PARAMETERS = error(
            "@Effect functions cannot have value parameters"
        )

        @JvmField
        val NOT_A_GIVEN_SET = error(
            "Not a @GivenSet"
        )

        @JvmField
        val EFFECT_FUNCTION_NEEDS_ONE_TYPE_PARAMETER = error(
            "@Effect functions must have 1 type parameter"
        )

        @JvmField
        val NOT_IN_EFFECT_BOUNDS = error(
            "Annotated class is not in @Effect bounds"
        )

        @JvmField
        val EFFECT_WITH_TYPE_PARAMETERS = error(
            "@Effect annotated declaration cannot have type parameters"
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
