package com.ivianuu.ast

import com.ivianuu.ast.declarations.AstClass
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.expressions.AstLoop

interface AstTarget<E : AstTargetElement> {
    val element: E
    fun bind(element: E)
}

abstract class AstAbstractTarget<E : AstTargetElement> : AstTarget<E> {
    override lateinit var element: E

    override fun bind(element: E) {
        this.element = element
    }
}

class AstClassTarget : AstAbstractTarget<AstClass<*>>()

class AstFunctionTarget : AstAbstractTarget<AstFunction<*>>()

class AstLoopTarget : AstAbstractTarget<AstLoop>()

class AstPropertyTarget : AstAbstractTarget<AstProperty>()