package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object InjektWritableSlices {
    val IS_MODULE: WritableSlice<Any, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
