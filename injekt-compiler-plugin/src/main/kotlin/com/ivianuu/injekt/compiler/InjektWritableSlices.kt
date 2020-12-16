package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.resolution.GivenGraph
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy

object InjektWritableSlices {
    val GIVEN_GRAPH =
        BasicWritableSlice<SourcePosition, GivenGraph.Success>(RewritePolicy.DO_NOTHING)
    val USED_GIVEN = BasicWritableSlice<CallableDescriptor, Unit>(RewritePolicy.DO_NOTHING)
}

data class SourcePosition(
    val filePath: String,
    val startOffset: Int,
    val endOffset: Int,
)
