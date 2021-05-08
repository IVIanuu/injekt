/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.*
import org.jetbrains.kotlin.utils.addToStdlib.*

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
        val GIVEN_RECEIVER =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(it, "receiver cannot be marked as @Given because it is implicitly @Given")
                }

        @JvmField
        val GIVEN_ON_CLASS_WITH_PRIMARY_GIVEN_CONSTRUCTOR =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "class cannot be marked with @Given if it has a @Given primary constructor") }

        @JvmField
        val GIVEN_ANNOTATION_CLASS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "annotation class cannot be @Given") }

        @JvmField
        val GIVEN_ENUM_CLASS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "enum class cannot be @Given") }

        @JvmField
        val GIVEN_INNER_CLASS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "@Given class cannot be inner") }

        @JvmField
        val GIVEN_ABSTRACT_CLASS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "@Given class cannot be abstract") }

        @JvmField
        val GIVEN_INTERFACE =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "interface cannot be @Given") }

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
                            ): String = obj.name.asString()
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
        val GIVEN_CONSTRAINT_ON_NON_GIVEN_DECLARATION =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "a @Given type constraint is only supported on @Given functions and @Given classes"
                    )
                }

        @JvmField
        val QUALIFIER_WITH_VALUE_PARAMETERS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "qualifier cannot have value parameters"
                    )
                }

        @JvmField
        val QUALIFIER_ON_NON_CLASS_AND_NON_TYPE =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "only types, classes and class constructors can be annotated with a qualifier"
                    )
                }

        @JvmField
        val MALFORMED_GIVEN_IMPORT =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        object : DiagnosticRenderer<Diagnostic> {
                            override fun render(diagnostic: Diagnostic): String =
                                "Cannot read given import: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"
                        }
                    )
                }

        @JvmField
        val UNRESOLVED_GIVEN_IMPORT =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        object : DiagnosticRenderer<Diagnostic> {
                            override fun render(diagnostic: Diagnostic): String =
                                "Unresolved given import: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"
                        }
                    )
                }

        @JvmField
        val DUPLICATED_GIVEN_IMPORT =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        object : DiagnosticRenderer<Diagnostic> {
                            override fun render(diagnostic: Diagnostic): String =
                                "Duplicated given import: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"
                        }
                    )
                }

        @JvmField
        val UNUSED_GIVEN_IMPORT = DiagnosticFactory0.create<PsiElement>(Severity.WARNING)
            .also {
                MAP.put(it, object : DiagnosticRenderer<Diagnostic> {
                    override fun render(diagnostic: Diagnostic): String =
                        "Unused given import: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"
                })
            }

        @JvmField
        val DECLARATION_PACKAGE_GIVEN_IMPORT = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
            .also {
                MAP.put(it, object : DiagnosticRenderer<Diagnostic> {
                    override fun render(diagnostic: Diagnostic): String =
                        "Givens of the same package are automatically imported: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"
                })
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
        is ResolutionResult.Failure.TypeArgumentKindMismatch -> {
            if (failure == unwrappedFailure) {
                appendLine("type parameter ${unwrappedFailure.parameter.fqName.shortName()} " +
                        "of given argument ${unwrappedFailure.candidate.callableFqName}() of type ${failureRequest.type.render()} " +
                        "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
                        "is marked with ${unwrappedFailure.kind.readableName()} but type argument " +
                        "${unwrappedFailure.argument.fqName} is not marked with ${unwrappedFailure.kind.readableName()}")
            } else {
                appendLine("type argument kind mismatch")
            }
        }
        is ResolutionResult.Failure.CandidateAmbiguity -> {
            val errorMessage = unwrappedFailure.candidateResults
                .firstNotNullResult { result ->
                    result.candidate
                        .safeAs<CallableGivenNode>()
                        ?.callable
                        ?.let { callable ->
                            callable
                                .customErrorMessages
                                ?.ambiguousMessage
                                ?.formatErrorMessage(callable.type, callable.typeArguments)
                        }
                }
                ?: if (failure == unwrappedFailure) {
                    "ambiguous given arguments:\n${unwrappedFailure.candidateResults.joinToString("\n") {
                        it.candidate.callableFqName.asString()
                    }}\ndo all match type ${unwrappedFailureRequest.type.render()} for parameter " +
                            "${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}"
            } else {
                    "ambiguous given arguments of type ${unwrappedFailureRequest.type.render()} " +
                            "for parameter ${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}"
            }
            appendLine(errorMessage)
        }
        is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
        is ResolutionResult.Failure.NoCandidates,
        is ResolutionResult.Failure.DivergentGiven-> {
            val errorMessage = (unwrappedFailureRequest.customErrorMessages
                ?.notFoundMessage
                ?: unwrappedFailureRequest.type.classifier.customErrorMessages?.notFoundMessage)
                ?.formatErrorMessage(unwrappedFailureRequest.type, unwrappedFailureRequest.typeArguments)
                ?: "no given argument found of type " +
                "${unwrappedFailureRequest.type.render()} for parameter " +
                "${unwrappedFailureRequest.parameterName} of function " +
                "${unwrappedFailureRequest.callableFqName}"
            appendLine(errorMessage)
        }
    }.let {  }

    if (failure !is ResolutionResult.Failure.DependencyFailure) return@buildString

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
                    is ResolutionResult.Failure.TypeArgumentKindMismatch -> {
                        append("${failure.parameter.fqName.shortName()} is marked with ${failure.kind.readableName()}: ")
                    }
                    is ResolutionResult.Failure.CandidateAmbiguity -> {
                        append("ambiguous: ${failure.candidateResults.joinToString(", ") {
                            it.candidate.callableFqName.asString() 
                        }} do match type ${request.type.render()}")
                    }
                    is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
                    is ResolutionResult.Failure.NoCandidates,
                    is ResolutionResult.Failure.DivergentGiven -> append("missing:")
                }.let { }
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
        is ResolutionResult.Failure.TypeArgumentKindMismatch -> {
            appendLine("but type argument ${unwrappedFailure.argument.fqName} is not marked with ${unwrappedFailure.kind.readableName()}")
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
    }.let {  }
}

private fun ResolutionResult.Failure.TypeArgumentKindMismatch.TypeArgumentKind.readableName() = when (this) {
    ResolutionResult.Failure.TypeArgumentKindMismatch.TypeArgumentKind.REIFIED -> "reified"
    ResolutionResult.Failure.TypeArgumentKindMismatch.TypeArgumentKind.FOR_TYPE_KEY -> "@ForTypeKey"
}

private fun String.formatErrorMessage(
    type: TypeRef,
    typeArguments: Map<ClassifierRef, TypeRef>
): String {
    val arguments = (typeArguments + type.classifier.typeParameters
        .toMap(type.arguments))
        .map { "[${it.key.fqName}]" to it.value.render() }
    return arguments.fold(this) { acc, nextReplacement ->
        acc.replace(nextReplacement.first, nextReplacement.second)
    }
}
