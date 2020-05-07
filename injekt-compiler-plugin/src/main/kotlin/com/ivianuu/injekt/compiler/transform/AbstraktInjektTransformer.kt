package com.ivianuu.injekt.compiler.transform

/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.ivianuu.injekt.compiler.InjektSymbols
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.types.KotlinType

abstract class AbstractInjektTransformer(
    val pluginContext: IrPluginContext
) : IrElementTransformerVoid(), ModuleLoweringPass {

    val symbols = InjektSymbols(pluginContext)

    val irProviders = pluginContext.irProviders
    protected val symbolTable = pluginContext.symbolTable
    val irBuiltIns = pluginContext.irBuiltIns
    protected val builtIns = pluginContext.builtIns
    protected val typeTranslator = pluginContext.typeTranslator
    protected open fun KotlinType.toIrType() = typeTranslator.translateType(this)

    lateinit var moduleFragment: IrModuleFragment

    override fun lower(module: IrModuleFragment) {
        moduleFragment = module
        visitModuleFragment(module, null)
        module.patchDeclarationParents()
    }

}
