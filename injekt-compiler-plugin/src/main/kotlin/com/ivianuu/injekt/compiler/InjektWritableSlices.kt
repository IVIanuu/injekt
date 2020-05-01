package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object InjektWritableSlices {
    val IS_MODULE: WritableSlice<Any, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val QUALIFIERS: WritableSlice<IrAttributeContainer, List<FqName>> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val PROVIDER_INDEX: WritableSlice<IrClass, Int> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val GET_FUNCTION_INDEX: WritableSlice<IrClass, Int> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
