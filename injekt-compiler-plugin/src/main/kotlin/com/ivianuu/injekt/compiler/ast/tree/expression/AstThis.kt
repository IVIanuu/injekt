package com.ivianuu.injekt.compiler.ast.tree.expression

import com.ivianuu.injekt.compiler.ast.tree.AstTarget
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor

fun String.lol(block: String.() -> Unit) {

}

fun Long.hehe() {
    "hello".lol lol2@{
        "bye".lol lol1@{
            this@lol1.toInt()
            this@hehe
        }
    }
}

class AstThis(
    override var type: AstType,
    var target: AstTarget
) : AstExpressionBase() {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitExpression(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
    }

}
