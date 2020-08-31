package com.ivianuu.injekt.compiler.ast.tree.expression

interface AstLoop : AstExpression {
    var body: AstExpression?
    var condition: AstExpression
    val label: String?
}

interface AstWhileLoop : AstLoop

interface AstDoWhileLoop : AstLoop
