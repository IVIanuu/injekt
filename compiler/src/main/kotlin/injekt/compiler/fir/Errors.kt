/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.compiler.fir

import injekt.compiler.resolution.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.*

private val psiElementClass by lazy {
  try {
    Class.forName("org.jetbrains.kotlin.com.intellij.psi.PsiElement")
  } catch (_: ClassNotFoundException) {
    Class.forName("com.intellij.psi.PsiElement")
  }
    .kotlin
}

private object InjektErrors : KtDiagnosticsContainer() {
  override fun getRendererFactory(): BaseDiagnosticRendererFactory = InjektErrorMessages
}

private object InjektErrorMessages : BaseDiagnosticRendererFactory() {
  override val MAP by KtDiagnosticFactoryToRendererMap("injekt") { map ->
    map.put(INJEKT_ERROR, message = "{0}", null)
  }
}

private fun <A> error1(
  positioningStrategy: AbstractSourceElementPositioningStrategy =
    SourceElementPositioningStrategies.DEFAULT
): DiagnosticFactory1DelegateProvider<A> = DiagnosticFactory1DelegateProvider<A>(
  severity = Severity.ERROR,
  positioningStrategy = positioningStrategy,
  psiType = psiElementClass,
  container = InjektErrors
)

val INJEKT_ERROR by error1<String>()

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

  val (unwrappedFailureRequest, unwrappedFailure) =
    failure.unwrapDependencyFailure(failureRequest)

  appendLine()

  when (unwrappedFailure) {
    is ResolutionResult.Failure.WithCandidate.CallContextMismatch -> {
      if (failure == unwrappedFailure) {
        appendLine(
          "injectable ${unwrappedFailure.candidate.chainFqName}() of type ${failureRequest.type.renderToString()} " +
              "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
              "is a ${unwrappedFailure.candidate.callContext.name.lowercase()} function " +
              "but current call context is ${unwrappedFailure.actualCallContext.name.lowercase()}."
        )
      } else {
        appendLine("call context mismatch.")
      }
    }
    is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch -> if (failure == unwrappedFailure)
      appendLine(
        "type parameter ${unwrappedFailure.parameter.fqName.shortName()} " +
            "of injectable ${unwrappedFailure.candidate.chainFqName}() of type ${failureRequest.type.renderToString()} " +
            "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
            "is reified but type argument ${unwrappedFailure.argument.fqName} is not reified."
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
      candidate: Injectable?,
      callContext: CallContext
    ) {
      if (candidate is LambdaInjectable) {
        when (candidate.type.callContext) {
          CallContext.DEFAULT -> {}
          CallContext.COMPOSABLE -> append("@Composable ")
          CallContext.SUSPEND -> append("suspend ")
        }
      } else {
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
        if (candidate is LambdaInjectable &&
          unwrappedFailure is ResolutionResult.Failure.WithCandidate.CallContextMismatch)
          appendLine("${indent()}/* ${candidate.dependencyScope?.callContext?.name?.lowercase()} call context */")

        append(indent())
        if (candidate !is LambdaInjectable)
          append("${request.parameterName} = ")
        if (failure is ResolutionResult.Failure.WithCandidate.DependencyFailure)
          printCall(
            failure.dependencyRequest, failure.dependencyFailure,
            failure.candidate,
            if (candidate is LambdaInjectable) request.type.callContext else callContext
          )
        else {
          append("/* ")
          when (failure) {
            is ResolutionResult.Failure.WithCandidate.CallContextMismatch ->
              append("${failure.candidate.callContext.name.lowercase()} call " +
                  "${failure.candidate.chainFqName}(...)")
            is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch ->
              append("${failure.parameter.fqName.shortName()} is reified")
            is ResolutionResult.Failure.CandidateAmbiguity -> append(
              "ambiguous: ${
                failure.candidateResults.joinToString(", ") {
                  it.candidate.chainFqName.asString()
                }
              } do match type ${request.type.renderToString()}"
            )
            is ResolutionResult.Failure.WithCandidate.DependencyFailure -> throw AssertionError()
            is ResolutionResult.Failure.NoCandidates,
            is ResolutionResult.Failure.WithCandidate.DivergentInjectable ->
              append("missing ${request.type.renderToString()}")
          }.let { }
          appendLine(" */ ")
        }
      }
      append(indent())
      if (candidate is LambdaInjectable) appendLine("}")
      else appendLine(")")
    }

    withIndent {
      if (unwrappedFailure is ResolutionResult.Failure.WithCandidate.CallContextMismatch)
        appendLine("${indent()}/* ${scope.callContext.name.lowercase()} call context */")
      append(indent())
      printCall(
        failureRequest,
        failure,
        null,
        if (failureRequest.type.isNonKFunctionType()) failureRequest.type.callContext
        else scope.callContext
      )
    }
    appendLine()

    when (unwrappedFailure) {
      is ResolutionResult.Failure.WithCandidate.CallContextMismatch ->
        appendLine("but call context was ${unwrappedFailure.actualCallContext.name.lowercase()}.")
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
