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

import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.indexPackageFile
import com.ivianuu.injekt.compiler.singleClassArgConstructorCall
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.ReaderTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentReaderTransformer(
    pluginContext: IrPluginContext,
    private val readerTransformer: ReaderTransformer
) : AbstractInjektTransformer(pluginContext) {

    private val nameProvider = NameProvider()

    override fun lower() {
        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                if (expression.symbol.descriptor.fqNameSafe.asString() !=
                    "com.ivianuu.injekt.runReader"
                ) return result
                val component = result.extensionReceiver!!.type
                val lambda = (result.getValueArgument(0) as IrFunctionExpression)
                    .function
                val readerInfo = readerTransformer.getReaderInfoForDeclaration(lambda)

                readerInfo.annotations +=
                    DeclarationIrBuilder(pluginContext, readerInfo.symbol)
                        .singleClassArgConstructorCall(
                            symbols.entryPoint,
                            component.classifierOrFail
                        )

                module.indexPackageFile.addChild(
                    buildClass {
                        name = nameProvider.allocateForGroup(
                            getJoinedName(
                                currentFile.fqName,
                                currentScope!!.scope.scopeOwner.fqNameSafe.parent()
                                    .child("${currentScope!!.scope.scopeOwner.name.asString()}Reader".asNameId())
                            )
                        )
                        kind = ClassKind.INTERFACE
                    }.apply {
                        createImplicitParameterDeclarationWithWrappedDescriptor()
                        addMetadataIfNotLocal()
                        annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                            irCall(symbols.index.constructors.single()).apply {
                                putValueArgument(
                                    0,
                                    irString(readerInfo.descriptor.fqNameSafe.asString())
                                )
                            }
                        }
                    }
                )

                return DeclarationIrBuilder(pluginContext, result.symbol).run {
                    irBlock {
                        val variablesByValueParameter =
                            lambda.valueParameters.associateWith { valueParameter ->
                                val componentFunction = readerInfo.declarations
                                    .filterIsInstance<IrFunction>()
                                    .single { it.name == valueParameter.name }
                                irTemporary(
                                    irCall(componentFunction).apply {
                                        dispatchReceiver = irImplicitCast(
                                            result.extensionReceiver!!.deepCopyWithVariables(),
                                            readerInfo.defaultType
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
