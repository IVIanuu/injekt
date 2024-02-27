@file:OptIn(InternalDiagnosticFactoryMethod::class)

package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.*

private val INJEKT_ERROR by error1<PsiElement, String>()

fun DiagnosticReporter.report(
  element: AbstractKtSourceElement,
  message: String,
  context: CheckerContext
) = report(INJEKT_ERROR.on(element, message, null), context)

fun InjectionResult.Error.render(): String = buildString {
  var indent = 0
  fun withIndent(block: () -> Unit) {
    indent++
    block()
    indent--
  }

  fun indent() = buildString {
    repeat(indent) { append("  ") }
  }

  val (unwrappedFailureRequest, unwrappedFailure) = failure.unwrapDependencyFailure(failureRequest)

  appendLine()

  when (unwrappedFailure) {
    is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch -> if (failure == unwrappedFailure)
      appendLine(
        "type parameter ${unwrappedFailure.parameter.fqName.shortName()} " +
            "of injectable ${unwrappedFailure.candidate.chainFqName}() of type ${failureRequest.type.renderToString()} " +
            "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
            "is reified but type argument " +
            "${unwrappedFailure.argument.fqName} is not reified."
      )
    else
      appendLine("type argument kind mismatch.")
    is ResolutionResult.Failure.CandidateAmbiguity -> appendLine(
      if (failure == unwrappedFailure)
        "ambiguous injectables:\n\n${
          unwrappedFailure.candidateResults.joinToString("\n") {
            it.candidate.chainFqName.asString()
          }
        }\n\ndo all match type ${unwrappedFailureRequest.type.renderToString()} for parameter " +
            "${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}."
      else
        "ambiguous injectables of type ${unwrappedFailureRequest.type.renderToString()} " +
            "for parameter ${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}."
    )
    is ResolutionResult.Failure.WithCandidate.DependencyFailure -> throw AssertionError()
    is ResolutionResult.Failure.NoCandidates,
    is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> appendLine(
      "no injectable found of type " +
          "${unwrappedFailureRequest.type.renderToString()} for parameter " +
          "${unwrappedFailureRequest.parameterName} of function " +
          "${unwrappedFailureRequest.callableFqName}."
    )
  }.let { }

  if (failure is ResolutionResult.Failure.WithCandidate.DependencyFailure) {
    appendLine()
    appendLine("I found:")
    appendLine()

    fun printCall(
      request: InjectableRequest,
      failure: ResolutionResult.Failure,
      candidate: Injectable?
    ) {
      if (candidate !is LambdaInjectable) {
        append("${request.callableFqName}")

        if (request.callableTypeArguments.isNotEmpty())
          append(request.callableTypeArguments.values.joinToString(", ", "<", ">") {
            it.renderToString()
          })
      }
      when (candidate) {
        is LambdaInjectable -> {
          append("{ ")
          if (candidate.valueParameterSymbols.isNotEmpty()) {
            for ((index, parameter) in candidate.valueParameterSymbols.withIndex()) {
              val argument = candidate.type.unwrapTags().arguments[index]
              append("${parameter.name}: ${argument.renderToString()}")
              if (index != candidate.valueParameterSymbols.lastIndex)
                append(",")
            }

            append(" -> ")
          }
          appendLine()
        }
        else -> appendLine("(")
      }
      withIndent {
        append(indent())
        if (candidate !is LambdaInjectable)
          append("${request.parameterName} = ")
        if (failure is ResolutionResult.Failure.WithCandidate.DependencyFailure)
          printCall(
            failure.dependencyRequest, failure.dependencyFailure,
            failure.candidate
          )
        else {
          append("/* ")
          when (failure) {
            is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch ->
              append("${failure.parameter.fqName.shortName()} is reified: ")
            is ResolutionResult.Failure.CandidateAmbiguity -> append(
              "ambiguous: ${
                failure.candidateResults.joinToString(", ") {
                  it.candidate.chainFqName.asString()
                }
              } do match type ${request.type.renderToString()}"
            )
            is ResolutionResult.Failure.WithCandidate.DependencyFailure -> throw AssertionError()
            is ResolutionResult.Failure.NoCandidates,
            is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> append("missing:")
          }.let { }
          append(" */ ")
          appendLine("inject<${request.type.renderToString()}>()")
        }
      }
      append(indent())
      if (candidate is LambdaInjectable) appendLine("}")
      else appendLine(")")
    }

    withIndent {
      append(indent())
      printCall(failureRequest, failure, null)
    }
    appendLine()

    when (unwrappedFailure) {
      is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch ->
        appendLine("but type argument ${unwrappedFailure.argument.fqName} is not reified.")
      is ResolutionResult.Failure.CandidateAmbiguity -> appendLine(
        "but\n\n${
          unwrappedFailure.candidateResults.joinToString("\n") {
            it.candidate.chainFqName.asString()
          }
        }\n\ndo all match type ${unwrappedFailureRequest.type.renderToString()}."
      )
      is ResolutionResult.Failure.WithCandidate.DependencyFailure -> throw AssertionError()
      is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> appendLine(
        "but injectable ${unwrappedFailure.candidate.chainFqName} " +
            "produces a diverging search when trying to match type ${unwrappedFailureRequest.type.renderToString()}."
      )
      is ResolutionResult.Failure.NoCandidates ->
        appendLine("but no injectables were found that match type ${unwrappedFailureRequest.type.renderToString()}.")
    }.let { }
  }
}
