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

import com.ivianuu.injekt.compiler.resolution.GivenGraph
import com.ivianuu.injekt.compiler.resolution.ResolutionResult
import com.ivianuu.injekt.compiler.resolution.render
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.name.Name

interface InjektErrors {
    companion object {
        @JvmField
        val MAP = DiagnosticFactoryToRendererMap("Injekt")

        @JvmField
        val UNRESOLVED_GIVEN =
            DiagnosticFactory1.create<PsiElement, GivenGraph.Error>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "{0}",
                        object : DiagnosticParameterRenderer<GivenGraph.Error> {
                            override fun render(
                                obj: GivenGraph.Error,
                                renderingContext: RenderingContext,
                            ): String = obj.render()
                        }
                    )
                }

        @JvmField
        val NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION =
            DiagnosticFactory1.create<PsiElement, Name>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "non @Given parameter on @{0} declaration",
                        Renderers.TO_STRING
                    )
                }

        @JvmField
        val GIVEN_CLASS_WITH_GIVEN_CONSTRUCTOR =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "class cannot be marked with @Given if it has a @Given marked constructor") }

        @JvmField
        val GIVEN_SUPER_TYPE_WITHOUT_GIVEN_CLASS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "class with a @Given super type must be marked with @Given or must have a @Given marked constructor") }

        @JvmField
        val CLASS_WITH_MULTIPLE_GIVEN_CONSTRUCTORS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "class cannot have multiple @Given marked constructors") }

        @JvmField
        val DECLARATION_WITH_MULTIPLE_CONTRIBUTIONS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "declaration may be only annotated with one contribution annotation") }

        @JvmField
        val NON_FOR_KEY_TYPE_PARAMETER_AS_FOR_KEY =
            DiagnosticFactory1.create<PsiElement, TypeParameterDescriptor>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "cannot use {0} as @ForKey type argument",
                        object : DiagnosticParameterRenderer<TypeParameterDescriptor> {
                            override fun render(
                                obj: TypeParameterDescriptor,
                                renderingContext: RenderingContext
                            ): String {
                                return obj.name.asString()
                            }
                        }
                    )
                }

        @JvmField
        val INTERCEPTOR_WITHOUT_FACTORY_PARAMETER =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "@Interceptor declaration must have one parameter which matches the return type." +
                                "E.g. intercept(factory: () -> Foo): Foo"
                    )
                }

        @JvmField
        val MACRO_WITHOUT_TYPE_PARAMETER =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "@Macro declaration must have at least one type parameter"
                    )
                }

        @JvmField
        val MACRO_WITHOUT_CONTRIBUTION =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "@Macro declaration must have 1 contribution annotation"
                    )
                }

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

private fun GivenGraph.Error.render(): String = buildString {
    var indent = 0
    fun withIndent(block: () -> Unit) {
        indent++
        block()
        indent--
    }

    fun indent() = buildString {
        repeat(indent) { append("    ") }
    }

    fun ResolutionResult.Failure.print() {
        when (this) {
            is ResolutionResult.Failure.CandidateAmbiguity -> {
                appendLine("${indent()}ambiguous given arguments of type ${request.type.render()} " +
                        "for parameter ${request.parameterName} of function ${request.callableFqName}:")
                withIndent {
                    candidateResults
                        .map { it.candidate }
                        .forEach { candidate ->
                            appendLine("${indent()}${candidate.callableFqName}")
                        }
                }
            }
            is ResolutionResult.Failure.CallContextMismatch -> {
                appendLine("${indent()} current call context is $actualCallContext but" +
                        " ${candidate.callableFqName} is ${candidate.callContext}")
            }
            is ResolutionResult.Failure.DivergentGiven -> {
                appendLine("${indent()}divergent given $request")
            }
            is ResolutionResult.Failure.CandidateFailures -> {
                appendLine("${indent()}given candidate of type ${request.type.render()} " +
                        "for parameter ${request.parameterName} of function ${request.callableFqName} has failures:")
                withIndent {
                    candidateFailure
                        .failure.print()
                }
            }
            is ResolutionResult.Failure.NoCandidates -> {
                appendLine("${indent()}no given argument found of type " +
                        "${request.type.render()} for parameter ${request.parameterName} of function ${request.callableFqName}")
            }
        }
    }

    failures
        .flatMap { it.value }
        .forEach { it.print() }
}