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
        val MUST_BE_FOR_KEY = error(
            "Type parameter must have @ForKey annotation if used as a @ForKey type argument"
        )

        @JvmField
        val FOR_KEY_TYPE_PARAMETER_INVALID_PARENT = error(
            "Type parameter with @ForKey can only be declared inside a function or properties"
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
