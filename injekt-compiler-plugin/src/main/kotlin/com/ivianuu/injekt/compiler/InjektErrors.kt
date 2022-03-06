/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*

interface InjektErrors {
  companion object {
    @JvmField val MAP = DiagnosticFactoryToRendererMap("Injekt")

    @JvmField val IMPORT_RENDERER = object : DiagnosticParameterRenderer<PsiElement> {
      override fun render(obj: PsiElement, renderingContext: RenderingContext): String =
        "'${obj.text.removeSurrounding("\"")}'"
    }

    @JvmField val UNRESOLVED_INJECTION =
      DiagnosticFactory1.create<PsiElement, InjectionGraph.Error>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "{0}",
            object : DiagnosticParameterRenderer<InjectionGraph.Error> {
              override fun render(
                obj: InjectionGraph.Error,
                renderingContext: RenderingContext,
              ): String = obj.render()
            }
          )
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

private fun InjectionGraph.Error.render(): String = buildString {
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
          "type parameter ${unwrappedFailure.parameter.name} " +
              "of injectable ${unwrappedFailure.candidate.callableFqName}() of type ${failureRequest.type.render()} " +
              "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
              "is reified but type argument " +
              "${unwrappedFailure.argument.fqNameSafe} is not reified."
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
          }\n\ndo all match type ${unwrappedFailureRequest.type.render()} for parameter " +
              "${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}."
        } else {
          "ambiguous injectables of type ${unwrappedFailureRequest.type.render()} " +
              "for parameter ${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}."
        }

      appendLine(errorMessage)
    }
    is ResolutionResult.Failure.WithCandidate.DependencyFailure -> throw AssertionError()
    is ResolutionResult.Failure.NoCandidates,
    is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> {
      appendLine(
        "no injectable found of type " +
          "${unwrappedFailureRequest.type.render()} for parameter " +
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
      if (candidate !is ProviderInjectable) {
        append("${request.callableFqName}")

        if (request.callableTypeArguments.isNotEmpty()) {
          append(request.callableTypeArguments.values.joinToString(", ", "<", ">") {
            it.render()
          })
        }
      }
      when (candidate) {
        is ProviderInjectable -> {
          append("{ ")
          if (candidate.parameterDescriptors.isNotEmpty()) {
            for ((index, parameter) in candidate.parameterDescriptors.withIndex()) {
              val argument = candidate.type.arguments[index]
              append("${parameter.name}: ${argument.render()}")
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
        if (candidate !is ProviderInjectable) {
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
              append("${failure.parameter.name} is reified: ")
            }
            is ResolutionResult.Failure.CandidateAmbiguity -> {
              append(
                "ambiguous: ${
                  failure.candidateResults.joinToString(", ") {
                    it.candidate.callableFqName.asString()
                  }
                } do match type ${request.type.render()}"
              )
            }
            is ResolutionResult.Failure.WithCandidate.DependencyFailure -> throw AssertionError()
            is ResolutionResult.Failure.NoCandidates,
            is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> append("missing:")
          }.let { }
          append(" */ ")
          appendLine("inject<${request.type.render()}>()")
        }
      }
      append(indent())
      if (candidate is ProviderInjectable) appendLine("}")
      else appendLine(")")
    }

    withIndent {
      append(indent())
      printCall(failureRequest, failure, null)
    }
    appendLine()

    when (unwrappedFailure) {
      is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch -> {
        appendLine("but type argument ${unwrappedFailure.argument.fqNameSafe} is not reified.")
      }
      is ResolutionResult.Failure.CandidateAmbiguity -> {
        appendLine(
          "but\n\n${
            unwrappedFailure.candidateResults.joinToString("\n") {
              it.candidate.callableFqName.asString()
            }
          }\n\ndo all match type ${unwrappedFailureRequest.type.render()}."
        )
      }
      is ResolutionResult.Failure.WithCandidate.DependencyFailure -> throw AssertionError()
      is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> {
        appendLine(
          "but injectable ${unwrappedFailure.candidate.callableFqName} " +
              "produces a diverging search when trying to match type ${unwrappedFailureRequest.type.render()}."
        )
      }
      is ResolutionResult.Failure.NoCandidates -> {
        appendLine("but no injectables were found that match type ${unwrappedFailureRequest.type.render()}.")
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
