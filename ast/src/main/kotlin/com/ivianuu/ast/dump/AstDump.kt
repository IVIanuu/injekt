package com.ivianuu.ast.dump

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstModuleFragment
import com.ivianuu.ast.printing.AstPrintingVisitor
import com.ivianuu.ast.printing.formatPrintedString

fun AstElement.dump(): String {
    return buildString {
        try {
            accept(AstDumpVisitor(this), null)
        } catch (e: Exception) {
            throw RuntimeException(toString().formatPrintedString(), e)
        }
    }.formatPrintedString()
}

private class AstDumpVisitor(out: Appendable) : AstPrintingVisitor(out) {

    override fun visitModuleFragment(moduleFragment: AstModuleFragment) {
        emitLine("MODULE FRAGMENT: ${moduleFragment.name}")
        indented {
            moduleFragment.files.forEach { it.emit() }
        }
    }

    override fun visitFile(file: AstFile) {
        emitLine("FILE: ${file.name}")
        indented {
            emitLine("packageName: ${file.packageFqName}")
        }
    }

}
