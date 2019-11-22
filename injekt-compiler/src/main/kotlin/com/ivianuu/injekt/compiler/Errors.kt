/*
 * Copyright 2019 Manuel Wrage
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
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.BindingTrace

enum class InjektError(val message: String) {
    // todo better names
    OnlyOneAnnotation("Can only have @Inject on the type or the constructor"),
    OnlyOneName("Can only have 1 name annotation"),
    EitherNameOrParam("Only one of @Param or @Name can be annotated per parameter"),
    CannotBePrivate("Must be public or internal");

    val factory
        get() = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
}

fun report(
    descriptor: DeclarationDescriptor,
    trace: BindingTrace,
    diagnostic: (PsiElement) -> InjektError
) {
    descriptor.findPsi()?.let {
        //trace.reportFromPlugin(diagnostic(it).factory.on(it), InjektErrors)
        msg { "on error ${diagnostic(it)}" }
    }
}

object InjektErrors : DefaultErrorMessages.Extension {
    private val map = DiagnosticFactoryToRendererMap("Injekt")
    override fun getMap(): DiagnosticFactoryToRendererMap = map
    init {
        InjektError.values()
            .forEach { map.put(it.factory, it.message) }
        Errors.Initializer.initializeFactoryNames(InjektErrors::class.java)
    }
}