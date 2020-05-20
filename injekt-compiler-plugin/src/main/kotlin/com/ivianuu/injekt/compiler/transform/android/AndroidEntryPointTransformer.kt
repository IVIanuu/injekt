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

package com.ivianuu.injekt.compiler.transform.android

import com.ivianuu.injekt.compiler.AndroidSymbols
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.overrides
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// todo check android classes
// todo check onCreate not final
// todo check onCreate calls super
// todo check is not a abstract class
// todo check is final?
class AndroidEntryPointTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val androidSymbols = AndroidSymbols(pluginContext)

    override fun visitClass(declaration: IrClass): IrStatement {
        if (!declaration.hasAnnotation(InjektFqNames.AndroidEntryPoint)) return super.visitClass(
            declaration
        )

        when {
            declaration.isSubclassOf(androidSymbols.application.owner) ->
                transformApplication(declaration)
        }

        return super.visitClass(declaration)
    }

    private fun transformApplication(application: IrClass) {
        val superClass: IrClass = application.superTypes
            .single { it.classOrNull != null }
            .classOrNull!!
            .owner

        val superClassOnCreate = superClass
            .functions
            .single {
                it.name.asString() == "onCreate" &&
                        it.valueParameters.isEmpty()
            }

        val thisOnCreate = application
            .functions
            .singleOrNull {
                it.name.asString() == "onCreate" &&
                        it.valueParameters.isEmpty()
            } ?: application.addFunction {
            name = Name.identifier("onCreate")
            returnType = irBuiltIns.unitType
        }.apply func@{
            overrides(superClassOnCreate)
            dispatchReceiverParameter = application.thisReceiver!!.copyTo(this)
        }

        if (thisOnCreate.body == null) {
            (thisOnCreate as IrFunctionImpl).origin = IrDeclarationOrigin.DEFINED
            thisOnCreate.body = DeclarationIrBuilder(pluginContext, thisOnCreate.symbol).run {
                irBlockBody {
                    +irCall(superClassOnCreate, null, superClass.symbol).apply {
                        dispatchReceiver = irGet(thisOnCreate.dispatchReceiverParameter!!)
                    }
                }
            }
        }

        thisOnCreate.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.superQualifierSymbol != superClass.symbol ||
                    expression.symbol != superClassOnCreate.symbol
                ) return super.visitCall(expression)
                return DeclarationIrBuilder(pluginContext, thisOnCreate.symbol).run {
                    irBlock {
                        +expression

                        +IrCallImpl(
                            expression.endOffset + 1,
                            expression.endOffset + 2,
                            irBuiltIns.unitType,
                            pluginContext.referenceFunctions(
                                FqName("com.ivianuu.injekt.composition.generateCompositions")
                            ).single()
                        )
                        +IrCallImpl(
                            expression.endOffset + 3,
                            expression.endOffset + 4,
                            irBuiltIns.unitType,
                            pluginContext.referenceFunctions(
                                FqName("com.ivianuu.injekt.composition.inject")
                            ).single()
                        ).apply {
                            putTypeArgument(
                                0,
                                application.defaultType
                            )

                            extensionReceiver = irCall(
                                pluginContext.referenceProperties(
                                    FqName("com.ivianuu.injekt.android.applicationComponent")
                                ).single().owner.getter!!
                            ).apply {
                                extensionReceiver =
                                    expression.dispatchReceiver!!.deepCopyWithSymbols()
                            }

                            putValueArgument(
                                0,
                                expression.dispatchReceiver!!.deepCopyWithSymbols()
                            )
                        }
                    }
                }
            }
        })

        println("transformed on create ${thisOnCreate.dump()}")
    }

}
