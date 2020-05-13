package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.analysis.TypeAnnotationChecker
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

fun IrFunction.isModule(bindingContext: BindingContext): Boolean {
    if (hasAnnotation(InjektFqNames.Module)) return true
    val typeAnnotationChecker = TypeAnnotationChecker()
    val bindingTrace = DelegatingBindingTrace(bindingContext, "Injekt IR")
    return try {
        typeAnnotationChecker.hasTypeAnnotation(
            bindingTrace, descriptor,
            InjektFqNames.Module
        )
    } catch (e: Exception) {
        false
    }
}
