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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.RootFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleFunctionTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InjektDeclarationStore(private val pluginContext: IrPluginContext) {

    lateinit var factoryTransformer: RootFactoryTransformer
    lateinit var factoryModuleTransformer: FactoryModuleTransformer
    lateinit var membersInjectorTransformer: MembersInjectorTransformer
    lateinit var moduleFunctionTransformer: ModuleFunctionTransformer

    fun getMembersInjectorForClassOrNull(clazz: IrClass): IrClass? {
        return clazz.declarations
            .filterIsInstance<IrClass>()
            .singleOrNull {
                it.descriptor.name == InjektNameConventions.getMembersInjectorNameForClass(clazz.name)
            }
    }

    fun getModuleFunctionForFactory(factoryFunction: IrFunction): IrFunction {
        return if (!factoryFunction.isExternalDeclaration()) {
            (factoryFunction.parent as IrDeclarationContainer)
                .declarations
                .filterIsInstance<IrFunction>()
                .firstOrNull {
                    it.descriptor.name == InjektNameConventions.getModuleNameForFactoryFunction(
                        factoryFunction
                    )
                } ?: factoryModuleTransformer
                .getModuleFunctionForFactoryFunction(factoryFunction)
        } else {
            pluginContext.referenceFunctions(
                factoryFunction.descriptor.fqNameSafe
                    .parent()
                    .child(InjektNameConventions.getModuleNameForFactoryFunction(factoryFunction))
            ).filter { it.owner.valueParameters.lastOrNull()?.name?.asString() == "moduleMarker" }
                .let {
                it.singleOrNull()?.owner ?: error(
                    "Couldn't find factory function for\n${InjektNameConventions.getModuleNameForFactoryFunction(
                        factoryFunction
                    )} all ${it.map { it.owner.render() }}\nfactory ${factoryFunction.render()}"
                )
            }
        }
    }

    fun getModuleFunctionForClass(moduleClass: IrClass): IrFunction {
        return if (!moduleClass.isExternalDeclaration()) {
            moduleFunctionTransformer.getModuleFunctionForClass(moduleClass)
        } else {
            pluginContext.referenceFunctions(
                moduleClass.descriptor.fqNameSafe
                    .parent()
                    .child(InjektNameConventions.getModuleFunctionNameForClass(moduleClass))
            ).single {
                it.owner.returnType.classOrNull == moduleClass.symbol
            }.owner
        }
    }

    fun getModuleClassForFunction(moduleFunction: IrFunction): IrClass {
        return getModuleClassForFunctionOrNull(moduleFunction) ?: error(
            "Couldn't find class for function ${moduleFunction.dump()}"
        )
    }

    fun getModuleClassForFunctionOrNull(moduleFunction: IrFunction): IrClass? =
        moduleFunctionTransformer.getModuleClassForFunction(moduleFunction)

}
