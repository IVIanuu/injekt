/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.CallContext
import com.ivianuu.injekt.compiler.resolution.Injectable
import com.ivianuu.injekt.compiler.resolution.InjectableRequest
import com.ivianuu.injekt.compiler.resolution.InjectionResult
import com.ivianuu.injekt.compiler.resolution.ProviderInjectable
import com.ivianuu.injekt.compiler.resolution.ResolutionResult
import com.ivianuu.injekt.compiler.resolution.callContext
import com.ivianuu.injekt.compiler.resolution.isFunctionType
import com.ivianuu.injekt.compiler.resolution.renderToString
import com.ivianuu.injekt.compiler.resolution.unwrapTags
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import java.util.*

interface InjektErrors {
  companion object {
    @JvmField val MAP = DiagnosticFactoryToRendererMap("Injekt")

    @JvmField val IMPORT_RENDERER = object : DiagnosticParameterRenderer<PsiElement> {
      override fun render(obj: PsiElement, renderingContext: RenderingContext): String =
        "'${obj.text.removeSurrounding("\"")}'"
    }

    @JvmField val UNRESOLVED_INJECTION =
      DiagnosticFactory1.create<PsiElement, InjectionResult.Error>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "{0}",
            object : DiagnosticParameterRenderer<InjectionResult.Error> {
              override fun render(
                obj: InjectionResult.Error,
                renderingContext: RenderingContext,
              ): String = obj.render()
            }
          )
        }

    @JvmField val INJECT_PARAMETER_ON_PROVIDE_DECLARATION =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(it, "parameters of a injectable are automatically treated as inject parameters")
        }

    @JvmField val PROVIDE_PARAMETER_ON_PROVIDE_DECLARATION =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(it, "parameters of a injectable are automatically provided")
        }

    @JvmField val INJECT_RECEIVER = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
      .also {
        MAP.put(it, "receiver cannot be injected")
      }

    @JvmField val PROVIDE_RECEIVER = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
      .also {
        MAP.put(it, "receiver is automatically provided")
      }

    @JvmField val PROVIDE_ON_CLASS_WITH_PRIMARY_PROVIDE_CONSTRUCTOR =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "class cannot be marked with @Provide if it has a @Provide primary constructor"
          )
        }

    @JvmField val PROVIDE_ANNOTATION_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "annotation class cannot be injectable") }

    @JvmField val PROVIDE_ENUM_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "enum class cannot be injectable") }

    @JvmField val PROVIDE_INNER_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "inner class cannot be injectable") }

    @JvmField val PROVIDE_ABSTRACT_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "abstract class cannot be injectable") }

    @JvmField val PROVIDE_INTERFACE =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "interface cannot be injectable") }

    @JvmField val PROVIDE_VARIABLE_MUST_BE_INITIALIZED =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "injectable variable must be initialized, delegated or marked with lateinit"
          )
        }

    @JvmField val MULTIPLE_SPREADS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "a declaration may have only one @Spread type parameter"
          )
        }

    @JvmField val SPREAD_ON_NON_PROVIDE_DECLARATION =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "a @Spread type parameter is only supported on @Provide functions and @Provide classes"
          )
        }

    @JvmField val TAG_WITH_VALUE_PARAMETERS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "tag cannot have value parameters"
          )
        }

    @JvmField val MALFORMED_INJECTABLE_IMPORT =
      DiagnosticFactory1.create<PsiElement, PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "cannot read injectable import: {0}",
            IMPORT_RENDERER
          )
        }

    @JvmField val UNRESOLVED_INJECTABLE_IMPORT =
      DiagnosticFactory1.create<PsiElement, PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "unresolved injectable import: {0}",
            IMPORT_RENDERER
          )
        }

    @JvmField val DUPLICATED_INJECTABLE_IMPORT =
      DiagnosticFactory1.create<PsiElement, PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "duplicated injectable import: {0}",
            IMPORT_RENDERER
          )
        }

    @JvmField val UNUSED_INJECTABLE_IMPORT =
      DiagnosticFactory1.create<PsiElement, PsiElement>(Severity.WARNING)
        .also {
          MAP.put(
            it,
            "unused injectable import: {0}",
            IMPORT_RENDERER
          )
        }

    @JvmField val DECLARATION_PACKAGE_INJECTABLE_IMPORT =
      DiagnosticFactory1.create<PsiElement, PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "injectables of the same package are automatically imported: {0}",
            IMPORT_RENDERER
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

private fun InjectionResult.Error.render(): String = buildString {
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
    is ResolutionResult.Failure.WithCandidate.CallContextMismatch -> {
      if (failure == unwrappedFailure) {
        appendLine(
          "injectable ${unwrappedFailure.candidate.callableFqName}() of type ${failureRequest.type.renderToString()} " +
              "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
              "is a ${unwrappedFailure.candidate.callContext.name.lowercase(Locale.getDefault())} function " +
              "but current call context is ${unwrappedFailure.actualCallContext.name.lowercase(Locale.getDefault())}."
        )
      } else {
        appendLine("call context mismatch.")
      }
    }
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
      candidate: Injectable?,
      callContext: CallContext
    ) {
      if (candidate is ProviderInjectable) {
        when (candidate.type.callContext) {
          CallContext.DEFAULT -> {}
          CallContext.COMPOSABLE -> append("@Composable ")
          CallContext.SUSPEND -> append("suspend ")
        }
      } else {
        append("${request.callableFqName}")

        if (request.callableTypeArguments.isNotEmpty()) {
          append(request.callableTypeArguments.values.joinToString(", ", "<", ">") {
            it.renderToString()
          })
        }
      }
      when (candidate) {
        is ProviderInjectable -> {
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
        if (candidate is ProviderInjectable &&
          unwrappedFailure is ResolutionResult.Failure.WithCandidate.CallContextMismatch) {
          appendLine("${indent()}/* ${candidate.dependencyScopes.values.single().callContext.name.lowercase(Locale.getDefault())} call context */")
        }
        append(indent())
        if (candidate !is ProviderInjectable) {
          append("${request.parameterName} = ")
        }
        if (failure is ResolutionResult.Failure.WithCandidate.DependencyFailure) {
          printCall(
            failure.dependencyRequest, failure.dependencyFailure,
            failure.candidate,
            if (candidate is ProviderInjectable) request.type.callContext else callContext
          )
        } else {
          append("/* ")
          when (failure) {
            is ResolutionResult.Failure.WithCandidate.CallContextMismatch -> {
              append("${failure.candidate.callContext.name.lowercase(Locale.getDefault())} call:")
            }
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
          if (failure is ResolutionResult.Failure.WithCandidate.CallContextMismatch) {
            appendLine("${failure.candidate.callableFqName}()")
          } else {
            appendLine("inject<${request.type.renderToString()}>()")
          }
        }
      }
      append(indent())
      if (candidate is ProviderInjectable) appendLine("}")
      else appendLine(")")
    }

    withIndent {
      if (unwrappedFailure is ResolutionResult.Failure.WithCandidate.CallContextMismatch) {
        appendLine("${indent()}/* ${scope.callContext.name.lowercase(Locale.getDefault())} call context */")
      }
      append(indent())
      printCall(
        failureRequest,
        failure,
        null,
        if (failureRequest.type.isFunctionType) failureRequest.type.callContext
        else scope.callContext
      )
    }
    appendLine()

    when (unwrappedFailure) {
      is ResolutionResult.Failure.WithCandidate.CallContextMismatch -> {
        appendLine("but call context was ${unwrappedFailure.actualCallContext.name.lowercase(Locale.getDefault())}.")
      }
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

private fun ResolutionResult.Failure.unwrapDependencyFailure(
  request: InjectableRequest
): Pair<InjectableRequest, ResolutionResult.Failure> =
  if (this is ResolutionResult.Failure.WithCandidate.DependencyFailure)
    dependencyFailure.unwrapDependencyFailure(dependencyRequest)
  else request to this
