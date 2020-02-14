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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.Variance

class TypeOfCallTransformer(
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    override fun visitCall(expression: IrCall): IrExpression {
        val descriptor = expression.symbol.descriptor
        return if (descriptor.fqNameSafe == FqName("com.ivianuu.injekt.typeOf") &&
            descriptor.valueParameters.isEmpty()
        ) {
            transformTypeOfCall(expression.transformChildren())
        } else {
            super.visitCall(expression)
        }
    }

    private fun transformTypeOfCall(expression: IrCall): IrExpression =
        generateTypeOfForType(expression.getTypeArgument(0)!!)

    private fun generateTypeOfForType(irType: IrType): IrExpression {
        val realTypeOf = context.moduleDescriptor.getPackage(FqName("com.ivianuu.injekt"))
            .memberScope
            .findFirstFunction("typeOf") { it.valueParameters.isNotEmpty() }

        val injektType = getTopLevelClass(InjektClassNames.Type)

        return DeclarationIrBuilder(context, (currentFunction as IrFunction).symbol).irBlock {
            +irCall(
                symbolTable.referenceSimpleFunction(realTypeOf),
                injektType.defaultType.toIrType()
            ).apply {
                putTypeArgument(0, irType)

                // classifier
                putValueArgument(
                    0,
                    IrClassReferenceImpl(
                        startOffset,
                        endOffset,
                        irType,
                        irType.classifierOrFail,
                        irType
                    )
                )

                // arguments
                if (irType.toKotlinType().arguments.isNotEmpty()) {
                    val argumentsType = context.builtIns.getArrayType(
                        Variance.INVARIANT,
                        injektType.defaultType
                    ).toIrType()
                    putValueArgument(
                        1,
                        irCall(
                            this@TypeOfCallTransformer.context.symbols.arrayOf,
                            argumentsType
                        ).apply {
                            putValueArgument(
                                0,
                                IrVarargImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    argumentsType,
                                    injektType.defaultType.toIrType(),
                                    irType.toKotlinType().arguments
                                        .map { generateTypeOfForType(it.type.toIrType()) }
                                )
                            )
                        }
                    )
                }

                // is nullable
                if (irType.isMarkedNullable()) {
                    putValueArgument(2, irBoolean(true))
                }

                // annotations
                if (irType.annotations.isNotEmpty()) {
                    val annotationsType = context.builtIns.getArrayType(
                        Variance.INVARIANT,
                        context.builtIns.kClass.defaultType
                    ).toIrType()

                    putValueArgument(
                        3,
                        irCall(
                            this@TypeOfCallTransformer.context.symbols.arrayOf,
                            annotationsType
                        ).apply {
                            putValueArgument(
                                0,
                                IrVarargImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    annotationsType,
                                    context.builtIns.kClass.defaultType.toIrType(),
                                    irType.annotations
                                        .map { it.type }
                                        .map { type ->
                                            IrClassReferenceImpl(
                                                startOffset,
                                                endOffset,
                                                type,
                                                type.classifierOrFail,
                                                type
                                            )
                                        }
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}
