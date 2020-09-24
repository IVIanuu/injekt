package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy

object InjektWritableSlices {
    val IS_READER = BasicWritableSlice<DeclarationDescriptor, Boolean>(RewritePolicy.DO_NOTHING)
    val IS_RUN_READER_FUNCTION = BasicWritableSlice<Any, Boolean>(RewritePolicy.DO_NOTHING)
    val IS_TRANSFORMED_READER = BasicWritableSlice<Any, Boolean>(RewritePolicy.DO_NOTHING)
}
