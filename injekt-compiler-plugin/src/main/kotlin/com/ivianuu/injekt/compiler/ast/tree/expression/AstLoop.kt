package com.ivianuu.injekt.compiler.ast.tree.expression

import com.ivianuu.injekt.compiler.ast.tree.AstTarget
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle

interface AstLoop : AstExpression, AstTarget {
    var body: AstExpression?
}

class AstForLoop(override var type: AstType) : AstLoop {

    override var body: AstExpression? = null
    lateinit var iterable: AstExpression
    lateinit var loopParameter: AstValueParameter

    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitForLoop(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        iterable.accept(visitor, data)
        loopParameter.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        iterable = iterable.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
    }

}

class AstWhileLoop(
    override var type: AstType,
    var kind: Kind
) : AstLoop {

    override var body: AstExpression? = null
    lateinit var condition: AstExpression

    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitWhileLoop(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        condition.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        condition = condition.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
    }

    enum class Kind {
        WHILE, DO_WHILE
    }

}
