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
import com.ivianuu.injekt.compiler.transform.composition.CompositionModuleMetadataTransformer
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.RootFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleFunctionTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InjektDeclarationStore(private val pluginContext: IrPluginContext) {

    lateinit var compositionModuleMetadataTransformer: CompositionModuleMetadataTransformer
    lateinit var factoryTransformer: RootFactoryTransformer
    lateinit var factoryModuleTransformer: FactoryModuleTransformer
    lateinit var membersInjectorTransformer: MembersInjectorTransformer
    lateinit var moduleFunctionTransformer: ModuleFunctionTransformer

    fun getCompositionModuleMetadata(function: IrFunction): IrClass? {
        return if (!function.isExternalDeclaration()) {
            compositionModuleMetadataTransformer.getCompositionModuleMetadata(function)
        } else {
            pluginContext.referenceClass(
                function.getPackageFragment()!!.fqName
                    .child(
                        InjektNameConventions.getCompositionModuleMetadataForModule(
                            function.getPackageFragment()!!.fqName,
                            function.descriptor.fqNameSafe
                        )
                    )
            )?.owner
        }
    }

    fun getMembersInjectorForClassOrNull(clazz: IrClass): IrFunction? {
        return if (!clazz.isExternalDeclaration()) {
            membersInjectorTransformer.getMembersInjectorForClass(clazz)
        } else {
            pluginContext.referenceFunctions(
                clazz.fqNameForIrSerialization
                    .parent()
                    .child(
                        InjektNameConventions.getMembersInjectorNameForClass(
                            clazz.getPackageFragment()!!.fqName,
                            clazz.descriptor.fqNameSafe
                        )
                    )
            ).singleOrNull()?.owner
        }
    }

    fun getModuleFunctionForFactory(factoryFunction: IrFunction): IrFunction {
        return if (!factoryFunction.isExternalDeclaration()) {
            val parent = factoryFunction.parent
            val existingModule = if (parent is IrDeclarationContainer) {
                parent
                    .declarations
                    .filterIsInstance<IrFunction>()
                    .firstOrNull {
                        it.descriptor.name == InjektNameConventions.getModuleNameForFactoryFunction(
                            factoryFunction
                        )
                    }
            } else if (parent is IrFunction) {
                var block: IrStatementContainer? = null
                factoryFunction.file.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitBlockBody(body: IrBlockBody): IrBody {
                        if (body.statements.any { it === factoryFunction }) {
                            block = body
                        }
                        return super.visitBlockBody(body)
                    }

                    override fun visitBlock(expression: IrBlock): IrExpression {
                        if (expression.statements.any { it === factoryFunction }) {
                            block = expression
                        }
                        return super.visitBlock(expression)
                    }
                })

                if (block != null) {
                    val index = block!!.statements.indexOf(factoryFunction)
                    block!!.statements[index - 1] as IrFunction
                } else {
                    error("Corrupt parent ${factoryFunction.render()}")
                }
            } else {
                null
            }
            existingModule ?: factoryModuleTransformer
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

    fun getModuleClassForFunctionOrNull(moduleFunction: IrFunction): IrClass? {
        return if (!moduleFunction.isExternalDeclaration()) {
            moduleFunctionTransformer.getModuleClassForFunction(moduleFunction)
        } else {
            val fqName = moduleFunction.getPackageFragment()!!
                .fqName
                .child(
                    InjektNameConventions.getModuleClassNameForModuleFunction(
                        moduleFunction.getPackageFragment()!!.fqName,
                        moduleFunction.descriptor.fqNameSafe
                    )
                )
            return pluginContext.referenceClass(fqName)?.owner
        }
    }

}
