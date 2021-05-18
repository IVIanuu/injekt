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
          MAP.put(it, "parameters of a provide declaration are automatically treated as inject parameters")
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
        .also { MAP.put(it, "annotation class cannot be provided") }

    @JvmField
    val PROVIDE_ENUM_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "enum class cannot be provided") }

    @JvmField
    val PROVIDE_INNER_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "inner class cannot be provided") }

    @JvmField
    val PROVIDE_ABSTRACT_CLASS =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "abstract class cannot be provided") }

    @JvmField
    val PROVIDE_INTERFACE =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also { MAP.put(it, "interface cannot be provided") }

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
    val MALFORMED_INJECTABLE_IMPORT =
      DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
        .also {
          MAP.put(
            it,
            object : DiagnosticRenderer<Diagnostic> {
              override fun render(diagnostic: Diagnostic): String =
                "cannot read injectable import: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"
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
            }
          )
        }

    @JvmField
    val UNUSED_INJECTABLE_IMPORT = DiagnosticFactory0.create<PsiElement>(Severity.WARNING)
      .also {
        MAP.put(it, object : DiagnosticRenderer<Diagnostic> {
          override fun render(diagnostic: Diagnostic): String =
            "unused injectable import: '${diagnostic.psiElement.text.removeSurrounding("\"")}'"
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
        })
      }

    @JvmField
    val TYPE_ALIAS_MODULE_NOT_OBJECT = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
      .also {
        MAP.put(it, object : DiagnosticRenderer<Diagnostic> {
          override fun render(diagnostic: Diagnostic): String =
            "typealias module must be an object"
        })
      }

    @JvmField
    val TYPE_ALIAS_MODULE_NOT_DECLARED_IN_SAME_COMPILATION_UNIT = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
      .also {
        MAP.put(it, object : DiagnosticRenderer<Diagnostic> {
          override fun render(diagnostic: Diagnostic): String =
            "typealias module must be declared in the same compilation unit"
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

  fun ResolutionResult.Failure.unwrapDependencyFailure(
    request: InjectableRequest
  ): Pair<InjectableRequest, ResolutionResult.Failure> {
    return if (this is ResolutionResult.Failure.DependencyFailure)
      dependencyFailure.unwrapDependencyFailure(dependencyRequest)
    else request to this
  }

  val (unwrappedFailureRequest, unwrappedFailure) = failure.unwrapDependencyFailure(failureRequest)

  when (unwrappedFailure) {
    is ResolutionResult.Failure.CallContextMismatch -> {
      if (failure == unwrappedFailure) {
        appendLine(
          "injectable ${unwrappedFailure.candidate.callableFqName}() of type ${failureRequest.type.render()} " +
              "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
              "is a ${unwrappedFailure.candidate.callContext.name.toLowerCase()} function " +
              "but current call context is ${unwrappedFailure.actualCallContext.name.toLowerCase()}"
        )
      } else {
        appendLine("call context mismatch")
      }
    }
    is ResolutionResult.Failure.TypeArgumentKindMismatch -> {
      if (failure == unwrappedFailure) {
        appendLine(
          "type parameter ${unwrappedFailure.parameter.fqName.shortName()} " +
              "of injectable ${unwrappedFailure.candidate.callableFqName}() of type ${failureRequest.type.render()} " +
              "for parameter ${failureRequest.parameterName} of function ${failureRequest.callableFqName} " +
              "is marked with ${unwrappedFailure.kind.readableName()} but type argument " +
              "${unwrappedFailure.argument.fqName} is not marked with ${unwrappedFailure.kind.readableName()}"
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
          }\ndo all match type ${unwrappedFailureRequest.type.render()} for parameter " +
              "${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}"
        )
      } else {
        appendLine(
          "ambiguous injectables of type ${unwrappedFailureRequest.type.render()} " +
              "for parameter ${unwrappedFailureRequest.parameterName} of function ${unwrappedFailureRequest.callableFqName}"
        )
      }
    }
    is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
    is ResolutionResult.Failure.NoCandidates,
    is ResolutionResult.Failure.DivergentInjectable -> {
      appendLine(
        "no injectable found of type " +
            "${unwrappedFailureRequest.type.render()} for parameter " +
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
        if (isProvider && unwrappedFailure is ResolutionResult.Failure.CallContextMismatch) {
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
            is ResolutionResult.Failure.CallContextMismatch -> {
              append("${failure.candidate.callContext.name.toLowerCase()} call:")
            }
            is ResolutionResult.Failure.TypeArgumentKindMismatch -> {
              append("${failure.parameter.fqName.shortName()} is marked with ${failure.kind.readableName()}: ")
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
            is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
            is ResolutionResult.Failure.NoCandidates,
            is ResolutionResult.Failure.DivergentInjectable -> append("missing:")
          }.let { }
          append(" */ ")
          if (failure is ResolutionResult.Failure.CallContextMismatch) {
            appendLine("${failure.candidate.callableFqName}()")
          } else {
            appendLine("inject<${request.type.render()}>()")
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
        appendLine(
          "but ${
            unwrappedFailure.candidateResults.joinToString("\n") {
              it.candidate.callableFqName.asString()
            }
          }\ndo all match type ${unwrappedFailureRequest.type.render()}"
        )
      }
      is ResolutionResult.Failure.DependencyFailure -> throw AssertionError()
      is ResolutionResult.Failure.DivergentInjectable -> {
        appendLine(
          "but injectable ${unwrappedFailure.candidate.callableFqName} " +
              "produces a diverging search when trying to match type ${unwrappedFailureRequest.type.render()}"
        )
      }
      is ResolutionResult.Failure.NoCandidates -> {
        appendLine("but no injectables were found that match type ${unwrappedFailureRequest.type.render()}")
      }
    }.let { }
  }
}

private fun ResolutionResult.Failure.TypeArgumentKindMismatch.TypeArgumentKind.readableName() =
  when (this) {
    ResolutionResult.Failure.TypeArgumentKindMismatch.TypeArgumentKind.REIFIED -> "reified"
    ResolutionResult.Failure.TypeArgumentKindMismatch.TypeArgumentKind.FOR_TYPE_KEY -> "@ForTypeKey"
  }
