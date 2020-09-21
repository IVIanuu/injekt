package com.ivianuu.injekt.compiler.generator

import org.jetbrains.kotlin.utils.Printer

fun buildCodeString(block: CodeBuilder.() -> Unit): String {
    val stringBuilder = StringBuilder()
    CodeBuilder(stringBuilder).apply(block)
    return stringBuilder.toString().formatPrintedString()
}

class CodeBuilder(out: Appendable) {

    @PublishedApi
    internal val printer = Printer(out, "%tab%")

    fun emit(value: Any?) {
        check(value !is Unit)
        printer.print(value)
    }

    fun emitLine(value: Any?) {
        check(value !is Unit)
        printer.println(value)
    }

    fun emitLine() {
        printer.println()
    }

    inline fun indented(body: () -> Unit) {
        printer.pushIndent()
        body()
        printer.popIndent()
    }

    inline fun braced(body: () -> Unit) {
        emitLine("{")
        indented {
            body()
            emitLine()
        }
        emitLine("}")
    }

}

private fun String.formatPrintedString() =
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
