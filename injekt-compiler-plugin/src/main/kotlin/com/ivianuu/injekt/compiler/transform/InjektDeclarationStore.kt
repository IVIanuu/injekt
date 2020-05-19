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
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.RootFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleClassTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InjektDeclarationStore(private val pluginContext: IrPluginContext) {

    lateinit var classFactoryTransformer: ClassFactoryTransformer
    lateinit var factoryTransformer: RootFactoryTransformer
    lateinit var factoryModuleTransformer: FactoryModuleTransformer
    lateinit var membersInjectorTransformer: MembersInjectorTransformer
    lateinit var moduleClassTransformer: ModuleClassTransformer

    private fun IrDeclaration.isExternalDeclaration() = origin ==
            IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
            origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

    fun getFactoryForClass(clazz: IrClass): IrClass {
        return if (!clazz.isExternalDeclaration()) {
            classFactoryTransformer.getFactoryForClass(clazz)
        } else {
            pluginContext.referenceClass(
                clazz.fqNameForIrSerialization
                    .parent()
                    .child(
                        InjektNameConventions.getFactoryNameForClass(
                            clazz.getPackageFragment()!!.fqName,
                            clazz.descriptor.fqNameSafe
                        )
                    )
            )!!.owner
        }
    }

    fun getMembersInjectorForClassOrNull(clazz: IrClass): IrClass? {
        return if (!clazz.isExternalDeclaration()) {
            membersInjectorTransformer.getMembersInjectorForClass(clazz)
        } else {
            pluginContext.referenceClass(
                clazz.fqNameForIrSerialization
                    .parent()
                    .child(
                        InjektNameConventions.getMembersInjectorNameForClass(
                            clazz.getPackageFragment()!!.fqName,
                            clazz.descriptor.fqNameSafe
                        )
                    )
            )?.owner
        }
    }

    fun getModuleFunctionForFactory(factoryFunction: IrFunction): IrFunction {
        return if (!factoryFunction.isExternalDeclaration()) {
            factoryModuleTransformer.getModuleFunctionForFactoryFunction(factoryFunction)
        } else {
            pluginContext.referenceFunctions(
                factoryFunction.descriptor.fqNameSafe
                    .parent()
                    .child(InjektNameConventions.getModuleNameForFactoryFunction(factoryFunction))
            ).let {
                it.singleOrNull()?.owner ?: error(
                    "Couldn't find factory function for ${InjektNameConventions.getModuleNameForFactoryFunction(
                        factoryFunction
                    )} all ${it.map { it.owner.render() }}"
                )
            }
        }
    }

    fun getModuleClassForFunction(moduleFunction: IrFunction): IrClass {
        return getModuleClassForFunctionOrNull(moduleFunction)
            ?: throw IllegalStateException(
                "Couldn't find module for ${moduleFunction.dump()} lol ?${
                moduleFunction.getPackageFragment()!!.fqName
                    .child(InjektNameConventions.getModuleClassNameForModuleFunction(moduleFunction))
                }"
            )
    }

    fun getModuleClassForFunctionOrNull(moduleFunction: IrFunction): IrClass? {
        return if (!moduleFunction.isExternalDeclaration()) {
            moduleClassTransformer.getModuleClassForFunction(moduleFunction)
        } else {
            pluginContext.referenceClass(
                moduleFunction.getPackageFragment()!!.fqName
                    .child(InjektNameConventions.getModuleClassNameForModuleFunction(moduleFunction))
            )?.owner
        }
    }

}
