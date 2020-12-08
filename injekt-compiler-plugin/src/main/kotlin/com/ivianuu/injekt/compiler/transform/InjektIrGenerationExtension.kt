package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.Binding
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

@Binding class InjektIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val transformers = listOf(CreateTransformer(pluginContext),
            MergeAccessorTransformer(pluginContext))
        transformers.forEach { moduleFragment.transformChildrenVoid(it) }
    }
}