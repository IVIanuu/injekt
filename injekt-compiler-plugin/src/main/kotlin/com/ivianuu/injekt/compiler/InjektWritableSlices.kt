package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object InjektWritableSlices {
    val TYPE_ANNOTATIONS: WritableSlice<Any, MutableSet<FqName>> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val QUALIFIERS: WritableSlice<IrAttributeContainer, List<IrConstructorCall>> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
