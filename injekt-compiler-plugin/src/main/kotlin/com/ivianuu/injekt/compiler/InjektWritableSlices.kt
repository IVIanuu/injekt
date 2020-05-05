package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object InjektWritableSlices {
    val QUALIFIERS: WritableSlice<IrAttributeContainer, List<FqName>> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
