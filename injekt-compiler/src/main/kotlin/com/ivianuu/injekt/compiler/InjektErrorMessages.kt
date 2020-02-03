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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.BindingTrace

fun report(
    descriptor: DeclarationDescriptor,
    trace: BindingTrace,
    diagnostic: (PsiElement) -> DiagnosticFactory0<PsiElement>
) {
    descriptor.findPsi()?.let {
        val factory = diagnostic(it)
        trace.reportFromPlugin(factory.on(it), InjektErrorMessages)
    }
}

val MustBeAObject = error()
val NeedsACompanionObject = error()
val NeedsPrimaryConstructorOrAnnotation = error()
val OnlyOneInjektConstructor = error()
val OnlyOneKind = error()
val OnlyOneName = error()
val OnlyOneScope = error()
val ParamCannotBeNamed = error()

private fun error() = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)

object InjektErrorMessages : DefaultErrorMessages.Extension {
    private val map = DiagnosticFactoryToRendererMap("Injekt")
    override fun getMap(): DiagnosticFactoryToRendererMap = map

    init {
        map.put(
            MustBeAObject,
            "Must be a object"
        )
        map.put(
            NeedsACompanionObject,
            "Needs a companion object"
        )
        map.put(
            NeedsPrimaryConstructorOrAnnotation,
            "Class needs a primary constructor or a constructor must be annotated with @InjektConstructor"
        )
        map.put(
            OnlyOneInjektConstructor,
            "Only one constructor can be annotated"
        )
        map.put(
            OnlyOneKind,
            "Can only have one @KindMarker annotated annotation"
        )
        map.put(
            OnlyOneScope,
            "Can only have one 1 scope annotation"
        )
        map.put(
            OnlyOneName,
            "Can only have 1 name annotation"
        )
        map.put(
            ParamCannotBeNamed,
            "Parameters cannot be named"
        )
    }
}
