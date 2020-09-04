package com.ivianuu.ast

import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.expressions.AstLoop

interface AstTarget<E : AstTargetElement> {
    val labelName: String?
    val labeledElement: E
    fun bind(element: E)
}

abstract class AstAbstractTarget<E : AstTargetElement>(
    override val labelName: String?
) : AstTarget<E> {
    override lateinit var labeledElement: E

    override fun bind(element: E) {
        labeledElement = element
    }
}

class AstFunctionTarget(
    labelName: String?,
    val isLambda: Boolean
) : AstAbstractTarget<AstFunction<*>>(labelName)

class AstLoopTarget(
    labelName: String?
) : AstAbstractTarget<AstLoop>(labelName)
