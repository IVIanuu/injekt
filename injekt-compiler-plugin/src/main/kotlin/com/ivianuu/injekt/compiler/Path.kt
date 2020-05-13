package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType

sealed class Path {
    abstract fun asAnnotation(
        builder: IrBuilderWithScope,
        symbols: InjektSymbols
    ): IrConstructorCall
}

class ClassPath(val clazz: IrClass) : Path() {
    override fun asAnnotation(
        builder: IrBuilderWithScope,
        symbols: InjektSymbols
    ): IrConstructorCall {
        return builder.irCall(symbols.astClassPath.constructors.single()).apply {
            putValueArgument(
                0,
                IrClassReferenceImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    builder.context.irBuiltIns.kClassClass.typeWith(clazz.defaultType),
                    clazz.symbol,
                    clazz.defaultType
                )
            )
        }

    }
}

class PropertyPath(val property: IrProperty) : Path() {
    override fun asAnnotation(
        builder: IrBuilderWithScope,
        symbols: InjektSymbols
    ): IrConstructorCall {
        return builder.irCall(symbols.astPropertyPath.constructors.single()).apply {
            putValueArgument(0, builder.irString(property.name.asString()))
        }
    }
}

class TypeParameterPath(val typeParameter: IrTypeParameter) : Path() {
    override fun asAnnotation(
        builder: IrBuilderWithScope,
        symbols: InjektSymbols
    ): IrConstructorCall {
        return builder.irCall(symbols.astTypeParameterPath.constructors.single()).apply {
            putValueArgument(0, builder.irString(typeParameter.name.asString()))
        }
    }
}

class ValueParameterPath(val valueParameter: IrValueParameter) : Path() {
    override fun asAnnotation(
        builder: IrBuilderWithScope,
        symbols: InjektSymbols
    ): IrConstructorCall {
        return builder.irCall(symbols.astValueParameterPath.constructors.single()).apply {
            putValueArgument(0, builder.irString(valueParameter.name.asString()))
        }
    }
}
