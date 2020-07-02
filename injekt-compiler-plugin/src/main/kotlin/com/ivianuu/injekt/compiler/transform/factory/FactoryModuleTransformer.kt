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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.addToFileOrAbove
import com.ivianuu.injekt.compiler.addToParentOrAbove
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyBodyTo
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.copyValueParametersToStatic
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class FactoryModuleTransformer(
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    private val modulesByFactories = mutableMapOf<IrFunction, IrFunction>()

    override fun visitFile(declaration: IrFile): IrFile {
        val factoryFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Factory) ||
                    declaration.hasAnnotation(InjektFqNames.ChildFactory) ||
                    declaration.hasAnnotation(InjektFqNames.CompositionFactory)
                ) {
                    factoryFunctions += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        factoryFunctions.forEach { factoryFunction ->
            getModuleFunctionForFactoryFunction(factoryFunction)
        }

        return super.visitFile(declaration)
    }

    fun getModuleFunctionForFactoryFunction(factoryFunction: IrFunction): IrFunction {
        modulesByFactories[factoryFunction]?.let { return it }

        return DeclarationIrBuilder(pluginContext, factoryFunction.symbol).run {
            val moduleFunction = buildFun {
                name = InjektNameConventions.getModuleNameForFactoryFunction(factoryFunction)
                returnType = irBuiltIns.unitType
                visibility = factoryFunction.visibility
                isInline = factoryFunction.isInline
                origin = factoryFunction.origin
            }.apply {
                parent = factoryFunction.parent
                annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                    .noArgSingleConstructorCall(symbols.module)

                copyParameterDeclarationsFrom(factoryFunction)

                body = factoryFunction.copyBodyTo(this)
                (body as? IrBlockBody)?.statements?.removeAt(body!!.statements.lastIndex)
            }
            if (!factoryFunction.isExternalDeclaration()) {
                moduleFunction.addToParentOrAbove(factoryFunction)
                factoryFunction.body = irBlockBody {
                    +irCall(moduleFunction).apply {
                        factoryFunction.typeParameters.forEach {
                            putTypeArgument(it.index, it.defaultType)
                        }
                        dispatchReceiver =
                            factoryFunction.dispatchReceiverParameter?.let { irGet(it) }
                        extensionReceiver =
                            factoryFunction.extensionReceiverParameter?.let { irGet(it) }
                        factoryFunction.valueParameters.forEachIndexed { index, valueParameter ->
                            putValueArgument(index, irGet(valueParameter))
                        }
                    }
                    +factoryFunction.body!!.statements.last()
                }
            }
            modulesByFactories[factoryFunction] = moduleFunction
            moduleFunction
        }
    }

}
