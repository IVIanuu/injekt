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

import com.ivianuu.injekt.compiler.resolution.CallContext
import com.ivianuu.injekt.compiler.resolution.GivenGraph
import com.ivianuu.injekt.compiler.resolution.GivenRequest
import com.ivianuu.injekt.compiler.resolution.ResolutionResult
import com.ivianuu.injekt.compiler.resolution.callContext
import com.ivianuu.injekt.compiler.resolution.isFunctionType
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
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext

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
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(it, "non @Given parameter on @Given declaration")
                }

        @JvmField
        val GIVEN_CLASS_WITH_GIVEN_CONSTRUCTOR =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "class cannot be marked with @Given if it has a @Given marked constructor") }

        @JvmField
        val CLASS_WITH_MULTIPLE_GIVEN_CONSTRUCTORS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "class cannot have multiple @Given marked constructors") }

        @JvmField
        val GIVEN_ANNOTATION_CLASS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "annotation class cannot be marked with @Given") }

        @JvmField
        val GIVEN_CONSTRUCTOR_ON_ANNOTATION_CLASS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "annotation class constructor cannot be marked with @Given") }

        @JvmField
        val GIVEN_ENUM_CLASS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "enum class cannot be marked with @Given") }

        @JvmField
        val GIVEN_ABSTRACT_CLASS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "abstract class cannot be marked with @Given") }

        @JvmField
        val GIVEN_TAILREC_FUNCTION =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "tailrec function cannot be marked with @Given") }

        @JvmField
        val NON_FOR_TYPE_KEY_TYPE_PARAMETER_AS_FOR_TYPE_KEY =
            DiagnosticFactory1.create<PsiElement, TypeParameterDescriptor>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "cannot use {0} as @ForTypeKey type argument",
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
        val MULTIPLE_GIVEN_CONSTRAINTS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "a declaration may have only one @Given type constraint"
                    )
                }

        @JvmField
        val GIVEN_CONSTRAINT_ON_NON_GIVEN_FUNCTION =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "a @Given type constraint is only supported on @Given functions"
                    )
                }

        @JvmField
        val DIVERGENT_GIVEN_CONSTRAINT =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "constrained given return type must not be assignable to the constraint type"
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

    fun ResolutionResult.Failure.unwrapDependencyFailure(
        request: GivenRequest
    ): Pair<GivenRequest, ResolutionResult.Failure> {
        return if (this is ResolutionResult.Failure.DependencyFailure)
            dependencyFailure.unwrapDependencyFailure(dependencyRequest)
        else request to this
    }

    val (unwrappedFailureRequest, unwrappedFailure) = failure.unwrapDependencyFailure(failureRequest)

    when (unwrappedFailure) {
        is ResolutionResult.Failure.CallContextMismatch -> {
            if (failure == unwrappedFailure) {
                appendLine("given argument ${unwrappedFailure.candidate.callableFqName}() of type ${failureRequest.type.render()} " +
                        "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
                        "is a ${unwrappedFailure.candidate.callContext.name.toLowerCase()} function " +
                        "but current call context is ${unwrappedFailure.actualCallContext.name.toLowerCase()}")
            } else {
                appendLine("call context mismatch")
            }
        }
        is ResolutionResult.Failure.CandidateAmbiguity -> {
            if (failure == unwrappedFailure) {
                appendLine("ambiguous given arguments:\n${unwrappedFailure.candidateResults.joinToString("\n") {
                    it.candidate.callableFqName.asString()
                }}\ndo all match type ${unwrappedFailureRequest.type.render()} for parameter " +
                        "${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}")
            } else {
                appendLine("ambiguous given arguments of type ${unwrappedFailureRequest.type.render()} " +
                        "for parameter ${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}")
            }
        }
        is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
        is ResolutionResult.Failure.NoCandidates,
        is ResolutionResult.Failure.DivergentGiven-> {
            appendLine("no given argument found of type " +
                    "${unwrappedFailureRequest.type.render()} for parameter " +
                    "${unwrappedFailureRequest.parameterName} of function " +
                    "${unwrappedFailureRequest.callableFqName}")
        }
    }


    if (failure is ResolutionResult.Failure.DependencyFailure) {
        appendLine("I found:")
        appendLine()

        fun printCall(
            request: GivenRequest,
            failure: ResolutionResult.Failure,
            callContext: CallContext
        ) {
            val isProvider = request.callableFqName.asString()
                .startsWith("com.ivianuu.injekt.") && request.callableFqName.asString()
                .endsWith("roviderOf")
            append("${request.callableFqName}")
            if (isProvider) {
                appendLine(" {")
            } else {
                appendLine("(")
            }
            withIndent {
                if (isProvider && unwrappedFailure is ResolutionResult.Failure.CallContextMismatch) {
                    appendLine("${indent()}/* ${callContext.name.toLowerCase()} call context */")
                }
                append(indent())
                if (!isProvider) {
                    append("${request.parameterName} = ")
                }
                if (failure is ResolutionResult.Failure.DependencyFailure) {
                    printCall(failure.dependencyRequest, failure.dependencyFailure,
                    if (isProvider) request.type.callContext else callContext)
                } else {
                    append("/* ")
                    when (failure) {
                        is ResolutionResult.Failure.CallContextMismatch -> {
                            append("${failure.candidate.callContext.name.toLowerCase()} call:")
                        }
                        is ResolutionResult.Failure.CandidateAmbiguity -> {
                            append("ambiguous: ${failure.candidateResults.joinToString(", ") {
                                it.candidate.callableFqName.asString() 
                            }} do match type ${request.type.render()}")
                        }
                        is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
                        is ResolutionResult.Failure.NoCandidates,
                        is ResolutionResult.Failure.DivergentGiven -> append("missing:")
                    }
                    append(" */ ")
                    if (failure is ResolutionResult.Failure.CallContextMismatch) {
                        appendLine("${failure.candidate.callableFqName}()")
                    } else {
                        appendLine("given<${request.type.render()}>()")
                    }
                }
            }
            append(indent())
            if (isProvider) {
                appendLine("}")
            } else {
                appendLine(")")
            }
        }

        withIndent {
            if (unwrappedFailure is ResolutionResult.Failure.CallContextMismatch) {
                appendLine("${indent()}/* ${scope.callContext.name.toLowerCase()} call context */")
            }
            append(indent())
            printCall(
                failureRequest,
                failure,
                if (failureRequest.type.isFunctionType) failureRequest.type.callContext
                else scope.callContext
            )
        }
        appendLine()

        when (unwrappedFailure) {
            is ResolutionResult.Failure.CallContextMismatch -> {
                appendLine("but call context was ${unwrappedFailure.actualCallContext.name.toLowerCase()}")
            }
            is ResolutionResult.Failure.CandidateAmbiguity -> {
                appendLine("but ${unwrappedFailure.candidateResults.joinToString("\n") {
                    it.candidate.callableFqName.asString()
                }}\ndo all match type ${unwrappedFailureRequest.type.render()}")
            }
            is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
            is ResolutionResult.Failure.DivergentGiven -> {
                appendLine("but given ${unwrappedFailure.candidate.callableFqName} " +
                        "produces a diverging search when trying to match type ${unwrappedFailureRequest.type.render()}")
            }
            is ResolutionResult.Failure.NoCandidates -> {
                appendLine("but no givens were found that match type ${unwrappedFailureRequest.type.render()}")
            }
        }
    }
}
