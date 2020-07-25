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

package com.ivianuu.injekt.compiler.transform.component

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.readableName
import com.ivianuu.injekt.compiler.singleClassArgConstructorCall
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class EntryPointTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    private val nameProvider = NameProvider()

    override fun lower() {
        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            private val newDeclarations = mutableListOf<IrDeclaration>()

            override fun visitFileNew(declaration: IrFile): IrFile {
                return super.visitFileNew(declaration)
                    .also {
                        newDeclarations.forEach { declaration.addChild(it) }
                        newDeclarations.clear()
                    }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                if (expression.symbol.descriptor.fqNameSafe.asString() !=
                    "com.ivianuu.injekt.runReader"
                ) return result
                val component = result.extensionReceiver!!.type
                val lambda = (result.getValueArgument(0) as IrFunctionExpression)
                    .function

                val entryPoint = buildClass {
                    name = nameProvider.allocateForGroup(
                        "${currentScope!!.scope.scopeOwner.name.asString()}\$EntryPoint".asNameId()
                    )
                    kind = ClassKind.INTERFACE
                }.apply {
                    parent = currentFile
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                    addMetadataIfNotLocal()
                    annotations +=
                        DeclarationIrBuilder(pluginContext, symbol)
                            .singleClassArgConstructorCall(
                                symbols.entryPoint,
                                component.classifierOrFail
                            )
                }

                fun addEntryPointFunction(type: IrType): IrFunction {
                    return entryPoint.addFunction {
                        this.name = type.readableName()
                        returnType = type
                        modality = Modality.ABSTRACT
                    }.apply {
                        dispatchReceiverParameter = entryPoint.thisReceiver?.copyTo(this)
                        addMetadataIfNotLocal()
                    }
                }

                newDeclarations += entryPoint

                return DeclarationIrBuilder(pluginContext, result.symbol).run {
                    irBlock {
                        val variablesByValueParameter =
                            lambda.valueParameters
                                .filter { it.hasAnnotation(InjektFqNames.Implicit) }
                                .associateWith { valueParameter ->
                                    val accessor = addEntryPointFunction(valueParameter.type)
                                    irTemporary(
                                        irCall(accessor).apply {
                                            dispatchReceiver = irImplicitCast(
                                                result.extensionReceiver!!.deepCopyWithVariables(),
                                                entryPoint.defaultType
                                            )
                                        }
                                    )
                                }.mapKeys { it.key.symbol }
                        (lambda.body as IrBlockBody).statements.forEach { stmt ->
                            +stmt.transform(
                                object : IrElementTransformerVoid() {
                                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                                        return variablesByValueParameter[expression.symbol]
                                            ?.let { irGet(it) }
                                            ?: super.visitGetValue(expression)
                                    }

                                    override fun visitReturn(expression: IrReturn): IrExpression {
                                        val result = super.visitReturn(expression) as IrReturn
                                        return if (result.returnTargetSymbol == lambda.symbol) result.value else result
                                    }
                                },
                                null
                            )
                        }
                    }
                }
            }
        })
    }


}
