package com.ivianuu.injekt.compiler.ast.string

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnnotationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithExpectActual
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithModality
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationWithVisibility
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.type.AstStarProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitorVoid
import org.jetbrains.kotlin.name.FqName

object Ast2StringTranslator {

    fun generate(element: AstElement): String {
        val builder = StringBuilder()
        element.accept(Writer(builder))
        return builder.toString()
    }

    private class Writer(private val builder: StringBuilder) : AstVisitorVoid {

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
            if (declaration.packageFqName != FqName.ROOT)
                appendIndentedLine("package ${declaration.packageFqName}")
            if (declaration.declarations.isNotEmpty()) appendLine()
            declaration.renderDeclarations()
        }

        override fun visitClass(declaration: AstClass) {
            declaration.renderAnnotations()
            appendIndent()
            declaration.renderVisibility()
            declaration.renderExpectActual()
            if (declaration.kind != AstClass.Kind.INTERFACE) declaration.renderModality()
            if (declaration.isFun) {
                append("fun ")
            }
            if (declaration.isData) {
                append("data ")
            }
            if (declaration.isInner) {
                append("inner ")
            }
            if (declaration.isExternal) {
                append("external ")
            }
            when (declaration.kind) {
                AstClass.Kind.CLASS -> append("class ")
                AstClass.Kind.INTERFACE -> append("interface ")
                AstClass.Kind.ENUM_CLASS -> append("enum class ")
                AstClass.Kind.ENUM_ENTRY -> TODO()
                AstClass.Kind.ANNOTATION -> append("annotation class ")
                AstClass.Kind.OBJECT -> {
                    if (declaration.isCompanion) append("companion ")
                    append("object ")
                }
            }.let { }

            append("${declaration.classId.className} ")
            if (declaration.declarations.isNotEmpty()) {
                appendBraced {
                    declaration.renderDeclarations()
                }
            } else {
                appendLine()
            }
        }

        override fun visitSimpleFunction(declaration: AstSimpleFunction) {
            declaration.renderAnnotations()
            appendIndent()
            declaration.renderVisibility()
            declaration.renderExpectActual()
            append("fun ")
            // todo type parameters
            append("${declaration.callableId.callableName}")
            append("(")
            declaration.valueParameters.forEachIndexed { index, valueParameter ->
                valueParameter.accept(this)
                if (index != declaration.valueParameters.lastIndex) append(", ")
            }
            append(")")
            append(": ")
            declaration.returnType.render()
            appendSpace()
            appendBraced {
            }
        }

        override fun visitConstructor(declaration: AstConstructor) {
            declaration.renderAnnotations()
            appendIndent()
            declaration.renderVisibility()
            declaration.renderExpectActual()
            append("constructor")
            append("(")
            declaration.valueParameters.forEachIndexed { index, valueParameter ->
                valueParameter.accept(this)
                if (index != declaration.valueParameters.lastIndex) append(", ")
            }
            append(")")
            append(": ${declaration.returnType.render()} ")
            appendBraced {
            }
        }

        override fun visitValueParameter(declaration: AstValueParameter) {
            declaration.renderAnnotations()
            append("${declaration.name}: ")
            declaration.type.render()
            if (declaration.defaultValue != null) {
                append(" = ")
                declaration.defaultValue!!.accept(this)
            }
        }

        private fun AstDeclarationWithVisibility.renderVisibility(appendSpace: Boolean = true) {
            append(visibility.name.toLowerCase())
            if (appendSpace) appendSpace()
        }

        private fun AstDeclarationWithExpectActual.renderExpectActual(appendSpace: Boolean = true) {
            if (expectActual != null) {
                append(expectActual!!.name.toLowerCase())
                if (appendSpace) appendSpace()
            }
        }

        private fun AstDeclarationWithModality.renderModality(appendSpace: Boolean = true) {
            append(modality.name.toLowerCase())
            if (appendSpace) appendSpace()
        }

        private fun AstDeclarationContainer.renderDeclarations() {
            declarations.forEachIndexed { index, declaration ->
                declaration.accept(this@Writer)
                if (index != declarations.lastIndex) appendLine()
            }
        }

        private fun AstAnnotationContainer.renderAnnotations() {
            annotations.forEachIndexed { index, annotation ->
                appendIndentedLine("@TODO")
                if (index != annotations.lastIndex) appendLine()
            }
        }

        private fun AstType.render() {
            renderAnnotations()

            when (val classifier = classifier) {
                is AstClass -> append(classifier.classId.packageName.child(classifier.classId.className))
                else -> error("Unexpected classifier $classifier")
            }

            if (arguments.isNotEmpty()) {
                append("<")
                arguments.forEachIndexed { index, typeArgument ->
                    typeArgument.renderTypeArgument()
                    if (index != arguments.lastIndex) append(", ")
                }
                append(">")
            }
        }

        private fun AstTypeArgument.renderTypeArgument() {
            when (this) {
                is AstStarProjection -> append("*")
                is AstTypeProjection -> {
                    variance?.let { append("${it.name.toLowerCase()} ") }
                    type.render()
                }
                else -> error("Unexpected type argument $this")
            }
        }
    }
}

fun AstElement.toAstString() = Ast2StringTranslator.generate(this)
