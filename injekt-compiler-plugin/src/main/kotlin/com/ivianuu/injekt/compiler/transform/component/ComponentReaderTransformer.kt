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
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.child
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.indexPackageFile
import com.ivianuu.injekt.compiler.singleClassArgConstructorCall
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentReaderTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    private val nameProvider = NameProvider()

    override fun lower() {
        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                if (expression.symbol.descriptor.fqNameSafe.asString() !=
                    "com.ivianuu.injekt.runReader" ||
                    expression.extensionReceiver == null
                ) return result
                val component = result.extensionReceiver!!.type
                val context =
                    result.getValueArgument(0)!!.type.typeArguments.first().typeOrFail

                context.classOrNull!!.owner.annotations +=
                    DeclarationIrBuilder(pluginContext, context.classOrNull!!)
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
                                    .child(currentScope!!.scope.scopeOwner.name.asString() + "Reader")
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
                                    irString(context.classOrNull!!.descriptor.fqNameSafe.asString())
                                )
                            }
                        }
                    }
                )

                return DeclarationIrBuilder(pluginContext, result.symbol).run {
                    irCall(
                        pluginContext.referenceFunctions(
                            FqName("com.ivianuu.injekt.runReader")
                        ).single { it.owner.extensionReceiverParameter == null }
                    ).apply {
                        putValueArgument(
                            0,
                            result.extensionReceiver
                        )

                        putValueArgument(
                            1,
                            result.getValueArgument(0)!!
                        )
                    }
                }
            }
        })
    }

}
