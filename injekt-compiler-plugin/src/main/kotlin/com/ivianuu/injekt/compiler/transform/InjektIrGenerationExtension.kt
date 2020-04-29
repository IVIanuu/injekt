package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.generateSymbols
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class InjektIrGenerationExtension(private val project: Project) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        fun ModuleLoweringPass.visitModuleAndGenerateSymbols() {
            generateSymbols(pluginContext)
            lower(moduleFragment)
            generateSymbols(pluginContext)
        }


    }

}
