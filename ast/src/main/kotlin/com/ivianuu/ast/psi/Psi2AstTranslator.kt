package com.ivianuu.ast.psi

import com.ivianuu.ast.AstGeneratorContext
import com.ivianuu.ast.tree.AstElement
import com.ivianuu.ast.tree.declaration.AstDeclaration
import com.ivianuu.ast.tree.declaration.AstFile
import com.ivianuu.ast.tree.declaration.AstModuleFragment
import com.ivianuu.ast.tree.visitor.AstVisitorVoid
import org.jetbrains.kotlin.psi.KtFile

class Psi2AstTranslator(
    private val context: AstGeneratorContext
) {

    fun generateModule(files: List<KtFile>): AstModuleFragment {
        val visitor = Psi2AstVisitor(context)
        files.forEach { it.accept(visitor, Psi2AstVisitor.Mode.PARTIAL) }

        val moduleFragment = AstModuleFragment(context.module.name).apply {
            this.files += files.map {
                it.accept(visitor, Psi2AstVisitor.Mode.FULL) as AstFile
            }
        }

        moduleFragment.accept(object : AstVisitorVoid {
            override fun visitElement(element: AstElement, data: Nothing?) {
                element.acceptChildren(this, null)
            }

            override fun visitDeclaration(declaration: AstDeclaration, data: Nothing?) {
                try {
                    declaration.parent
                } catch (e: Throwable) {
                    error("Parent not set $declaration")
                }
                super.visitDeclaration(declaration, null)
            }
        }, null)

        return moduleFragment
    }

}
