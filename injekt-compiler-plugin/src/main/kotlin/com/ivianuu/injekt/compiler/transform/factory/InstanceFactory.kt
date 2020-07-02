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

package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InstanceFactory(
    factoryFunction: IrFunction,
    moduleClass: IrClass,
    factoryModuleAccessor: FactoryExpression,
    pluginContext: IrPluginContext,
    symbols: InjektSymbols,
    declarationStore: InjektDeclarationStore
) : AbstractFactory(
    factoryFunction.descriptor.fqNameSafe,
    moduleClass,
    factoryModuleAccessor,
    factoryFunction,
    pluginContext,
    symbols,
    declarationStore
) {

    fun getInstanceExpression(): IrExpression {
        return DeclarationIrBuilder(pluginContext, factoryFunction.symbol).run {
            irBlock {
                factoryMembers.blockBuilder = this

                val instanceRequest = BindingRequest(
                    key = factoryFunction.returnType
                        .asKey(),
                    requestingKey = null,
                    requestOrigin = factoryFunction.descriptor.fqNameSafe
                )

                init(null, listOf(instanceRequest))

                +factoryExpressions.getBindingExpression(instanceRequest)(this)!!
            }
        }
    }
}
