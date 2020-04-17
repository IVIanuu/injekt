package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object InjektWritableSlices {
    val MODULE_STATE: WritableSlice<KtElement, ModuleAnnotationChecker.ModuleFunctionState> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val INFERRED_MODULE_DESCRIPTOR: WritableSlice<FunctionDescriptor, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val COMPONENT_FQ_NAME: WritableSlice<IrCall, FqName> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)
}