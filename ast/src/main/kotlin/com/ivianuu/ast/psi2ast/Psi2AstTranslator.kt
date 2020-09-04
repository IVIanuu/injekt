package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstModuleFragment
import com.ivianuu.ast.declarations.builder.buildModuleFragment
import org.jetbrains.kotlin.psi.KtFile

class Psi2AstTranslator(private val context: Psi2AstGeneratorContext) {

    fun generateModule(ktFiles: List<KtFile>): AstModuleFragment {
        val visitor = Psi2AstVisitor(context)
        return buildModuleFragment {
            name = context.module.name
            files += ktFiles.map { it.accept(visitor, null) as AstFile }
            context.symbolTable.unboundSymbols
                .forEach { (descriptor, symbol) ->
                    if (!symbol.isBound) {
                        context.stubGenerator.getDeclaration(symbol, descriptor)
                    }
                }
        }
    }

}
