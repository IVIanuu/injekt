package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstExpectActual
import com.ivianuu.injekt.compiler.ast.tree.AstVisibility
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import org.jetbrains.kotlin.name.Name

class AstTypeAlias(
    override var name: Name,
    var type: AstType,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var expectActual: AstExpectActual? = null
) : AstDeclarationBase(), AstDeclarationWithName, AstDeclarationWithVisibility,
    AstDeclarationWithExpectActual {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R {
        TODO()
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        TODO()
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        TODO()
    }

}
