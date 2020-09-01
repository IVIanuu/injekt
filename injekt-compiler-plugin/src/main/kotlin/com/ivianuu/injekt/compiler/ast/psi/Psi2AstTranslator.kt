package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.extension.AstBuiltIns
import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitorVoid
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

class Psi2AstTranslator(
    private val astProvider: AstProvider,
    private val bindingContext: BindingContext,
    private val module: ModuleDescriptor,
    private val storage: Psi2AstStorage,
    private val typeMapper: TypeMapper
) {

    lateinit var builtIns: AstBuiltIns

    fun generateModule(files: List<KtFile>): AstModuleFragment {
        val context = GeneratorContext(
            module,
            bindingContext,
            builtIns,
            module.builtIns,
            storage,
            typeMapper,
            astProvider
        )
        val visitor = Psi2AstVisitor(context)

        files.forEach { it.accept(visitor, Psi2AstVisitor.Mode.PARTIAL) }

        astProvider.psi2AstVisitor = visitor

        val moduleFragment = AstModuleFragment(module.name).apply {
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
