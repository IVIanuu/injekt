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

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InjektPluginContext(
    private val moduleFragment: IrModuleFragment,
    private val pluginContext: IrPluginContext
) : IrPluginContext by pluginContext {
    override fun referenceClass(fqName: FqName): IrClassSymbol? {
        return (pluginContext.referenceClass(fqName)
            ?: run {
                var clazz: IrClassSymbol? = null
                moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitClass(declaration: IrClass): IrStatement {
                        if (declaration.descriptor.fqNameSafe == fqName) {
                            clazz = declaration.symbol
                        }
                        return super.visitClass(declaration)
                    }
                })
                clazz
            })
            ?.also { (pluginContext as IrPluginContextImpl).linker.getDeclaration(it) }
    }

    override fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol> {
        return (pluginContext.referenceConstructors(classFqn) + run {
            referenceClass(classFqn)?.constructors?.toList() ?: emptyList()
        }).distinct()
    }

    override fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol> {
        return (pluginContext.referenceFunctions(fqName) + run {
            val functions = mutableListOf<IrSimpleFunctionSymbol>()
            moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                    if (declaration.descriptor.fqNameSafe == fqName) {
                        functions += declaration.symbol
                    }
                    return super.visitSimpleFunction(declaration)
                }
            })
            functions
        }).distinct()
    }

    override fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol> {
        return (pluginContext.referenceProperties(fqName) + run {
            val properties = mutableListOf<IrPropertySymbol>()
            moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitProperty(declaration: IrProperty): IrStatement {
                    if (declaration.descriptor.fqNameSafe == fqName) {
                        properties += declaration.symbol
                    }
                    return super.visitProperty(declaration)
                }
            })
            properties
        }).distinct()
    }
}
