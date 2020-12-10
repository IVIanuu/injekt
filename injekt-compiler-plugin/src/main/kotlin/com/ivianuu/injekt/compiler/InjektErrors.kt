package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.render
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
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
        val UNRESOLVED_GIVEN = DiagnosticFactory1.create<PsiElement, TypeRef>(Severity.ERROR)
            .also { MAP.put(it, "Unresolved given for {0}", TypeRefRenderer) }

        @JvmField
        val MULTIPLE_GIVENS = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
            .also { MAP.put(it, "Multiple givens found") }

        @JvmField
        val GIVEN_CALL_NOT_AS_DEFAULT_VALUE = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
            .also { MAP.put(it, "The given property can only be used on a parameter with a @Given type") }

        @JvmField
        val NON_GIVEN_VALUE_PARAMETER_ON_GIVEN_DECLARATION =
            DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
                .also { MAP.put(it, "Non @Given value parameter on @Given declaration") }

        @JvmField
        val GIVEN_PARAMETER_WITHOUT_DEFAULT = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
            .also {
                MAP.put(it,
                    "@Given parameter must have have default value either 'given' or a fallback")
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