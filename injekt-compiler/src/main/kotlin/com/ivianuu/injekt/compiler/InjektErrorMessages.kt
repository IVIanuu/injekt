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
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.BindingTrace

fun report(
    descriptor: DeclarationDescriptor,
    trace: BindingTrace,
    diagnostic: (PsiElement) -> DiagnosticFactory0<PsiElement>
) {
    descriptor.findPsi()?.let {
        val factory = diagnostic(it)
        trace.report(factory.on(it))
    }
}

val EitherFactoryOrSingle = error()
val OnlyOneScope = error()
val NeedsAQualifierCompanionObject = error()
val NeedsACompanionObject = error()
val ParamCannotBeNamed = error()
val OnlyOneInjektConstructor = error()
val NeedsPrimaryConstructorOrAnnotation = error()
val SingleNeedsScope = error()

private fun error() = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)

object InjektErrorMessages : DefaultErrorMessages.Extension {
    private val map = DiagnosticFactoryToRendererMap("Injekt")
    override fun getMap(): DiagnosticFactoryToRendererMap = map

    init {
        map.put(
            EitherFactoryOrSingle,
            "Can only have one of @Factory or @Single"
        )
        map.put(
            OnlyOneScope,
            "Can only have one 1 scope ammptatopm"
        )
        map.put(
            NeedsACompanionObject,
            "Needs a companion object"
        )
        map.put(
            NeedsAQualifierCompanionObject,
            "Needs a companion object which implements Qualifier"
        )
        map.put(
            ParamCannotBeNamed,
            "Parameters cannot be named"
        )
        map.put(
            OnlyOneInjektConstructor,
            "Only one constructor can be annotated"
        )
        map.put(
            NeedsPrimaryConstructorOrAnnotation,
            "Class needs a primary constructor or a constructor must be annotated with @InjektConstructor"
        )
        map.put(
            SingleNeedsScope,
            "@Single annotated classes needs a scope"
        )
    }
}
