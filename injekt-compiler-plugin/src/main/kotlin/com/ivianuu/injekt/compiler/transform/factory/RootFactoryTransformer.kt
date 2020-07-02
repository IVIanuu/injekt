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
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class RootFactoryTransformer(
    context: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(context) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val factoryFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Factory)) {
                    factoryFunctions += declaration
                } else if (declaration.hasAnnotation(InjektFqNames.ChildFactory) ||
                    declaration.hasAnnotation(InjektFqNames.CompositionFactory)
                ) {
                    declaration.body =
                        InjektDeclarationIrBuilder(pluginContext, declaration.symbol).run {
                            builder.irExprBody(irInjektIntrinsicUnit())
                        }
                }
                return super.visitFunctionNew(declaration)
            }
        })

        factoryFunctions.forEach { function ->
            DeclarationIrBuilder(pluginContext, function.symbol).run {
                val oldBody = function.body!!
                function.body = irBlockBody {
                    val moduleExpr = oldBody.statements
                        .first() as IrExpression

                    val moduleClass = declarationStore.getModuleClassForFunction(
                        declarationStore.getModuleFunctionForFactory(function)
                    )

                    val moduleAccessor = if (moduleClass.kind == ClassKind.OBJECT) {
                        val expr: FactoryExpression = { irGetObject(moduleClass.symbol) }
                        expr
                    } else {
                        val moduleVariable = irTemporary(moduleExpr)
                        val expr: FactoryExpression = { irGet(moduleVariable) }
                        expr
                    }

                    val implFactory = FactoryImpl(
                        origin = function.descriptor.fqNameSafe,
                        parent = null,
                        superType = function.returnType,
                        moduleClass = moduleClass,
                        factoryFunction = function,
                        factoryModuleAccessor = moduleAccessor,
                        pluginContext = pluginContext,
                        symbols = symbols,
                        declarationStore = declarationStore
                    )

                    +irReturn(implFactory.getImplExpression())
                }
            }
        }

        return super.visitModuleFragment(declaration)
    }

}
