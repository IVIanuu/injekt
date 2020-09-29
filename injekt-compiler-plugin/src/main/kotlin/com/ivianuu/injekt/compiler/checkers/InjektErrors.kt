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

package com.ivianuu.injekt.compiler.checkers

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

interface
InjektErrors {
    companion object {
        @JvmField
        val MAP = DiagnosticFactoryToRendererMap("Injekt")

        private fun error(message: String) =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, message) }


        @JvmField
        val FACTORY_EXPANDED_TYPE_MUST_BE_FUNCTION = error(
            "@RootFactory, @ChildFactory and @MergeFactory must be a function type"
        )

        @JvmField
        val ROOT_FACTORY_WITH_TYPE_PARAMETERS = error(
            "@RootFactory cannot have type parameters"
        )

        @JvmField
        val EITHER_CLASS_OR_CONSTRUCTOR_GIVEN = error(
            "Either the class or 1 constructor may be annotated with @Given"
        )

        @JvmField
        val MULTIPLE_CONSTRUCTORS_ON_GIVEN_CLASS = error(
            "Can't choose a constructor. Annotate the right one with @Given"
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
        val EFFECT_FUNCTION_INVALID_TYPE_PARAMETERS = error(
            "@Effect function must have one of the following type parameter signatures <T> or <T : S, S>"
        )

        @JvmField
        val NOT_IN_EFFECT_BOUNDS = error(
            "Annotated class is not in @Effect bounds"
        )

        @JvmField
        val EFFECT_USAGE_WITH_TYPE_PARAMETERS = error(
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
