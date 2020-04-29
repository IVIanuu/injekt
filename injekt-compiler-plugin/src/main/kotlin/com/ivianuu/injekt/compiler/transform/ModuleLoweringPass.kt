package com.ivianuu.injekt.compiler.transform

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

interface ModuleLoweringPass {
    fun lower(module: IrModuleFragment)
}
