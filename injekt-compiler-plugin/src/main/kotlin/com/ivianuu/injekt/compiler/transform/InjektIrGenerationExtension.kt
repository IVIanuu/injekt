package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.DeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class InjektIrGenerationExtension(
    private val declarationStore: DeclarationStore,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transformChildrenVoid(GivenCallTransformer(declarationStore, pluginContext))
        moduleFragment.transformChildrenVoid(GivenOptimizationTransformer())
    }
}
