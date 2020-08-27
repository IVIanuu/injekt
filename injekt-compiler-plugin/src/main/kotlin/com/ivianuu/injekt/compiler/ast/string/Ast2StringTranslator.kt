package com.ivianuu.injekt.compiler.ast.string

import com.ivianuu.injekt.compiler.ast.AstAnnotationContainer
import com.ivianuu.injekt.compiler.ast.AstCall
import com.ivianuu.injekt.compiler.ast.AstClass
import com.ivianuu.injekt.compiler.ast.AstDeclarationContainer
import com.ivianuu.injekt.compiler.ast.AstElement
import com.ivianuu.injekt.compiler.ast.AstFile
import com.ivianuu.injekt.compiler.ast.AstFunction
import com.ivianuu.injekt.compiler.ast.AstModalityOwner
import com.ivianuu.injekt.compiler.ast.AstMultiPlatformDeclaration
import com.ivianuu.injekt.compiler.ast.AstTransformResult
import com.ivianuu.injekt.compiler.ast.AstTransformer
import com.ivianuu.injekt.compiler.ast.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.AstValueParameter
import com.ivianuu.injekt.compiler.ast.AstVisibilityOwner
import com.ivianuu.injekt.compiler.ast.compose

object Ast2StringTranslator {

    fun generate(element: AstElement): String {
        val builder = StringBuilder()
        Writer(builder).transform(element)
        return builder.toString()
    }

    private class Writer(private val builder: StringBuilder) : AstTransformer {

        private var indent = ""

        private fun append(value: Any) {
            builder.append("$value")
        }

        private fun appendLine() {
            builder.appendLine()
        }

        private fun appendLine(value: Any) {
            builder.appendLine("$value")
        }

        private fun appendIndented(value: Any) {
            append(indent)
            append(value)
        }

        private fun appendIndentedLine(value: Any) {
            appendLine("$indent$value")
        }

        private fun appendIndent() {
            append(indent)
        }

        private fun appendSpace() = append(" ")

        private inline fun <T> appendIndented(block: () -> T): T {
            indent += "    "
            val result = block()
            indent = indent.dropLast(4)
            return result
        }

        private inline fun <T> appendBraced(block: () -> T): T = run {
            appendLine("{")
            val result = appendIndented(block)
            appendIndentedLine("}")
            result
        }

        override fun transform(element: AstElement): AstTransformResult<AstElement> {
            when (element) {
                is AstFile -> {
                    appendIndentedLine("package ${element.packageFqName}")
                    if (element.declarations.isNotEmpty()) appendLine()
                    element.renderDeclarations()
                }
                is AstClass -> {
                    element.renderAnnotations()
                    appendIndent()
                    element.renderVisibility()
                    element.renderMultiPlatformModality()
                    if (element.kind != AstClass.Kind.INTERFACE) element.renderModality()
                    if (element.isInner) {
                        append("inner ")
                    }
                    if (element.isData) {
                        append("data ")
                    }
                    when (element.kind) {
                        AstClass.Kind.CLASS -> append("class ")
                        AstClass.Kind.INTERFACE -> append("interface ")
                        AstClass.Kind.ENUM_CLASS -> append("enum class ")
                        AstClass.Kind.ENUM_ENTRY -> TODO()
                        AstClass.Kind.ANNOTATION -> append("annotation class ")
                        AstClass.Kind.OBJECT -> {
                            if (element.isCompanion) append("companion ")
                            append("object ")
                        }
                    }.let { }

                    append("${element.classId.className} ")
                    if (element.declarations.isNotEmpty()) {
                        appendBraced {
                            element.renderDeclarations()
                        }
                    } else {
                        appendLine()
                    }
                }
                is AstFunction -> {
                    element.renderAnnotations()
                    appendIndent()
                    //element.renderModifiers(appendSpace = true)
                    append("fun ")
                    //element.renderName(appendSpace = true)
                    appendBraced { }
                }
                is AstTypeParameter -> TODO()
                is AstValueParameter -> TODO()
                is AstCall -> TODO()
            }.let { }
            return element.compose()
        }

        private fun AstVisibilityOwner.renderVisibility(appendSpace: Boolean = true) {
            append(visibility.name.toLowerCase())
            if (appendSpace) appendSpace()
        }

        private fun AstMultiPlatformDeclaration.renderMultiPlatformModality(appendSpace: Boolean = true) {
            if (multiPlatformModality != null) {
                append(multiPlatformModality!!.name.toLowerCase())
                if (appendSpace) appendSpace()
            }
        }

        private fun AstModalityOwner.renderModality(appendSpace: Boolean = true) {
            append(modality.name.toLowerCase())
            if (appendSpace) appendSpace()
        }

        private fun AstDeclarationContainer.renderDeclarations() {
            declarations.forEachIndexed { index, declaration ->
                transform(declaration)
                if (index != declarations.lastIndex) appendLine()
            }
        }

        private fun AstAnnotationContainer.renderAnnotations() {
            annotations.forEachIndexed { index, annotation ->
                appendIndentedLine("@TODO")
                if (index != annotations.lastIndex) appendLine()
            }
        }

    }
}

fun AstElement.toAstString() = Ast2StringTranslator.generate(this)
