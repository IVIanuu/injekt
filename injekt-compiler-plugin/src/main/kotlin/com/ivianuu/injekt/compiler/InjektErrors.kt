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
        val ParamCannotBeNamed = error()
        @JvmField
        val OnlyOneInjektConstructor = error()
        @JvmField
        val OnlyOneScope = error()
        @JvmField
        val NeedsPrimaryConstructor = error()
        @JvmField
        val MustBeAModule = error()
        @JvmField
        val MustBeStatic = error()

        @JvmField
        val NotAValidAnnotationType = error()

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
            InjektErrors.OnlyOneScope,
            "Can only have one 1 scope annotation"
        )
        map.put(
            InjektErrors.ParamCannotBeNamed,
            "Parameters cannot be named"
        )
        map.put(
            InjektErrors.OnlyOneInjektConstructor,
            "Only one constructor can be annotated"
        )
        map.put(
            InjektErrors.NeedsPrimaryConstructor,
            "Class needs a primary constructor or the constructor must be annotated"
        )
        map.put(
            InjektErrors.MustBeAModule,
            "@ModuleMarker declaration must be of type Qualifier"
        )
        map.put(
            InjektErrors.MustBeStatic,
            "Must be a top-level declaration"
        )
        map.put(
            InjektErrors.NotAValidAnnotationType,
            "Not a valid annotation type"
        )
    }

}
