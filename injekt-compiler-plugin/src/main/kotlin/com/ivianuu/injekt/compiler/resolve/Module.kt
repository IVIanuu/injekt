package com.ivianuu.injekt.compiler.resolve

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType

data class ModuleWithAccessor(
    val module: IrClass,
    val typeParametersMap: Map<IrTypeParameterSymbol, IrType>,
    val accessor: () -> IrExpression
)
