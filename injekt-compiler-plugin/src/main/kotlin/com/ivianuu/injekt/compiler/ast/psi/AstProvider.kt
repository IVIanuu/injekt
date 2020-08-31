package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration

class AstProvider {

    lateinit var stubGenerator: Psi2AstStubGenerator
    lateinit var psi2AstVisitor: Psi2AstVisitor

    fun <T : AstElement> get(descriptor: DeclarationDescriptor): T {
        if (descriptor.findPsi() == null) return stubGenerator.get(descriptor)

        val psi = descriptor.findPsi()!! as KtDeclaration
        return psi.accept(psi2AstVisitor, null) as T
    }

}
