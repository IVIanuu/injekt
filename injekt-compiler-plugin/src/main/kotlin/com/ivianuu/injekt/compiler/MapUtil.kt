package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.name.FqName

val SupportedMapKeyTypes: List<FqName> = listOf(
    KotlinBuiltIns.FQ_NAMES.kClass.toSafe(),
    KotlinBuiltIns.FQ_NAMES._int.toSafe(),
    KotlinBuiltIns.FQ_NAMES._long.toSafe(),
    KotlinBuiltIns.FQ_NAMES.string.toSafe()
)

sealed class MapKey {
    abstract fun IrBuilderWithScope.asExpression(): IrExpression
}

data class ClassKey(val value: IrType) : MapKey() {
    override fun IrBuilderWithScope.asExpression(): IrExpression {
        return IrClassReferenceImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.kClassClass.typeWith(value),
            value.classifierOrFail,
            value
        )
    }
}

data class IntKey(val value: Int) : MapKey() {
    override fun IrBuilderWithScope.asExpression(): IrExpression = irInt(value)
}

data class LongKey(val value: Long) : MapKey() {
    override fun IrBuilderWithScope.asExpression(): IrExpression = irLong(value)
}

data class StringKey(val value: String) : MapKey() {
    override fun IrBuilderWithScope.asExpression(): IrExpression = irString(value)
}
