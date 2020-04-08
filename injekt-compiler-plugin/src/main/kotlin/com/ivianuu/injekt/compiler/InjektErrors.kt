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
        val MustBeStaticProperty = error()

        @JvmField
        val ParamCannotBeNamed = error()
        @JvmField
        val OnlyOneInjektConstructor = error()
        @JvmField
        val OnlyOneScope = error()
        @JvmField
        val NeedsPrimaryConstructorOrAnnotation = error()
        @JvmField
        val InvalidModuleSignature = error()
        @JvmField
        val ModuleMustBeStatic = error()
        @JvmField
        val CannotInvokeModuleFunctions = error()
        @JvmField
        val KeyOverloadMustHave1TypeParameter = error()
        @JvmField
        val KeyOverloadMustHaveKeyParam = error()

        @JvmField
        val MustBeABehavior = error()

        @JvmField
        val MustBeAQualifier = error()

        @JvmField
        val MustBeAScope = error()

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
            InjektErrors.MustBeStaticProperty,
            "Must be a top level property"
        )
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
            InjektErrors.NeedsPrimaryConstructorOrAnnotation,
            "Class needs a primary constructor or a constructor must be annotated with @InjektConstructor"
        )
        map.put(
            InjektErrors.InvalidModuleSignature,
            "@Module functions must take ComponentBuilder as a parameter or extension receiver and must return unit"
        )
        map.put(
            InjektErrors.ModuleMustBeStatic,
            "@Module functions must be callable from a static context"
        )
        map.put(
            InjektErrors.CannotInvokeModuleFunctions,
            "@Module functions cannot be invoked"
        )
        map.put(
            InjektErrors.KeyOverloadMustHave1TypeParameter,
            "@KeyOverload function must have exactly 1 type parameter"
        )
        map.put(
            InjektErrors.KeyOverloadMustHaveKeyParam,
            "@KeyOverload function must have key: Key<T> as first parameter"
        )
        map.put(
            InjektErrors.MustBeABehavior,
            "@BehaviorMarker property must be of type Behavior"
        )
        map.put(
            InjektErrors.MustBeAQualifier,
            "@QualifierMarker property must be of type Qualifier"
        )
        map.put(
            InjektErrors.MustBeAScope,
            "@ScopeMarker property must be of type Scope"
        )
    }

}
