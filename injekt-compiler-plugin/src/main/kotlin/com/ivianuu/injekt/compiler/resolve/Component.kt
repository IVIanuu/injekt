package com.ivianuu.injekt.compiler.resolve

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression

data class ComponentWithAccessor(
    val component: IrClass,
    val accessor: () -> IrExpression
)
