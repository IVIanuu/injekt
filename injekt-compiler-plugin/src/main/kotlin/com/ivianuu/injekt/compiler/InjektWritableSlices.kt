package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object InjektWritableSlices {
    val QUALIFIERS: WritableSlice<IrAttributeContainer, List<IrConstructorCall>> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
