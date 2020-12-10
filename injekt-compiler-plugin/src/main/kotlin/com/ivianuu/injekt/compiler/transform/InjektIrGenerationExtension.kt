package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.Binding
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

@Binding class InjektIrGenerationExtension(
    private val givenCallTransformer: (IrPluginContext) -> GivenCallTransformer,
    private val givenOptimizationTransformer: GivenOptimizationTransformer,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transformChildrenVoid(givenCallTransformer(pluginContext))
        moduleFragment.transformChildrenVoid(givenOptimizationTransformer)
        println(moduleFragment.dump())
    }
}
