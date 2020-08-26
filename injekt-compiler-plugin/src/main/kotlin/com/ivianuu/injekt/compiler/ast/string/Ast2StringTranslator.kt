package com.ivianuu.injekt.compiler.ast.string

import com.ivianuu.injekt.compiler.ast.AstAnnotationContainer
import com.ivianuu.injekt.compiler.ast.AstClass
import com.ivianuu.injekt.compiler.ast.AstDeclaration
import com.ivianuu.injekt.compiler.ast.AstDeclarationContainer
import com.ivianuu.injekt.compiler.ast.AstElement
import com.ivianuu.injekt.compiler.ast.AstFile
import com.ivianuu.injekt.compiler.ast.AstModifierContainer
import com.ivianuu.injekt.compiler.ast.AstVisitorVoid
import com.ivianuu.injekt.compiler.ast.accept

object Ast2StringTranslator {

    fun generate(element: AstElement): String {
        val builder = StringBuilder()
        element.accept(Visitor(builder))
        return builder.toString()
    }

    private class Visitor(private val builder: StringBuilder) : AstVisitorVoid {

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

        override fun visitFile(declaration: AstFile) {
            appendIndentedLine("package ${declaration.packageFqName}")
            if (declaration.declarations.isNotEmpty()) appendLine()
            declaration.renderDeclarations()
        }

        override fun visitClass(declaration: AstClass) {
            declaration.renderAnnotations()
            appendIndent()
            declaration.renderModifiers(appendSpace = true)
            declaration.renderName(appendSpace = true)
            if (declaration.declarations.isNotEmpty()) {
                appendBraced {
                    declaration.renderDeclarations()
                }
            } else {
                appendLine()
            }
        }

        private fun AstDeclarationContainer.renderDeclarations() {
            declarations.forEachIndexed { index, declaration ->
                declaration.accept(this@Visitor)
                if (index != declarations.lastIndex) appendLine()
            }
        }

        private fun AstAnnotationContainer.renderAnnotations() {
            annotations.forEachIndexed { index, annotation ->
                appendIndentedLine("@TODO")
                if (index != annotations.lastIndex) appendLine()
            }
        }

        private fun AstModifierContainer.renderModifiers(appendSpace: Boolean = true) {
            modifiers.forEachIndexed { index, modifier ->
                append(modifier.name.toLowerCase())
                if (index != modifiers.lastIndex) appendSpace()
            }
            if (appendSpace && modifiers.isNotEmpty()) appendSpace()
        }

        private fun AstDeclaration.renderName(appendSpace: Boolean = true) {
            append(name)
            if (appendSpace) appendSpace()
        }

    }
}

fun AstElement.toAstString() = Ast2StringTranslator.generate(this)
