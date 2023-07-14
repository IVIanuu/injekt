/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.Injectable
import com.ivianuu.injekt.compiler.resolution.InjectableRequest
import com.ivianuu.injekt.compiler.resolution.InjectionResult
import com.ivianuu.injekt.compiler.resolution.LambdaInjectable
import com.ivianuu.injekt.compiler.resolution.ResolutionResult
import com.ivianuu.injekt.compiler.resolution.renderToString
import com.ivianuu.injekt.compiler.resolution.unwrapDependencyFailure
import com.ivianuu.injekt.compiler.resolution.unwrapTags
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext

interface InjektErrors {
  companion object {
    @JvmField val MAP = DiagnosticFactoryToRendererMap("Injekt")

    @JvmField val INJEKT_ERROR = DiagnosticFactory1.create<PsiElement, String>(Severity.ERROR)
      .also {
        MAP.put(
          it,
          "{0}",
          object : DiagnosticParameterRenderer<String> {
            override fun render(
              obj: String,
              renderingContext: RenderingContext,
            ): String = obj
          }
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

fun Context.reportError(element: PsiElement, message: String) {
  trace!!.report(InjektErrors.INJEKT_ERROR.on(element, message))
}

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
    is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch -> {
      if (failure == unwrappedFailure) {
        appendLine(
          "type parameter ${unwrappedFailure.parameter.fqName.shortName()} " +
              "of injectable ${unwrappedFailure.candidate.callableFqName}() of type ${failureRequest.type.renderToString()} " +
              "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
              "is reified but type argument " +
              "${unwrappedFailure.argument.fqName} is not reified."
        )
      } else {
        appendLine("type argument kind mismatch.")
      }
    }
    is ResolutionResult.Failure.CandidateAmbiguity -> {
      val errorMessage = if (failure == unwrappedFailure) {
          "ambiguous injectables:\n\n${
            unwrappedFailure.candidateResults.joinToString("\n") {
              it.candidate.callableFqName.asString()
            }
          }\n\ndo all match type ${unwrappedFailureRequest.type.renderToString()} for parameter " +
              "${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}."
        } else {
          "ambiguous injectables of type ${unwrappedFailureRequest.type.renderToString()} " +
              "for parameter ${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}."
        }

      appendLine(errorMessage)
    }
    is ResolutionResult.Failure.WithCandidate.DependencyFailure -> throw AssertionError()
    is ResolutionResult.Failure.NoCandidates,
    is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> {
      appendLine(
        "no injectable found of type " +
          "${unwrappedFailureRequest.type.renderToString()} for parameter " +
          "${unwrappedFailureRequest.parameterName} of function " +
          "${unwrappedFailureRequest.callableFqName}."
      )
    }
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

        if (request.callableTypeArguments.isNotEmpty()) {
          append(request.callableTypeArguments.values.joinToString(", ", "<", ">") {
            it.renderToString()
          })
        }
      }
      when (candidate) {
        is LambdaInjectable -> {
          append("{ ")
          if (candidate.parameterDescriptors.isNotEmpty()) {
            for ((index, parameter) in candidate.parameterDescriptors.withIndex()) {
              val argument = candidate.type.unwrapTags().arguments[index]
              append("${parameter.name}: ${argument.renderToString()}")
              if (index != candidate.parameterDescriptors.lastIndex)
                append(",")
            }

            append(" -> ")
          }
          appendLine()
        }
        else -> {
          appendLine("(")
        }
      }
      withIndent {
        append(indent())
        if (candidate !is LambdaInjectable) {
          append("${request.parameterName} = ")
        }
        if (failure is ResolutionResult.Failure.WithCandidate.DependencyFailure) {
          printCall(
            failure.dependencyRequest, failure.dependencyFailure,
            failure.candidate
          )
        } else {
          append("/* ")
          when (failure) {
            is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch -> {
              append("${failure.parameter.fqName.shortName()} is reified: ")
            }
            is ResolutionResult.Failure.CandidateAmbiguity -> {
              append(
                "ambiguous: ${
                  failure.candidateResults.joinToString(", ") {
                    it.candidate.callableFqName.asString()
                  }
                } do match type ${request.type.renderToString()}"
              )
            }
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
      is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch -> {
        appendLine("but type argument ${unwrappedFailure.argument.fqName} is not reified.")
      }
      is ResolutionResult.Failure.CandidateAmbiguity -> {
        appendLine(
          "but\n\n${
            unwrappedFailure.candidateResults.joinToString("\n") {
              it.candidate.callableFqName.asString()
            }
          }\n\ndo all match type ${unwrappedFailureRequest.type.renderToString()}."
        )
      }
      is ResolutionResult.Failure.WithCandidate.DependencyFailure -> throw AssertionError()
      is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> {
        appendLine(
          "but injectable ${unwrappedFailure.candidate.callableFqName} " +
              "produces a diverging search when trying to match type ${unwrappedFailureRequest.type.renderToString()}."
        )
      }
      is ResolutionResult.Failure.NoCandidates -> {
        appendLine("but no injectables were found that match type ${unwrappedFailureRequest.type.renderToString()}.")
      }
    }.let { }
  }
}
