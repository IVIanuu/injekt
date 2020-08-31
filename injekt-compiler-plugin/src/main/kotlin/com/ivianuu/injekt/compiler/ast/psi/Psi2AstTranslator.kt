package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.extension.AstBuiltIns
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

class Psi2AstTranslator(
    private val astProvider: AstProvider,
    private val bindingContext: BindingContext,
    private val module: ModuleDescriptor,
    private val stubGenerator: Psi2AstStubGenerator,
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
        astProvider.psi2AstVisitor = visitor
        astProvider.stubGenerator = stubGenerator
        return AstModuleFragment(module.name).apply {
            this.files += files.map {
                visitor.visitKtFile(it, null) as AstFile
            }
        }
    }

}
