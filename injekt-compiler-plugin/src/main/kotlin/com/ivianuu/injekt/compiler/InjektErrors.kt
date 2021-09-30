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

import com.ivianuu.injekt.compiler.resolution.CallContext
import com.ivianuu.injekt.compiler.resolution.InjectableRequest
import com.ivianuu.injekt.compiler.resolution.InjectionGraph
import com.ivianuu.injekt.compiler.resolution.ResolutionResult
import com.ivianuu.injekt.compiler.resolution.callContext
import com.ivianuu.injekt.compiler.resolution.isProviderFunctionType
import com.ivianuu.injekt.compiler.resolution.renderToString
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticRenderer
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext

interface InjektErrors {
  companion object {
    @JvmField
    val MAP = DiagnosticFactoryToRendererMap("Injekt")

    @JvmField
    val UNRESOLVED_INJECTION =
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

    @JvmField
    val INJECT_PARAMETER_ON_PROVIDE_DECLARATION =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(it, "parameters of a injectable are automatically treated as inject parameters")
        }

    @JvmField
    val PROVIDE_PARAMETER_ON_PROVIDE_DECLARATION =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(it, "parameters of a injectable are automatically provided")
        }

    @JvmField
    val INJECT_RECEIVER = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
      .also {
        MAP.put(it, "receiver cannot be injected")
      }

    @JvmField
    val PROVIDE_RECEIVER = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
      .also {
        MAP.put(it, "receiver is automatically provided")
      }

    @JvmField
    val PROVIDE_ON_CLASS_WITH_PRIMARY_PROVIDE_CONSTRUCTOR =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "class cannot be marked with @Provide if it has a @Provide primary constructor"
          )
        }

    @JvmField
    val PROVIDE_ANNOTATION_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "annotation class cannot be injectable") }

    @JvmField
    val PROVIDE_ENUM_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "enum class cannot be injectable") }

    @JvmField
    val PROVIDE_INNER_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "inner class cannot be injectable") }

    @JvmField
    val PROVIDE_ABSTRACT_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "abstract class cannot be injectable") }

    @JvmField
    val PROVIDE_INTERFACE =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "interface cannot be injectable") }

    @JvmField
    val PROVIDE_VARIABLE_MUST_BE_INITIALIZED = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
      .also {
        MAP.put(it, object : DiagnosticRenderer<Diagnostic> {
          override fun render(diagnostic: Diagnostic): String =
            "injectable variable must be initialized, delegated or marked with lateinit"

          override fun renderParameters(diagnostic: Diagnostic): Array<out Any?> = emptyArray()
        })
      }

    @JvmField
    val MULTIPLE_SPREADS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "a declaration may have only one @Spread type parameter"
          )
        }

    @JvmField
    val SPREAD_ON_NON_PROVIDE_DECLARATION =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "a @Spread type parameter is only supported on @Provide functions and @Provide classes"
          )
        }

    @JvmField
    val TAG_WITH_VALUE_PARAMETERS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "tag cannot have value parameters"
          )
        }

    @JvmField
    val TAG_ON_NON_CLASS_AND_NON_TYPE =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            "only types, classes and class constructors can be annotated with a tag"
          )
        }

    @JvmField
    val MALFORMED_INJECTABLE_IMPORT =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            object : DiagnosticRenderer<Diagnostic> {
              override fun render(diagnostic: Diagnostic): String =
                "cannot read injectable import: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"

              override fun renderParameters(diagnostic: Diagnostic): Array<out Any?> = emptyArray()
            }
          )
        }

    @JvmField
    val UNRESOLVED_INJECTABLE_IMPORT =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            object : DiagnosticRenderer<Diagnostic> {
              override fun render(diagnostic: Diagnostic): String =
                "unresolved injectable import: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"

              override fun renderParameters(diagnostic: Diagnostic): Array<out Any?> = emptyArray()
            }
          )
        }

    @JvmField
    val DUPLICATED_INJECTABLE_IMPORT =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            object : DiagnosticRenderer<Diagnostic> {
              override fun render(diagnostic: Diagnostic): String =
                "duplicated injectable import: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"

              override fun renderParameters(diagnostic: Diagnostic): Array<out Any?> = emptyArray()
            }
          )
        }

    @JvmField
    val UNUSED_INJECTABLE_IMPORT = DiagnosticFactory0.create<PsiElement>(Severity.WARNING)
      .also {
        MAP.put(it, object : DiagnosticRenderer<Diagnostic> {
          override fun render(diagnostic: Diagnostic): String =
            "unused injectable import: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"

          override fun renderParameters(diagnostic: Diagnostic): Array<out Any?> = emptyArray()
        })
      }

    @JvmField
    val DECLARATION_PACKAGE_INJECTABLE_IMPORT = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
      .also {
        MAP.put(it, object : DiagnosticRenderer<Diagnostic> {
          override fun render(diagnostic: Diagnostic): String =
            "injectables of the same package are automatically imported: '${
              diagnostic.psiElement.text.removeSurrounding("\"")
            }'"

          override fun renderParameters(diagnostic: Diagnostic): Array<out Any?> = emptyArray()
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

private fun InjectionGraph.Error.render(): String = buildString {
  var indent = 0
  fun withIndent(block: () -> Unit) {
    indent++
    block()
    indent--
  }

  fun indent() = buildString {
    repeat(indent) { append("    ") }
  }

  val (unwrappedFailureRequest, unwrappedFailure) = failure.unwrapDependencyFailure(failureRequest)

  when (unwrappedFailure) {
    is ResolutionResult.Failure.WithCandidate.CallContextMismatch -> {
      if (failure == unwrappedFailure) {
        appendLine(
          "injectable ${unwrappedFailure.candidate.callableFqName}() of type ${failureRequest.type.renderToString()} " +
              "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
              "is a ${unwrappedFailure.candidate.callContext.name.toLowerCase()} function " +
              "but current call context is ${unwrappedFailure.actualCallContext.name.toLowerCase()}"
        )
      } else {
        appendLine("call context mismatch")
      }
    }
    is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch -> {
      if (failure == unwrappedFailure) {
        appendLine(
          "type parameter ${unwrappedFailure.parameter.fqName.shortName()} " +
              "of injectable ${unwrappedFailure.candidate.callableFqName}() of type ${failureRequest.type.renderToString()} " +
              "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
              "is reified but type argument " +
              "${unwrappedFailure.argument.fqName} is not reified"
        )
      } else {
        appendLine("type argument kind mismatch")
      }
    }
    is ResolutionResult.Failure.CandidateAmbiguity -> {
      if (failure == unwrappedFailure) {
        appendLine(
          "ambiguous injectables:\n${
            unwrappedFailure.candidateResults.joinToString("\n") {
              it.candidate.callableFqName.asString()
            }
          }\ndo all match type ${unwrappedFailureRequest.type.renderToString()} for parameter " +
              "${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}"
        )
      } else {
        appendLine(
          "ambiguous injectables of type ${unwrappedFailureRequest.type.renderToString()} " +
              "for parameter ${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}"
        )
      }
    }
    is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
    is ResolutionResult.Failure.NoCandidates,
    is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> {
      appendLine(
        "no injectable found of type " +
            "${unwrappedFailureRequest.type.renderToString()} for parameter " +
            "${unwrappedFailureRequest.parameterName} of function " +
            "${unwrappedFailureRequest.callableFqName}"
      )
    }
  }.let { }

  if (failure is ResolutionResult.Failure.DependencyFailure) {
    appendLine("I found:")
    appendLine()

    fun printCall(
      request: InjectableRequest,
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
        if (isProvider &&
          unwrappedFailure is ResolutionResult.Failure.WithCandidate.CallContextMismatch) {
          appendLine("${indent()}/* ${callContext.name.toLowerCase()} call context */")
        }
        append(indent())
        if (!isProvider) {
          append("${request.parameterName} = ")
        }
        if (failure is ResolutionResult.Failure.DependencyFailure) {
          printCall(
            failure.dependencyRequest, failure.dependencyFailure,
            if (isProvider) request.type.callContext else callContext
          )
        } else {
          append("/* ")
          when (failure) {
            is ResolutionResult.Failure.WithCandidate.CallContextMismatch -> {
              append("${failure.candidate.callContext.name.toLowerCase()} call:")
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
            is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
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
      if (isProvider) {
        appendLine("}")
      } else {
        appendLine(")")
      }
    }

    withIndent {
      if (unwrappedFailure is ResolutionResult.Failure.WithCandidate.CallContextMismatch) {
        appendLine("${indent()}/* ${scope.callContext.name.toLowerCase()} call context */")
      }
      append(indent())
      printCall(
        failureRequest,
        failure,
        if (failureRequest.type.isProviderFunctionType) failureRequest.type.callContext
        else scope.callContext
      )
    }
    appendLine()

    when (unwrappedFailure) {
      is ResolutionResult.Failure.WithCandidate.CallContextMismatch -> {
        appendLine("but call context was ${unwrappedFailure.actualCallContext.name.toLowerCase()}")
      }
      is ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch -> {
        appendLine("but type argument ${unwrappedFailure.argument.fqName} is not reified")
      }
      is ResolutionResult.Failure.CandidateAmbiguity -> {
        appendLine(
          "but ${
            unwrappedFailure.candidateResults.joinToString("\n") {
              it.candidate.callableFqName.asString()
            }
          }\ndo all match type ${unwrappedFailureRequest.type.renderToString()}"
        )
      }
      is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
      is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> {
        appendLine(
          "but injectable ${unwrappedFailure.candidate.callableFqName} " +
              "produces a diverging search when trying to match type ${unwrappedFailureRequest.type.renderToString()}"
        )
      }
      is ResolutionResult.Failure.NoCandidates -> {
        appendLine("but no injectables were found that match type ${unwrappedFailureRequest.type.renderToString()}")
      }
    }.let { }
  }
}

fun ResolutionResult.Failure.unwrapDependencyFailure(
  request: InjectableRequest
): Pair<InjectableRequest, ResolutionResult.Failure> =
  if (this is ResolutionResult.Failure.DependencyFailure)
    dependencyFailure.unwrapDependencyFailure(dependencyRequest)
  else request to this
