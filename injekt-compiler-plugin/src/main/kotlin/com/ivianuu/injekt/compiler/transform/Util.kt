package com.ivianuu.injekt.compiler.transform

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.constructors

fun IrBlockBodyBuilder.initializeClassWithAnySuperClass(symbol: IrClassSymbol) {
    +IrDelegatingConstructorCallImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.unitType,
        context.irBuiltIns.anyClass
            .constructors.single()
    )
    +IrInstanceInitializerCallImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        symbol,
        context.irBuiltIns.unitType
    )
}
