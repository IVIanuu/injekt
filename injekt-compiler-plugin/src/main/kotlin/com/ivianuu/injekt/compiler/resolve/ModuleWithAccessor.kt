package com.ivianuu.injekt.compiler.resolve

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression

data class ModuleWithAccessor(
    val module: IrClass,
    val accessor: () -> IrExpression
)