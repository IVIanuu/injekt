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
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction

class DefinitionTransformer(
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    private val component = getTopLevelClass(InjektClassNames.Component)
    private val parameters = getTopLevelClass(InjektClassNames.Parameters)

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        if (expression.origin == IrStatementOrigin.LAMBDA &&
            expression.function.extensionReceiverParameter?.descriptor?.type == component.defaultType &&
            expression.function.valueParameters[0].type.toKotlinType() == parameters.defaultType
        ) {
            return convertLambdaIfNecessary(expression)
        }

        return super.visitFunctionExpression(expression)
    }

    private fun convertLambdaIfNecessary(
        functionExpression: IrFunctionExpression
    ): IrExpression {
        val function = functionExpression.function

        return DeclarationIrBuilder(context, currentScope!!.scope.scopeOwnerSymbol).irBlock(
            origin = IrStatementOrigin.OBJECT_LITERAL,
            resultType = functionExpression.type
        ) {
            val irClass = buildClass {
                visibility = Visibilities.LOCAL
                name = Name.special("<function reference to ${function.fqNameWhenAvailable}>")
            }.apply clazz@{
                superTypes = listOf(functionExpression.type)
                createImplicitParameterDeclarationWithWrappedDescriptor()

                addConstructor {
                    returnType = defaultType
                    visibility = Visibilities.PUBLIC
                    isPrimary = true
                }.apply {
                    body = irBlockBody {
                        +IrDelegatingConstructorCallImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.irBuiltIns.unitType,
                            symbolTable.referenceConstructor(
                                context.builtIns.any
                                    .unsubstitutedPrimaryConstructor!!
                            )
                        )
                        +IrInstanceInitializerCallImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            this@clazz.symbol,
                            context.irBuiltIns.unitType
                        )
                    }
                }

                addFunction {
                    name = Name.identifier("invoke")
                    modality = Modality.OPEN
                    returnType = function.returnType
                    isSuspend = function.isSuspend
                }.apply {
                    overriddenSymbols = overriddenSymbols + listOf(
                        symbolTable.referenceSimpleFunction(
                            (functionExpression.type
                                .toKotlinType()
                                .constructor
                                .declarationDescriptor as ClassDescriptor)
                                .unsubstitutedMemberScope
                                .findSingleFunction(Name.identifier("invoke"))
                        )
                    )
                    dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
                    annotations += function.annotations
                    val valueParameterMap =
                        function.explicitParameters.withIndex().associate { (index, param) ->
                            param to param.copyTo(this, type = param.type, index = index)
                        }
                    valueParameters = valueParameters + valueParameterMap.values
                    body = DeclarationIrBuilder(context, symbol).irBlockBody {
                        function.body?.statements?.forEach { statement ->
                            +statement.transform(object : IrElementTransformerVoid() {
                                override fun visitGetValue(expression: IrGetValue): IrExpression {
                                    val replacement = valueParameterMap[expression.symbol.owner]
                                        ?: return super.visitGetValue(expression)

                                    at(expression.startOffset, expression.endOffset)
                                    return irGet(replacement)
                                }

                                override fun visitReturn(expression: IrReturn): IrExpression =
                                    if (expression.returnTargetSymbol != function.symbol) {
                                        super.visitReturn(expression)
                                    } else {
                                        at(expression.startOffset, expression.endOffset)
                                        irReturn(expression.value.transform(this, null))
                                    }

                                override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                                    if (declaration.parent == function)
                                        declaration.parent = this@apply
                                    return super.visitDeclaration(declaration)
                                }
                            }, null)
                        }
                    }
                }
            }

            +irClass
            +IrConstructorCallImpl.fromSymbolDescriptor(
                startOffset, endOffset, irClass.defaultType,
                irClass.constructors.single().symbol,
                IrStatementOrigin.OBJECT_LITERAL
            )
        }
    }
}
