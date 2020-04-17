package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object InjektWritableSlices {
    val MODULE_STATE: WritableSlice<KtElement, ModuleAnnotationChecker.ModuleFunctionState> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val INFERRED_MODULE_DESCRIPTOR: WritableSlice<FunctionDescriptor, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}