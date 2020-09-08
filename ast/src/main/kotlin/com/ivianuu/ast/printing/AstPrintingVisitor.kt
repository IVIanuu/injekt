package com.ivianuu.ast.printing

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.visitors.AstVisitorVoid
import org.jetbrains.kotlin.utils.Printer

fun String.formatPrintedString() =
    // replace tabs at beginning of line with white space
    replace(Regex("\\n(%tab%)+", RegexOption.MULTILINE)) {
        val size = it.range.last - it.range.first - 1
        "\n" + (0..(size / 5)).joinToString("") { "    " }
    }
        // tabs that are inserted in the middle of lines should be replaced with empty strings
        .replace(Regex("%tab%", RegexOption.MULTILINE), "")
        // remove empty lines
        .replace(Regex("\\n(\\s)*$", RegexOption.MULTILINE), "")
        // brackets with comma on new line
        .replace(Regex("}\\n(\\s)*,", RegexOption.MULTILINE), "},")

abstract class AstPrintingVisitor(out: Appendable) : AstVisitorVoid() {

    protected val printer = Printer(out, "%tab%")

    protected fun emit(value: Any?) {
        check(value !is Unit)
        printer.print(value)
    }

    protected fun emitLine(value: Any?) {
        check(value !is Unit)
        printer.println(value)
    }

    protected fun emitLine() {
        printer.println()
    }

    protected fun emitSpace() {
        emit(" ")
    }

    protected fun AstElement.emit() {
        accept(this@AstPrintingVisitor, null)
    }

    protected inline fun indented(body: () -> Unit) {
        printer.pushIndent()
        body()
        printer.popIndent()
    }

    override fun visitElement(element: AstElement) {
        error("Unhandled $element")
    }

}
