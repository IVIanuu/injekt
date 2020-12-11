package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.GivenNode
import com.ivianuu.injekt.compiler.resolution.GivenRequest
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.render
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

interface InjektErrors {
    companion object {
        @JvmField
        val MAP = DiagnosticFactoryToRendererMap("Injekt")

        @JvmField
        val UNRESOLVED_GIVEN =
            DiagnosticFactory3.create<PsiElement, TypeRef, FqName, List<GivenRequest>>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "No given found for {0} required by {1}:{2}",
                        TypeRefRenderer,
                        Renderers.TO_STRING,
                        object : DiagnosticParameterRenderer<List<GivenRequest>> {
                            override fun render(
                                obj: List<GivenRequest>,
                                renderingContext: RenderingContext,
                            ): String = buildString {
                                obj.reversed().forEachIndexed { index, request ->
                                    append("'${request.type.render()}' ")
                                    if (index == obj.lastIndex) appendLine("is given by")
                                    else appendLine("is given at")
                                    appendLine("    '${request.origin}'")
                                }
                            }
                        }
                    )
                }

        @JvmField
        val MULTIPLE_GIVENS =
            DiagnosticFactory3.create<PsiElement, TypeRef, FqName, List<GivenNode>>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "Multiple givens found for {0} required by {1}:\n",
                        TypeRefRenderer,
                        Renderers.TO_STRING,
                        object : DiagnosticParameterRenderer<List<GivenNode>> {
                            override fun render(
                                obj: List<GivenNode>,
                                renderingContext: RenderingContext,
                            ): String {
                                return buildString {
                                    obj.forEach {
                                        appendLine("${it.type.render()}: ${it.origin}")
                                    }
                                }
                            }
                        }
                    )
                }

        @JvmField
        val GIVEN_CALL_NOT_AS_DEFAULT_VALUE = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
            .also {
                MAP.put(it,
                    "given property can only be used as a default value for a parameter")
            }

        @JvmField
        val GIVEN_OR_ELSE_CALL_NOT_AS_DEFAULT_VALUE =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also {
                    MAP.put(it,
                        "givenOrElse can only be used as a default value for a parameter")
                }

        @JvmField
        val NON_GIVEN_VALUE_PARAMETER_ON_GIVEN_DECLARATION =
            DiagnosticFactory1.create<PsiElement, Name>(Severity.ERROR)
                .also {
                    MAP.put(
                        it,
                        "Non @Given value parameter on @{0} declaration",
                        Renderers.TO_STRING
                    )
                }

        @JvmField
        val GIVEN_DECLARATION_WITH_EXTENSION_RECEIVER =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "@Given declaration cannot have a extension receiver") }

        @JvmField
        val GIVEN_CLASS_WITH_GIVEN_CONSTRUCTOR =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "Class cannot be given and have a given constructor") }

        @JvmField
        val CLASS_WITH_MULTIPLE_GIVEN_CONSTRUCTORS =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "Class cannot have multiple given constructors") }

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

private val TypeRefRenderer = object : DiagnosticParameterRenderer<TypeRef> {
    override fun render(obj: TypeRef, renderingContext: RenderingContext): String {
        return obj.render()
    }
}